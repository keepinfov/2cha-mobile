package dev.yaul.twocha.crypto

/**
 * Factory for creating AEAD cipher instances
 */
object CipherFactory {

    /**
     * Create an AEAD cipher based on the suite type
     *
     * @param suite The cipher suite to use
     * @param key The 32-byte encryption key
     * @return AeadCipher instance
     */
    fun create(suite: CipherSuite, key: ByteArray): AeadCipher {
        return when (suite) {
            CipherSuite.CHACHA20_POLY1305 -> ChaCha20Poly1305Cipher(key)
            CipherSuite.AES_256_GCM -> Aes256GcmCipher(key)
        }
    }

    /**
     * Create cipher from hex key string
     */
    fun create(suite: CipherSuite, hexKey: String): AeadCipher {
        val key = CryptoUtils.hexToBytes(hexKey)
        return create(suite, key)
    }
}