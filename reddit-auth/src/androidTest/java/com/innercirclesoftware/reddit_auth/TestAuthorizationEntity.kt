package com.innercirclesoftware.reddit_auth

import com.innercirclesoftware.randoms.randomInt
import com.innercirclesoftware.randoms.randomLong
import com.innercirclesoftware.randoms.randomString
import com.innercirclesoftware.randoms.suchThat
import com.innercirclesoftware.reddit_auth.persistence.AuthorizationEntity
import java.time.Instant
import java.time.Instant.now

internal object TestAuthorizationEntity {

    fun create(
            accountId: Long,
            accessToken: String = { randomString(size = randomInt(min = 1, maxInclusive = 100)) }.suchThat { it.isNotBlank() }(),
            refreshToken: String = { randomString() }.suchThat { it.isNotBlank() }(),
            expires: Instant = Instant.ofEpochMilli(randomLong(min = now().toEpochMilli())),
    ) = AuthorizationEntity(
            accountId = accountId,
            accessToken = accessToken,
            refreshToken = refreshToken,
            expires = expires
    )

}