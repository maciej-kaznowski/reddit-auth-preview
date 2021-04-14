package com.innercirclesoftware.reddit_auth.persistence

import com.innercirclesoftware.reddit_auth_api.AccountAuthorization
import io.reactivex.Flowable
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

internal interface AuthRepository {

    /**
     * Get the refresh token for an account.
     * If an account isn't authorised, then an empty token is returned. If later on a token is added for that account then it will be emitted.
     */
    fun getRefreshToken(accountId: Long): Flowable<Optional<String>>

    /**
     * Check if the provided account is authorized
     */
    fun isAuthorized(accountId: Long): Boolean

    /**
     * Upsert a new authorization. If a previous authorization existed for the account then any entities scopes which
     * do not belong to the new authorization will be removed.
     *
     */
    fun upsert(accountId: Long, authorization: AccountAuthorization)

    /**
     * Deletes the authorization and it's scopes for the provided account.
     *
     * @return true if the account was authorized
     */
    fun deleteForAccount(accountId: Long): Boolean

    /**
     * Revoked just the access token for an account.
     *
     * @return true if the account had a non-revoked access token. false if it was already revoked, or the account isn't authorised.
     */
    fun revokeAccessToken(accountId: Long): Boolean

    /**
     * Gets the current access token for an account.
     * If the accounts access token has been revoked then it will still be returned.
     *
     * @return an empty optional if the account isn't authorized.
     *
     */
    fun getAccessToken(accountId: Long): Flowable<Optional<String>>

}


@Singleton
internal class AuthRepositoryImpl @Inject constructor(
        private val authorizationDao: AuthorizationDao,
) : AuthRepository {

    override fun getRefreshToken(accountId: Long): Flowable<Optional<String>> {
        return authorizationDao.getRefreshToken(accountId)
                .map { refreshTokens ->
                    if (refreshTokens.size > 1) {
                        throw IllegalStateException("Multiple refresh tokens exist for account $accountId")
                    }

                    Optional.ofNullable(refreshTokens.firstOrNull())
                }
                .distinctUntilChanged()
    }

    override fun isAuthorized(accountId: Long): Boolean {
        return authorizationDao.existsByAccountId(accountId).isNotEmpty()
    }

    override fun upsert(accountId: Long, authorization: AccountAuthorization) {
        val authorizationEntity = with(authorization) {
            AuthorizationEntity(
                    accountId = accountId,
                    accessToken = accessToken,
                    refreshToken = refreshToken,
                    expires = expires,
            )
        }

        val scopes = authorization.scopes.map { scope ->
            AuthorizationScopeEntity(value = scope.value, accountId = accountId)
        }

        authorizationDao.upsertAuthorizationForAccount(
                accountId = accountId,
                authorizationEntity = authorizationEntity,
                newScopes = scopes,
        )
    }

    override fun deleteForAccount(accountId: Long): Boolean {
        return authorizationDao.deleteByAccountId(accountId) == 1
    }

    override fun revokeAccessToken(accountId: Long): Boolean {
        return authorizationDao.replaceAccessToken(accountId, "revoked") == 1 //we can't use an empty token as reddit messes up with that
    }

    override fun getAccessToken(accountId: Long): Flowable<Optional<String>> {
        return authorizationDao.getAccessToken(accountId)
                .map { accessTokens ->
                    if (accessTokens.size > 1) {
                        throw IllegalStateException("Multiple access tokens exist for account $accountId")
                    }

                    Optional.ofNullable(accessTokens.firstOrNull())
                }
                .distinctUntilChanged()
    }
}