package com.innercirclesoftware.reddit_auth_test

import com.innercirclesoftware.reddit_auth_api.AccountAuthorization
import com.innercirclesoftware.reddit_auth_api.RedditAuthenticator
import io.reactivex.Completable
import io.reactivex.Completable.never
import io.reactivex.Flowable
import io.reactivex.Single
import java.util.*

class TestRedditAuthenticator(
        val onAuthorizeNewAccount: (code: String) -> Single<AccountAuthorization> = { Single.just(TestAccountAuthorization.create()) },
        val onAuthorizeAccount: (accountId: Long, authorization: AccountAuthorization) -> Unit = { _, _ -> },
        val onReAuthenticate: (accountId: Long) -> Completable = { Completable.complete() },
        val onDeleteForAccount: (accountId: Long) -> Boolean = { true },
        val onRevokeAccessToken: (accountId: Long) -> Boolean = { true },
        val onGetAccessToken: (accountId: Long) -> Flowable<Optional<String>> = { Flowable.just(Optional.of(TestAccountAuthorization.create().accessToken)).concatWith(never()) },
) : RedditAuthenticator {

    override fun authorizeNewAccount(code: String): Single<AccountAuthorization> = onAuthorizeNewAccount(code)
    override fun authorizeAccount(accountId: Long, authorization: AccountAuthorization) = onAuthorizeAccount(accountId, authorization)
    override fun reAuthenticate(accountId: Long) = onReAuthenticate(accountId)
    override fun deleteForAccount(accountId: Long) = onDeleteForAccount(accountId)
    override fun revokeAccessToken(accountId: Long) = onRevokeAccessToken(accountId)
    override fun getAccessToken(accountId: Long) = onGetAccessToken(accountId)
}