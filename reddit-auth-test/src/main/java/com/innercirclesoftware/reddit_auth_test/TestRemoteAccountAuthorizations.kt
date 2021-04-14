package com.innercirclesoftware.reddit_auth_test

import com.innercirclesoftware.randoms.orNull
import com.innercirclesoftware.randoms.randomInt
import com.innercirclesoftware.randoms.randomLong
import com.innercirclesoftware.randoms.randomSetOfSize
import com.innercirclesoftware.randoms.randomString
import com.innercirclesoftware.randoms.suchThat
import com.innercirclesoftware.reddit_api.auth.RemoteAccountAuthorization
import com.innercirclesoftware.reddit_auth_api.AccountAuthorization
import java.time.Instant

object TestRemoteAccountAuthorizations {

    fun create(
            accessToken: String = { randomString(size = randomInt(min = 1, maxInclusive = 100)) }.suchThat { it.isNotBlank() }(),
            type: String = AccountAuthorization.TokenType.values().random().value,
            expiresIn: Long = randomLong(min = 0L, maxExclusive = (Long.MAX_VALUE - Instant.now().toEpochMilli()) / 1000L), //seconds in future
            refreshToken: String? = { randomString() }.suchThat { it.isNotBlank() }.orNull()(),
            scope: String = randomSetOfSize(size = randomInt(
                    min = 0,
                    maxInclusive = AccountAuthorization.Scope.values().size
            )) { AccountAuthorization.Scope.values().random().value }.joinToString(separator = " "),
    ): RemoteAccountAuthorization {
        return RemoteAccountAuthorization(
                accessToken = accessToken,
                type = type,
                expiresIn = expiresIn,
                refreshToken = refreshToken,
                scope = scope
        )
    }
}