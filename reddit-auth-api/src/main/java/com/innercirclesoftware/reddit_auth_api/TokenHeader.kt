package com.innercirclesoftware.reddit_auth_api

data class TokenHeader constructor(private val value: String) {

    override fun toString() = value

    companion object {

        fun forAccessToken(accessToken: String): TokenHeader {
            require(accessToken.isNotBlank()) { "Cannot create a TokenHeader from a blank access token" }
            return TokenHeader("bearer $accessToken")
        }
    }
}