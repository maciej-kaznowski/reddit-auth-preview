package com.innercirclesoftware.reddit_auth.persistence

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "authorization")
internal data class AuthorizationEntity(
        @PrimaryKey(autoGenerate = false) @ColumnInfo(name = "account_id", index = true) val accountId: Long,

        @ColumnInfo(name = "access_token") val accessToken: String,
        @ColumnInfo(name = "refresh_token") val refreshToken: String,
        @ColumnInfo(name = "expires") val expires: Instant,
) {

    init {
        require(accountId != 0L) { "AccountId must be defined" }
        require(accessToken.isNotBlank()) { "Access token must not be blank" }
        require(refreshToken.isNotBlank()) { "Refresh token must not be blank" }
    }

}