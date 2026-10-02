package com.neverreader.backend

import androidx.test.core.app.ApplicationProvider
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.RegistryConfiguration
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * The real AES-GCM path, with a Tink keyset built in memory.
 *
 * AccountManagerTest stands in a reversible cipher, which proves the manager
 * encrypts but nothing about the encryption itself. This uses the same
 * KeyTemplates.get("AES256_GCM") the production keyset manager uses, obtained
 * without AndroidKeysetManager so it needs no Android keystore - which is what
 * made TokenCrypto untestable in the first place.
 *
 * Under Robolectric only because TokenCrypto encodes with android.util.Base64.
 */
@RunWith(RobolectricTestRunner::class)
class TokenCipherTest {

    private lateinit var aead: Aead

    @Before
    fun setUp() {
        // Touching the object registers AeadConfig, which is what makes the
        // primitive registry know about AES-GCM.
        TokenCrypto.aead(ApplicationProvider.getApplicationContext())
        aead = KeysetHandle.generateNew(KeyTemplates.get("AES256_GCM"))
            .getPrimitive(RegistryConfiguration.get(), Aead::class.java)
    }

    @Test
    fun `a token survives a round trip`() {
        val token = "ya29.a0AfH6SMB-secret-token"

        val encrypted = TokenCrypto.encrypt(aead, token)

        assertEquals(token, TokenCrypto.decrypt(aead, encrypted))
    }

    @Test
    fun `the encrypted form is not the plaintext`() {
        val token = "ya29.a0AfH6SMB-secret-token"

        val encrypted = TokenCrypto.encrypt(aead, token)

        assertNotEquals(token, encrypted)
        assertTrue(
            !encrypted.contains(token),
            "the ciphertext still contains the plaintext: $encrypted",
        )
    }

    @Test
    fun `the same plaintext encrypts differently each time`() {
        // AES-GCM is randomised; identical tokens must not produce identical
        // ciphertext, or the stored rows would be comparable at a glance.
        val token = "same-token"

        val first = TokenCrypto.encrypt(aead, token)
        val second = TokenCrypto.encrypt(aead, token)

        assertNotEquals(first, second)
        assertEquals(token, TokenCrypto.decrypt(aead, first))
        assertEquals(token, TokenCrypto.decrypt(aead, second))
    }

    @Test
    fun `a long token round trips`() {
        val token = "x".repeat(4096)

        assertEquals(token, TokenCrypto.decrypt(aead, TokenCrypto.encrypt(aead, token)))
    }

    @Test
    fun `an empty token round trips`() {
        assertEquals("", TokenCrypto.decrypt(aead, TokenCrypto.encrypt(aead, "")))
    }

    @Test
    fun `tampered ciphertext does not decrypt`() {
        val encrypted = TokenCrypto.encrypt(aead, "token")
        val tampered = encrypted.dropLast(2) + (if (encrypted.last() == 'A') 'B' else 'A') + encrypted.last()

        val error = runCatching { TokenCrypto.decrypt(aead, tampered) }.exceptionOrNull()

        assertTrue(error is Exception, message = "$error")
    }
}