package com.innercirclesoftware.reddit_auth

import com.innercirclesoftware.reddit_auth.persistence.AuthorizationScopeEntity
import com.innercirclesoftware.reddit_auth_api.AccountAuthorization

internal object TestAuthorizationScopeEntity {

    fun create(
            id: Long = 0L,
            value: String = AccountAuthorization.Scope.values().random().value,
            accountId: Long,
    ) = AuthorizationScopeEntity(
            id = id,
            value = value,
            accountId = accountId,
    )

}