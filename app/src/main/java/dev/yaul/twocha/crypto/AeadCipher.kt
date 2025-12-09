package dev.yaul.twocha.crypto

import dev.yaul.twocha.protocol.Constants

/**
 * AEAD (Authenticated Encryption with Associated Data) cipher interface
 *
 * Supports both ChaCha20-Poly1305 and AES-256-GCM
 */
interface AeadCipher {
    /**
     * Cipher name for display
     */
    val name: String

    /**
     * Encrypt plaintext with AEAD
     *
     * @param nonce 12-byte nonce (must be unique per encryption with same key)
     * @param plaintext Data to encrypt
     * @param aad Additional Authenticated Data (not encrypted, but authenticated)
     * @return Ciphertext + authentication tag
     * @throws CryptoException if encryption fails
     */
    fun encrypt(nonce: ByteArray, plaintext: ByteArray, aad: ByteArray): ByteArray

    /**
     * Decrypt ciphertext with AEAD
     *
     * @param nonce 12-byte nonce (same as used for encryption)
     * @param ciphertextWithTag Ciphertext + authentication tag
     * @param aad Additional Authenticated Data (must match encryption)
     * @return Decrypted plaintext
     * @throws CryptoException if decryption fails or authentication fails
     */
    fun decrypt(nonce: ByteArray, ciphertextWithTag: ByteArray, aad: ByteArray): ByteArray

    /**
     * Try to decrypt, returning null on failure instead of throwing
     */
    fun tryDecrypt(nonce: ByteArray, ciphertextWithTag: ByteArray, aad: ByteArray): ByteArray? {
        return try {
            decrypt(nonce, ciphertextWithTag, aad)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Securely zero the key material
     */
    fun destroy()
}

/**
 * Exception for cryptographic operations
 */
class CryptoException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Cipher suite types
 */
enum class CipherSuite {
    CHACHA20_POLY1305,
    AES_256_GCM;

    companion object {
        fun fromString(value: String): CipherSuite {
            return when (value.lowercase().replace("_", "-")) {
                "chacha20-poly1305" -> CHACHA20_POLY1305
                "aes-256-gcm" -> AES_256_GCM
                else -> CHACHA20_POLY1305 // default
            }
        }
    }

    override fun toString(): String {
        return when (this) {
            CHACHA20_POLY1305 -> "chacha20-poly1305"
            AES_256_GCM -> "aes-256-gcm"
        }
    }
}

/**
 * Utility functions for key handling
 */
object CryptoUtils {
    /**
     * Convert hex string to byte array
     */
    fun hexToBytes(hex: String): ByteArray {
        val cleanHex = hex.trim().lowercase()
        require(cleanHex.length == 64) {
            "Key must be 64 hex chars, got ${cleanHex.length}"
        }
        return cleanHex.chunked(2)
            .map { it.toInt(16).toByte() }
            .toByteArray()
    }

    /**
     * Convert byte array to hex string
     */
    fun bytesToHex(bytes: ByteArray): String {
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Securely zero a byte array
     */
    fun secureZero(array: ByteArray) {
        array.fill(0)
    }

    /**
     * Validate key length
     */
    fun validateKey(key: ByteArray): Boolean {
        return key.size == Constants.CHACHA20_KEY_SIZE
    }
}package dev.yaul.twocha.crypto

import dev.yaul.twocha.protocol.Constants

/**
 * AEAD (Authenticated Encryption with Associated Data) cipher interface
 *
 * Supports both ChaCha20-Poly1305 and AES-256-GCM
 */
interface AeadCipher {
    /**
     * Cipher name for display
     */
    val name: String

    /**
     * Encrypt plaintext with AEAD
     *
     * @param nonce 12-byte nonce (must be unique per encryption with same key)
     * @param plaintext Data to encrypt
     * @param aad Additional Authenticated Data (not encrypted, but authenticated)
     * @return Ciphertext + authentication tag
     * @throws CryptoException if encryption fails
     */
    fun encrypt(nonce: ByteArray, plaintext: ByteArray, aad: ByteArray): ByteArray

    /**
     * Decrypt ciphertext with AEAD
     *
     * @param nonce 12-byte nonce (same as used for encryption)
     * @param ciphertextWithTag Ciphertext + authentication tag
     * @param aad Additional Authenticated Data (must match encryption)
     * @return Decrypted plaintext
     * @throws CryptoException if decryption fails or authentication fails
     */
    fun decrypt(nonce: ByteArray, ciphertextWithTag: ByteArray, aad: ByteArray): ByteArray

    /**
     * Try to decrypt, returning null on failure instead of throwing
     */
    fun tryDecrypt(nonce: ByteArray, ciphertextWithTag: ByteArray, aad: ByteArray): ByteArray? {
        return try {
            decrypt(nonce, ciphertextWithTag, aad)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Securely zero the key material
     */
    fun destroy()
}

/**
 * Exception for cryptographic operations
 */
class CryptoException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Cipher suite types
 */
enum class CipherSuite {
    CHACHA20_POLY1305,
    AES_256_GCM;

    companion object {
        fun fromString(value: String): CipherSuite {
            return when (value.lowercase().replace("_", "-")) {
                "chacha20-poly1305" -> CHACHA20_POLY1305
                "aes-256-gcm" -> AES_256_GCM
                else -> CHACHA20_POLY1305 // default
            }
        }
    }

    override fun toString(): String {
        return when (this) {
            CHACHA20_POLY1305 -> "chacha20-poly1305"
            AES_256_GCM -> "aes-256-gcm"
        }
    }
}

/**
 * Utility functions for key handling
 */
object CryptoUtils {
    /**
     * Convert hex string to byte array
     */
    fun hexToBytes(hex: String): ByteArray {
        val cleanHex = hex.trim().lowercase()
        require(cleanHex.length == 64) {
            "Key must be 64 hex chars, got ${cleanHex.length}"
        }
        return cleanHex.chunked(2)
            .map { it.toInt(16).toByte() }
            .toByteArray()
    }

    /**
     * Convert byte array to hex string
     */
    fun bytesToHex(bytes: ByteArray): String {
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Securely zero a byte array
     */
    fun secureZero(array: ByteArray) {
        array.fill(0)
    }

    /**
     * Validate key length
     */
    fun validateKey(key: ByteArray): Boolean {
        return key.size == Constants.CHACHA20_KEY_SIZE
    }
}