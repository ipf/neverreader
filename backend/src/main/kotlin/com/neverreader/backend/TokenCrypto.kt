package com.neverreader.backend

import android.content.Context
import android.util.Base64
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.RegistryConfiguration
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager

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
