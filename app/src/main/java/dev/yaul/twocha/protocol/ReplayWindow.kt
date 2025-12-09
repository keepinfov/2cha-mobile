package dev.yaul.twocha.protocol

/**
 * Sliding window for replay attack protection
 *
 * Uses a 64-bit bitmap to track seen packets within the window.
 * Packets outside the window (too old) are rejected.
 * Duplicate packets are rejected.
 */
class ReplayWindow(
    private val windowSize: Long = Constants.REPLAY_WINDOW_SIZE
) {
    @Volatile
    private var lastCounter: Long = 0

    @Volatile
    private var bitmap: Long = 0

    /**
     * Check if packet is valid (not a replay) and update window
     *
     * @param counter The packet counter to check
     * @return true if packet should be accepted, false if replay/invalid
     */
    @Synchronized
    fun checkAndUpdate(counter: Long): Boolean {
        // Reject counter 0 (invalid)
        if (counter == 0L) {
            return false
        }

        if (counter > lastCounter) {
            // New packet ahead of window
            val diff = counter - lastCounter
            if (diff >= windowSize) {
                // Big jump - reset bitmap
                bitmap = 1L
            } else {
                // Shift bitmap and set new bit
                bitmap = bitmap shl diff.toInt()
                bitmap = bitmap or 1L
            }
            lastCounter = counter
            return true
        }

        // Packet within or before window
        val diff = lastCounter - counter
        if (diff >= windowSize) {
            // Too old - reject
            return false
        }

        // Check if already seen
        val bit = 1L shl diff.toInt()
        if ((bitmap and bit) != 0L) {
            // Already seen - replay
            return false
        }

        // Mark as seen
        bitmap = bitmap or bit
        return true
    }

    /**
     * Check if a counter value would be valid without updating the window
     */
    @Synchronized
    fun isValid(counter: Long): Boolean {
        if (counter == 0L) return false
        if (counter > lastCounter) return true

        val diff = lastCounter - counter
        if (diff >= windowSize) return false

        val bit = 1L shl diff.toInt()
        return (bitmap and bit) == 0L
    }

    /**
     * Reset the window to initial state
     */
    @Synchronized
    fun reset() {
        lastCounter = 0
        bitmap = 0
    }

    /**
     * Get current last seen counter
     */
    val currentCounter: Long
        @Synchronized get() = lastCounter
}