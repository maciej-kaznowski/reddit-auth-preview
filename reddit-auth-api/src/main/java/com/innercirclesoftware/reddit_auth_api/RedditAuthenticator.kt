package com.innercirclesoftware.reddit_auth_api

import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single
import java.util.*

interface RedditAuthenticator {

    /**
     * Authorizes a new account with reddit.
     * This involves using the code to get a non-empty refresh and access tokens. This does not persist any of the resulting information.
     */
    fun authorizeNewAccount(
            code: String,
    ): Single<AccountAuthorization>

    /**
     * Persist the authorization for an account synchronously.
     * If an account is already authenticated, then it's authorization will be replaced.
     */
    fun authorizeAccount(accountId: Long, authorization: AccountAuthorization)

    /**
     * Gets new tokens for the provided account and persists them.
     * If an account isn't already authenticated, then the completable will error with [AccountNotAuthorisedException]
     */
    fun reAuthenticate(accountId: Long): Completable

    /**
     * Deletes the authorization for the provided account.
     * After this, the user will have to re-authorize the app.
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

    fun requireAccessTokenHeader(accountId: Long): Flowable<TokenHeader> {
        return getAccessToken(accountId).map { accessTokenOpt ->
            val accessToken: String = accessTokenOpt.orElseThrow { AccountNotAuthorisedException("Account '$accountId' is not authorised") }
            TokenHeader.forAccessToken(accessToken)
        }
    }
}