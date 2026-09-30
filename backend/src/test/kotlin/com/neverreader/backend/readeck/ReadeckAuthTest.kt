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

    @Test
    fun `code verifier is 64 unreserved characters and challenge is unpadded base64url`() {
        val verifier = ReadeckAuth.newCodeVerifier()
        assertEquals(64, verifier.length)
        assertTrue(verifier.all { it.isLetterOrDigit() })

        // RFC 7636 appendix B's published example, so this pins the encoding
        // rather than whatever base64 variant happens to be on the classpath.
        assertEquals(
            "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM",
            ReadeckAuth.codeChallenge("dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"),
        )
        // Unpadded: a trailing '=' would not match what the server recomputes.
        assertTrue(!ReadeckAuth.codeChallenge(verifier).contains('='))
    }

    @Test
    fun `authorize url is built at the instance root, not under api`() {
        val url = ReadeckAuth.buildAuthorizeUrl(
            serverUrl = "https://readeck.example.com/api",
            clientId = "abc",
            redirectUri = ReadeckAuth.REDIRECT_URI,
            scope = "bookmarks:read",
            codeChallenge = "chal",
            state = "st",
        )
        // /authorize is served from the root, unlike every other endpoint.
        assertTrue(url, url.startsWith("https://readeck.example.com/authorize?"))
        assertTrue(url, url.contains("client_id=abc"))
        assertTrue(url, url.contains("code_challenge=chal"))
        assertTrue(url, url.contains("code_challenge_method=S256"))
        assertTrue(url, url.contains("state=st"))
    }

    @Test
    fun `authorize url percent encodes the redirect uri and scope`() {
        val url = ReadeckAuth.buildAuthorizeUrl(
            serverUrl = "https://readeck.example.com",
            clientId = "abc",
            // The custom scheme's '://' and '/' are not legal unescaped in a
            // query value; an unencoded redirect_uri does not match the
            // registered one and the server refuses to redirect.
            redirectUri = ReadeckAuth.REDIRECT_URI,
            scope = "bookmarks:read bookmarks:write",
            codeChallenge = "chal",
            state = "st",
        )
        val redirect = url.substringAfter("redirect_uri=").substringBefore("&")
        assertTrue(url, redirect.none { it == ':' || it == '/' })
        assertTrue(url, url.contains("scope=bookmarks%3Aread%20bookmarks%3Awrite"))
    }

    @Test
    fun `registration advertises both grants and the redirect uri`() = runTest {
        server.enqueue(MockResponse().setResponseCode(201).setBody("""{"client_id":"abc"}"""))

        ReadeckAuth.startAuthorization(serverUrl(), appVersion)

        val body = server.takeRequest().body.readUtf8()
        val json = Json.parseToJsonElement(body).jsonObject
        // redirect_uris is rejected as missing unless authorization_code is in
        // grant_types, and the two must be offered together.
        assertTrue(body, body.contains("authorization_code"))
        assertTrue(body, body.contains("urn:ietf:params:oauth:grant-type:device_code"))
        assertTrue(
            body,
            json["redirect_uris"].toString().contains(ReadeckAuth.REDIRECT_URI),
        )
    }

    @Test
    fun `authorization flow returns an authorize url and the verifier to keep`() = runTest {
        server.enqueue(MockResponse().setResponseCode(201).setBody("""{"client_id":"abc"}"""))

        val pending = ReadeckAuth.startAuthorization(serverUrl(), appVersion)

        assertEquals("abc", pending.clientId)
        assertEquals(64, pending.codeVerifier.length)
        assertTrue(pending.state.isNotBlank())
        assertTrue(pending.authorizeUrl, pending.authorizeUrl.startsWith(server.url("/").toString().trimEnd('/')))
        // The challenge on the wire has to be the one derived from the verifier
        // we are about to be asked for, or the exchange fails at the last step.
        assertTrue(
            pending.authorizeUrl,
            pending.authorizeUrl.contains(ReadeckAuth.codeChallenge(pending.codeVerifier)),
        )
        assertTrue(pending.authorizeUrl, pending.authorizeUrl.contains("state=${pending.state}"))
    }

    @Test
    fun `exchanges the code with the verifier and no client id`() = runTest {
        server.enqueue(MockResponse().setResponseCode(201).setBody("""{"access_token":"tok-abc"}"""))

        val token = ReadeckAuth.exchangeCode(serverUrl(), "the-code", "the-verifier")

        assertEquals("tok-abc", token)
        val request = server.takeRequest()
        assertEquals("/api/oauth/token", request.path)
        val body = request.body.readUtf8()
        assertTrue(body, body.contains("\"grant_type\":\"authorization_code\""))
        assertTrue(body, body.contains("the-code"))
        assertTrue(body, body.contains("the-verifier"))
        // The authorization_code branch of oauthTokenCreate takes exactly these
        // three; sending client_id as well fails validation.
        assertTrue(body, !body.contains("client_id"))
    }
}
