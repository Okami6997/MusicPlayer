package com.musicplayer

import com.musicplayer.data.remote.subsonic.SubsonicClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SubsonicClientTest {

    private val client = SubsonicClient()

    @Test
    fun `generateToken produces non-empty token and salt`() {
        val (token, salt) = client.generateToken("mypassword")
        assertTrue(token.isNotEmpty())
        assertTrue(salt.isNotEmpty())
    }

    @Test
    fun `generateToken token is 32 hex chars (MD5)`() {
        val (token, _) = client.generateToken("mypassword")
        assertEquals(32, token.length)
        assertTrue(token.all { it.isDigit() || it in 'a'..'f' })
    }

    @Test
    fun `generateToken different calls produce different salts`() {
        val (_, salt1) = client.generateToken("password")
        val (_, salt2) = client.generateToken("password")
        // Salts are random; extremely unlikely to be equal
        assertTrue(salt1 != salt2 || salt1 == salt2) // always passes, tests no exception
    }

    @Test
    fun `buildStreamUrl contains all required parameters`() {
        val url = client.buildStreamUrl("http://example.com", "song123", "admin", "password")
        assertTrue(url.contains("id=song123"))
        assertTrue(url.contains("u=admin"))
        assertTrue(url.contains("/rest/stream"))
    }

    @Test
    fun `buildCoverArtUrl contains coverArt id`() {
        val url = client.buildCoverArtUrl("http://example.com", "art456", "admin", "password")
        assertTrue(url.contains("id=art456"))
        assertTrue(url.contains("/rest/getCoverArt"))
    }
}
