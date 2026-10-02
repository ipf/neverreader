package com.neverreader.backend

import android.content.Context
import android.util.Base64
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.RegistryConfiguration
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager

/**
 * Symmetric encryption for access tokens.
 *
 * An interface rather than the Tink calls themselves so AccountManager can be
 * tested: AndroidKeysetManager keeps its master key in the Android keystore,
 * which Robolectric does not provide, so anything taking a concrete Aead was
 * unreachable from a test.
 */
interface TokenCipher {
    fun encrypt(plaintext: String): String
    fun decrypt(ciphertext: String): String
}

/** The real thing: Tink, with its master key in the Android keystore. */
class TinkTokenCipher(context: Context) : TokenCipher {

    private val aead: Aead by lazy { TokenCrypto.aead(context) }

    override fun encrypt(plaintext: String): String = TokenCrypto.encrypt(aead, plaintext)

    override fun decrypt(ciphertext: String): String = TokenCrypto.decrypt(aead, ciphertext)
}

object TokenCrypto {

    init {
        AeadConfig.register()
    }

    fun aead(context: Context): Aead =
        AndroidKeysetManager.Builder()
            .withSharedPref(context, "neverreader_keyset", "neverreader_sec")
            .withKeyTemplate(KeyTemplates.get("AES256_GCM"))
            .withMasterKeyUri("android-keystore://neverreader_master")
            .build()
            .keysetHandle
            .getPrimitive(RegistryConfiguration.get(), Aead::class.java)

    fun encrypt(aead: Aead, plaintext: String): String =
        Base64.encodeToString(aead.encrypt(plaintext.toByteArray(), null), Base64.NO_WRAP)

    fun decrypt(aead: Aead, ciphertext: String): String =
        String(aead.decrypt(Base64.decode(ciphertext, Base64.NO_WRAP), null))
}
