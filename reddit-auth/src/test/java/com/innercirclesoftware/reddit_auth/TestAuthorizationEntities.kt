package com.innercirclesoftware.reddit_auth

import com.innercirclesoftware.randoms.randomLong
import com.innercirclesoftware.randoms.randomString
import com.innercirclesoftware.randoms.suchThat
import com.innercirclesoftware.reddit_auth.persistence.AuthorizationEntity
import java.time.Instant

internal object TestAuthorizationEntities {

    fun create(
            accountId: Long = { randomLong() }.suchThat { it != 0L }(),
            accessToken: String = { randomString() }.suchThat { it.isNotBlank() }(),
            refreshToken: String = { randomString() }.suchThat { it.isNotBlank() }(),
            expires: Instant = Instant.ofEpochMilli(randomLong()),
    ) = AuthorizationEntity(
            accountId = accountId,
            accessToken = accessToken,
            refreshToken = refreshToken,
            expires = expires,
    )

}