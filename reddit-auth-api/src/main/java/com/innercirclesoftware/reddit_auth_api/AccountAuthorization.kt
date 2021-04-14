package com.innercirclesoftware.reddit_auth_api

import java.time.Instant

/**
 * @param refreshToken always non-empty, this is long-lived but can change. Use to get a new access token
 * @param accessToken always non-empty, this is short-lived and is used to authenticate each reddit network request. Typically valid for 60 minutes, but not guaranteed.
 * @param tokenType constant: bearer
 * @param expires the UTC time when the token is guaranteed to be expired by. It may expire before this time due to latency in getting this time.
 * @param scopes a set of scopes that we are authorized to access with the access token.
 *
 */
data class AccountAuthorization(
        val refreshToken: String,
        val accessToken: String,
        val tokenType: TokenType,
        val expires: Instant,
        val scopes: Set<Scope>,
) {

    init {
        require(refreshToken.isNotEmpty()) { "Refresh token cannot be empty" }
        require(accessToken.isNotEmpty()) { "Access token cannot be empty" }
    }

    enum class Scope(val value: String) {

        EDIT("edit"),
        FLAIR("flair"),
        HISTORY("history"),
        IDENTITY("identity"),

        MOD_CONFIG("modconfig"),
        MOD_FLAIR("modflair"),
        MOD_LOG("modlog"),
        MOD_POSTS("modposts"),
        MOD_WIKI("modwiki"),

        MY_SUBREDDITS("mysubreddits"),
        PRIVATE_MESSAGES("privatemessages"),
        READ("read"),
        REPORT("report"),
        SAVE("save"),
        SUBMIT("submit"),
        SUBSCRIBE("subscribe"),
        VOTE("vote"),
        WIKI_EDIT("wikiedit"),
        WIKI_READ("wikiread");

        companion object {

            fun parse(value: String): Scope? {
                return values().firstOrNull { it.value == value }
            }
        }
    }

    enum class TokenType(val value: String) {

        BEARER("bearer");

        companion object {

            fun parse(value: String): TokenType? {
                return values().firstOrNull { it.value == value }
            }

        }
    }
}