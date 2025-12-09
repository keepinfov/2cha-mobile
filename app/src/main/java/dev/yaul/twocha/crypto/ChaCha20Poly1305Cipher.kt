package dev.yaul.twocha.crypto

import dev.yaul.twocha.protocol.Constants
import org.bouncycastle.crypto.engines.ChaCha7539Engine
import org.bouncycastle.crypto.macs.Poly1305
import org.bouncycastle.crypto.modes.ChaCha20Poly1305
import org.bouncycastle.crypto.params.AEADParameters
import org.bouncycastle.crypto.params.KeyParameter

/**
 * ChaCha20-Poly1305 AEAD implementation using Bouncy Castle
 * RFC 8439 compliant
 */
class ChaCha20Poly1305Cipher(key: ByteArray) : AeadCipher {

    override val name: String = "ChaCha20-Poly1305"

    private val keyParameter: KeyParameter
    private var destroyed = false

    init {
        require(key.size == Constants.CHACHA20_KEY_SIZE) {
            "Key must be ${Constants.CHACHA20_KEY_SIZE} bytes, got ${key.size}"
        }
        // Copy key to prevent external modification
        keyParameter = KeyParameter(key.copyOf())
    }

    override fun encrypt(nonce: ByteArray, plaintext: ByteArray, aad: ByteArray): ByteArray {
        checkNotDestroyed()
        require(nonce.size == Constants.NONCE_SIZE) {
            "Nonce must be ${Constants.NONCE_SIZE} bytes, got ${nonce.size}"
        }

        try {
            val cipher = ChaCha20Poly1305()
            val params = AEADParameters(
                keyParameter,
                Constants.TAG_SIZE * 8, // tag size in bits
                nonce,
                aad
            )
            cipher.init(true, params)

            val output = ByteArray(cipher.getOutputSize(plaintext.size))
            var len = cipher.processBytes(plaintext, 0, plaintext.size, output, 0)
            len += cipher.doFinal(output, len)

            return output.copyOf(len)
        } catch (e: Exception) {
            throw CryptoException("Encryption failed", e)
        }
    }

    override fun decrypt(nonce: ByteArray, ciphertextWithTag: ByteArray, aad: ByteArray): ByteArray {
        checkNotDestroyed()
        require(nonce.size == Constants.NONCE_SIZE) {
            "Nonce must be ${Constants.NONCE_SIZE} bytes, got ${nonce.size}"
        }

        if (ciphertextWithTag.size < Constants.TAG_SIZE) {
            throw CryptoException("Ciphertext too short for authentication tag")
        }

        try {
            val cipher = ChaCha20Poly1305()
            val params = AEADParameters(
                keyParameter,
                Constants.TAG_SIZE * 8, // tag size in bits
                nonce,
                aad
            )
            cipher.init(false, params)

            val output = ByteArray(cipher.getOutputSize(ciphertextWithTag.size))
            var len = cipher.processBytes(ciphertextWithTag, 0, ciphertextWithTag.size, output, 0)
            len += cipher.doFinal(output, len)

            return output.copyOf(len)
        } catch (e: org.bouncycastle.crypto.InvalidCipherTextException) {
            throw CryptoException("Authentication failed - data may be tampered", e)
        } catch (e: Exception) {
            throw CryptoException("Decryption failed", e)
        }
    }

    override fun destroy() {
        if (!destroyed) {
            // Zero out the key material
            try {
                val keyField = KeyParameter::class.java.getDeclaredField("key")
                keyField.isAccessible = true
                val keyBytes = keyField.get(keyParameter) as ByteArray
                CryptoUtils.secureZero(keyBytes)
            } catch (e: Exception) {
                // Best effort cleanup
            }
            destroyed = true
        }
    }

    private fun checkNotDestroyed() {
        if (destroyed) {
            throw CryptoException("Cipher has been destroyed")
        }
    }

    protected fun finalize() {
        destroy()
    }
}