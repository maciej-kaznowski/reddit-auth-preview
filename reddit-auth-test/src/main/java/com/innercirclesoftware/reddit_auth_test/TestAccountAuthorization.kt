package com.innercirclesoftware.reddit_auth_test

import com.innercirclesoftware.randoms.randomInt
import com.innercirclesoftware.randoms.randomLong
import com.innercirclesoftware.randoms.randomSetOfSize
import com.innercirclesoftware.randoms.randomString
import com.innercirclesoftware.randoms.suchThat
import com.innercirclesoftware.reddit_auth_api.AccountAuthorization
import java.time.Instant
import java.time.Instant.now

object TestAccountAuthorization {

    fun create(
            refreshToken: String = { randomString() }.suchThat { it.isNotBlank() }(),
            accessToken: String = { randomString() }.suchThat { it.isNotBlank() }(),
            tokenType: AccountAuthorization.TokenType = AccountAuthorization.TokenType.values().random(),
            expires: Instant = Instant.ofEpochMilli(randomLong(min = now().toEpochMilli())),
            scopes: Set<AccountAuthorization.Scope> = randomSetOfSize(size = randomInt(min = 0, maxInclusive = AccountAuthorization.Scope.values().size)) {
                AccountAuthorization.Scope.values().random()
            },
    ) = AccountAuthorization(
            refreshToken = refreshToken,
            accessToken = accessToken,
            tokenType = tokenType,
            expires = expires,
            scopes = scopes,
    )
}