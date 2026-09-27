package com.neverreader.backend.readeck

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * The OAuth device flow, which is what the setup screen drives.
 *
 * The content types here are not incidental: Readeck's OpenAPI spec registers
 * only `application/json` on /oauth/token, while /oauth/device accepts form
 * encoding too. Sending form-encoded to /oauth/token made the whole flow fail
 * at the last step, long after the browser had already been opened.
 */
private val REQUIRED_CLIENT_FIELDS = listOf("client_name", "client_uri", "software_id", "software_version")

class ReadeckAuthTest {

    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun serverUrl() = server.url("/").toString().trimEnd('/')

    private val appVersion = "1.2.3"

    @Test
    fun `registers a client as json and returns the client id`() = runTest {
        server.enqueue(MockResponse().setResponseCode(201).setBody("""{"client_id":"abc123"}"""))

        val clientId = ReadeckAuth.registerClient(serverUrl(), appVersion)

        assertEquals("abc123", clientId)
        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/oauth/client", request.path)
        assertTrue(
            "expected application/json, got ${request.getHeader("Content-Type")}",
            request.getHeader("Content-Type")!!.startsWith("application/json"),
        )
    }

    /**
     * Readeck's oauthClientCreate schema requires client_name, client_uri,
     * software_id and software_version. Dropping software_version made every
     * registration fail with `invalid_client_metadata` before the user ever saw
     * a code, so assert the full required set rather than a sample.
     */
    @Test
    fun `client registration sends every field the spec marks required`() = runTest {
        server.enqueue(MockResponse().setResponseCode(201).setBody("""{"client_id":"abc"}"""))

        ReadeckAuth.registerClient(serverUrl(), appVersion)

        val body = server.takeRequest().body.readUtf8()
        val json = Json.parseToJsonElement(body).jsonObject
        val missing = REQUIRED_CLIENT_FIELDS.filterNot { key -> key in json }
        assertTrue("missing $missing in $body", missing.isEmpty())
        assertEquals(appVersion, json["software_version"]!!.jsonPrimitive.content)
    }

    @Test
    fun `client registration asks for the device code grant`() = runTest {
        server.enqueue(MockResponse().setResponseCode(201).setBody("""{"client_id":"abc"}"""))

        ReadeckAuth.registerClient(serverUrl(), appVersion)

        val body = server.takeRequest().body.readUtf8()
        assertTrue(body, body.contains("urn:ietf:params:oauth:grant-type:device_code"))
    }

    @Test
    fun `starts the device flow and maps the session`() = runTest {
        server.enqueue(
            MockResponse().setBody(
                """{"device_code":"dev","user_code":"ABCD-EFGH",
                   "verification_uri":"https://example.com/link",
                   "verification_uri_complete":"https://example.com/link?user_code=ABCD-EFGH",
                   "interval":5}"""
            )
        )

        val session = ReadeckAuth.startDeviceFlow(serverUrl(), "abc123")

        assertEquals("dev", session.deviceCode)
        assertEquals("ABCD-EFGH", session.userCode)
        assertEquals("https://example.com/link", session.verificationUri)
        assertEquals("https://example.com/link?user_code=ABCD-EFGH", session.verificationUriComplete)
        assertEquals(5L, session.interval)
        assertEquals("/api/oauth/device", server.takeRequest().path)
    }

    @Test
    fun `polls the token endpoint with json and returns the access token`() = runTest {
        // The first poll is still pending; the second returns the token.
        server.enqueue(
            MockResponse().setResponseCode(400).setBody("""{"error":"authorization_pending"}""")
        )
        server.enqueue(MockResponse().setResponseCode(201).setBody("""{"access_token":"tok-123"}"""))

        val token = ReadeckAuth.awaitToken(
            serverUrl(),
            "abc123",
            DeviceSession("dev", "ABCD", "https://example.com/link", null, 0),
        )

        assertEquals("tok-123", token)

        val poll = server.takeRequest()
        assertEquals("/api/oauth/token", poll.path)
        assertTrue(
            "expected application/json on /oauth/token, got ${poll.getHeader("Content-Type")}",
            poll.getHeader("Content-Type")!!.startsWith("application/json"),
        )
        val body = poll.body.readUtf8()
        assertTrue(body, body.contains("urn:ietf:params:oauth:grant-type:device_code"))
        assertTrue(body, body.contains("dev"))
    }

    @Test
    fun `surfaces the server's error description on a hard failure`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(400).setBody(
                """{"error":"invalid_client","error_description":"unknown client"}"""
            )
        )

        val message = runCatching {
            ReadeckAuth.awaitToken(
                serverUrl(),
                "abc123",
                DeviceSession("dev", "ABCD", "https://example.com/link", null, 0),
            )
        }.exceptionOrNull()?.message

        assertEquals("device flow failed: unknown client", message)
    }
}
