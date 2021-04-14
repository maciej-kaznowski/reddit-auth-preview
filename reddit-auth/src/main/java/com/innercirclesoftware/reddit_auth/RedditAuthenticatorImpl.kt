package com.innercirclesoftware.reddit_auth

import com.innercirclesoftware.reddit_api.auth.RemoteAccountAuthorization
import com.innercirclesoftware.reddit_auth.persistence.AuthRepository
import com.innercirclesoftware.reddit_auth.rest.GrantFields
import com.innercirclesoftware.reddit_auth.rest.RestRedditAuthService
import com.innercirclesoftware.reddit_auth_api.AccountAccessDeniedException
import com.innercirclesoftware.reddit_auth_api.AccountAuthorization
import com.innercirclesoftware.reddit_auth_api.AccountNotAuthorisedException
import com.innercirclesoftware.reddit_auth_api.AuthHeader
import com.innercirclesoftware.reddit_auth_api.RedditAuthenticator
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single
import retrofit2.HttpException
import java.time.Instant
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
internal class RedditAuthenticatorImpl @Inject constructor(
        private val restService: RestRedditAuthService,
        private val authRepository: AuthRepository,
        private val authHeader: AuthHeader,
        @Named("redirect-uri") private val redirectUri: String,
) : RedditAuthenticator {

    override fun authorizeNewAccount(
            code: String,
    ): Single<AccountAuthorization> {
        return restService
                .getAccessToken(
                        authHeader,
                        GrantFields.fromCode(code = code, redirectUri = redirectUri)
                )
                .map { remoteAuthorization -> remoteAuthorization.toModel() }
    }

    override fun authorizeAccount(accountId: Long, authorization: AccountAuthorization) {
        authRepository.upsert(accountId, authorization)
    }

    private val reAuthenticationRequests = ConcurrentHashMap<Long, Completable>()

    override fun reAuthenticate(accountId: Long): Completable {
        return reAuthenticationRequests.getOrPut(accountId) {
            authRepository.getRefreshToken(accountId)
                    .map { refreshTokenOpt -> refreshTokenOpt.orElseThrow { AccountNotAuthorisedException("Can not re-authenticate account $accountId as it isn't authorized") } }
                    .switchMap { existingRefreshToken ->
                        restService.getAccessToken(authHeader, GrantFields.fromRefreshToken(existingRefreshToken))
                                .map { remoteAuthorization -> remoteAuthorization.toModel(refreshTokenIfNull = existingRefreshToken) } //in future the remote will always return a refresh token
                                .doOnSuccess { authorization -> authRepository.upsert(accountId, authorization) }
                                .toFlowable()
                    }
                    .firstOrError()
                    .ignoreElement()
                    .doOnTerminate { reAuthenticationRequests.remove(accountId) }
                    .onErrorResumeNext { cause ->
                        when {
                            cause is HttpException && cause.code() == 400 -> Completable.error(AccountAccessDeniedException(
                                    message = "Could not renew authorisation for account $accountId as access has been denied.",
                                    cause
                            ))
                            else -> Completable.error(cause)
                        }
                    }
                    .cache()
        }
    }

    override fun deleteForAccount(accountId: Long): Boolean {
        return authRepository.deleteForAccount(accountId)
    }

    override fun revokeAccessToken(accountId: Long): Boolean {
        return authRepository.revokeAccessToken(accountId)
    }

    override fun getAccessToken(accountId: Long): Flowable<Optional<String>> {
        return authRepository.getAccessToken(accountId)
    }
}

private fun RemoteAccountAuthorization.toModel(
        refreshTokenIfNull: String? = null,
): AccountAuthorization {
    return AccountAuthorization(
            refreshToken = requireNotNull(refreshToken ?: refreshTokenIfNull) {
                "Refresh token not provided" //only null when getting a new access token
            },
            accessToken = accessToken,
            tokenType = requireNotNull(AccountAuthorization.TokenType.parse(type)) { "Unrecognized token type '$type'" },
            expires = Instant.now().minusSeconds(expiresIn), /*does not account for latency*/
            scopes = scope.splitToSequence(' ')
                    .filter { it.isNotBlank() }
                    .map { scopeStr -> requireNotNull(AccountAuthorization.Scope.parse(scopeStr)) { "Unrecognised scope '$scopeStr' in '$scope'" } }
                    .toSet(),
    )
}