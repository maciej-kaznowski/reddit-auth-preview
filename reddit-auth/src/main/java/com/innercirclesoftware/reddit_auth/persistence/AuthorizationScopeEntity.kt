package com.innercirclesoftware.reddit_auth.persistence

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
        tableName = "authorization_scopes",
        foreignKeys = [
            ForeignKey(entity = AuthorizationEntity::class, parentColumns = ["account_id"], childColumns = ["account_id"], onDelete = ForeignKey.CASCADE)
        ]
)
internal class AuthorizationScopeEntity(
        @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") val id: Long = 0,
        @ColumnInfo(name = "value") val value: String,
        @ColumnInfo(name = "account_id", index = true) val accountId: Long,
) {


    init {
        require(value.isNotBlank()) { "Value cannot be blank" }
        require(accountId != 0L) { "accountId must be defined" }
    }
}