package dev.yaul.twocha.crash

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dev.yaul.twocha.R
import dev.yaul.twocha.CrashReportActivity
import kotlin.system.exitProcess

object CrashReporter : Thread.UncaughtExceptionHandler {
    private const val TAG = "CrashReporter"
    private const val PREFS_NAME = "crash_reports"
    private const val KEY_LAST_CRASH = "last_crash"
    private const val MAX_REPORT_LENGTH = 500_000 // Keep intent extras well below binder limits

    private lateinit var application: Application
    private var defaultHandler: Thread.UncaughtExceptionHandler? = null
    private var dialogShowing = false

    fun install(app: Application) {
        if (this::application.isInitialized) return

        application = app
        defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler(this)

        application.registerActivityLifecycleCallbacks(object : SimpleActivityLifecycleCallbacks() {
            override fun onActivityResumed(activity: Activity) {
                if (activity is CrashReportActivity) return
                showPendingCrashDialog(activity)
            }
        })
    }

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        val report = buildReport(thread, throwable)
        persistReport(report)
        Log.e(TAG, "Uncaught exception in thread ${thread.name}", throwable)

        launchCrashScreen(report)

        // Let the default handler proceed so the process is cleaned up
        defaultHandler?.uncaughtException(thread, throwable) ?: exitProcess(2)
    }

    fun consumePendingCrashReport(): String? {
        val prefs = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val report = prefs.getString(KEY_LAST_CRASH, null)
        prefs.edit().remove(KEY_LAST_CRASH).apply()
        return report
    }

    private fun persistReport(report: String) {
        val prefs = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LAST_CRASH, report.truncated()).apply()
    }

    private fun launchCrashScreen(report: String) {
        val intent = Intent(application, CrashReportActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(CrashReportActivity.EXTRA_CRASH_REPORT, report.truncated())
        }
        runCatching { application.startActivity(intent) }
    }

    private fun showPendingCrashDialog(activity: Activity) {
        if (dialogShowing) return
        val pendingReport = consumePendingCrashReport() ?: return

        dialogShowing = true
        MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.crash_dialog_title)
            .setMessage(R.string.crash_dialog_message)
            .setCancelable(false)
            .setPositiveButton(R.string.crash_dialog_view_details) { dialog, _ ->
                dialog.dismiss()
                dialogShowing = false
                val intent = Intent(activity, CrashReportActivity::class.java).apply {
                    putExtra(CrashReportActivity.EXTRA_CRASH_REPORT, pendingReport)
                }
                activity.startActivity(intent)
            }
            .setNegativeButton(android.R.string.cancel) { dialog, _ ->
                dialog.dismiss()
                dialogShowing = false
            }
            .show()
    }

    private fun buildReport(thread: Thread, throwable: Throwable): String {
        val stackTrace = Log.getStackTraceString(throwable)
        return buildString {
            appendLine("Thread: ${thread.name}")
            appendLine("Exception: ${throwable.javaClass.name}")
            appendLine("Message: ${throwable.message}")
            appendLine()
            appendLine(stackTrace)
        }
    }

    private fun String.truncated(): String {
        if (length <= MAX_REPORT_LENGTH) return this
        val truncatedBody = substring(0, MAX_REPORT_LENGTH)
        return buildString(MAX_REPORT_LENGTH + 64) {
            append(truncatedBody)
            appendLine()
            appendLine("… (truncated crash report)")
        }
    }
}

open class SimpleActivityLifecycleCallbacks : Application.ActivityLifecycleCallbacks {
    override fun onActivityCreated(activity: Activity, savedInstanceState: android.os.Bundle?) {}
    override fun onActivityStarted(activity: Activity) {}
    override fun onActivityResumed(activity: Activity) {}
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: android.os.Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {}
}
