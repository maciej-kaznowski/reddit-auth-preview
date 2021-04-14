package com.innercirclesoftware.reddit_auth

import com.innercirclesoftware.randoms.randomLong
import com.innercirclesoftware.randoms.randomString
import com.innercirclesoftware.randoms.suchThat
import com.innercirclesoftware.reddit_api.auth.RemoteAccountAuthorization
import com.innercirclesoftware.reddit_auth.persistence.AuthRepository
import com.innercirclesoftware.reddit_auth.rest.RestRedditAuthService
import com.innercirclesoftware.reddit_auth_api.AuthHeader
import com.innercirclesoftware.reddit_auth_api.RedditAuthenticator
import com.innercirclesoftware.reddit_auth_test.TestAccountAuthorization
import com.innercirclesoftware.reddit_auth_test.TestRemoteAccountAuthorizations
import com.innercirclesoftware.shared_test.argThat
import com.innercirclesoftware.shared_test.eq
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers.io
import org.amshove.kluent.Verify
import org.amshove.kluent.When
import org.amshove.kluent.`it answers`
import org.amshove.kluent.`it returns`
import org.amshove.kluent.any
import org.amshove.kluent.called
import org.amshove.kluent.calling
import org.amshove.kluent.mock
import org.amshove.kluent.on
import org.amshove.kluent.that
import org.amshove.kluent.was
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.reset
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class RedditAuthenticatorImplTest {

    private val mockRestService: RestRedditAuthService = mock()
    private val mockAuthRepository: AuthRepository = mock()

    //valid response when getting the authorization for a code
    private val validNewAuthorizationResponse = TestRemoteAccountAuthorizations.create(
            refreshToken = { randomString() }.suchThat { it.isNotBlank() }()
    )

    private val redditAuthenticator: RedditAuthenticator = RedditAuthenticatorImpl(
            restService = mockRestService,
            authRepository = mockAuthRepository,
            authHeader = AuthHeader(clientId = randomString()),
            redirectUri = randomString()
    )

    @Before
    fun setUp() {
        reset(mockRestService, mockAuthRepository)
    }

    @Test
    fun `authorizeNewAccount() does not error`() {
        `when getAccessToken() called return`(validNewAuthorizationResponse)

        redditAuthenticator.authorizeNewAccount(randomString()).test().await()
                .assertNoErrors()
    }

    @Test
    fun `authorizeNewAccount() throws error for unrecognised scope`() {
        `when getAccessToken() called return`(validNewAuthorizationResponse.copy(
                scope = "unknown_scope edit",
        ))

        redditAuthenticator.authorizeNewAccount(randomString()).test().await()
                .assertErrorMessage("Unrecognised scope 'unknown_scope' in 'unknown_scope edit'")
    }

    @Test
    fun `authorizeNewAccount() throws error when refresh token not returned`() {
        `when getAccessToken() called return`(validNewAuthorizationResponse.copy(
                refreshToken = null
        ))

        redditAuthenticator.authorizeNewAccount(randomString()).test().await()
                .assertErrorMessage("Refresh token not provided")
    }

    @Test
    fun `authorizeNewAccount() throws error when token type not recognised`() {
        `when getAccessToken() called return`(validNewAuthorizationResponse.copy(
                type = "unknown type"
        ))

        redditAuthenticator.authorizeNewAccount(randomString()).test().await()
                .assertErrorMessage("Unrecognized token type 'unknown type'")
    }

    @Test
    fun `authorizeAccount() stores authorization for account`() {
        val authorization = TestAccountAuthorization.create()
        redditAuthenticator.authorizeAccount(accountId = 1L, authorization)
        Verify.on(mockAuthRepository).upsert(1L, authorization) was called
    }

    @Test
    fun `reAuthenticate() throws error if account not already authenticated`() {
        val accountId = randomLong()

        When calling mockAuthRepository.getRefreshToken(accountId) `it returns` Flowable.just(Optional.empty())

        redditAuthenticator.reAuthenticate(accountId).test().await()
                .assertErrorMessage("Can not re-authenticate account $accountId as it isn't authorized")
    }

    @Test
    fun `reAuthenticate() uses existing refresh token if not returned from server`() {
        val accountId = { randomLong() }.suchThat { it != 0L }()
        val existingAuthorization = TestAuthorizationEntities.create(accountId = accountId)
        val newAuthorization = TestRemoteAccountAuthorizations.create(refreshToken = null)

        `when getAccessToken() called return`(newAuthorization)
        When calling mockAuthRepository.getRefreshToken(accountId) `it returns` Flowable.just(Optional.of(existingAuthorization.refreshToken))

        redditAuthenticator.reAuthenticate(accountId).test().await()
                .assertNoErrors()
                .assertComplete()

        Verify.on(mockAuthRepository).upsert(any(), argThat { it.refreshToken == existingAuthorization.refreshToken }) was called
    }

    @Test
    fun `reAuthenticate() replaces existing refresh token if issued `() {
        val accountId = { randomLong() }.suchThat { it != 0L }()
        val existingAuthorization = TestAuthorizationEntities.create(accountId = accountId)
        val newAuthorization = TestRemoteAccountAuthorizations.create(refreshToken = { randomString() }.suchThat { it.isNotBlank() }())

        `when getAccessToken() called return`(newAuthorization)
        When calling mockAuthRepository.getRefreshToken(accountId) `it returns` Flowable.just(Optional.of(existingAuthorization.refreshToken))

        redditAuthenticator.reAuthenticate(accountId).test().await()
                .assertNoErrors()
                .assertComplete()

        Verify.on(mockAuthRepository).upsert(any(), argThat { it.refreshToken == newAuthorization.refreshToken }) was called
    }

    @Test
    fun `reAuthenticate() called concurrently updates authentication once`() {
        val accountId = { randomLong() }.suchThat { it != 0L }()
        val existingRefreshToken = { randomString() }.suchThat { it.isNotBlank() }()

        When calling mockAuthRepository.getRefreshToken(accountId) `it returns` Flowable.just(Optional.of(existingRefreshToken))
        When calling mockRestService.getAccessToken(any(), any()) `it answers` {
            Single.just(TestRemoteAccountAuthorizations.create())
                    .delay(randomLong(min = 0, maxExclusive = 1_000), TimeUnit.MILLISECONDS, io())
        }

        val allReAuthentications = (0 until 100).map { redditAuthenticator.reAuthenticate(accountId).subscribeOn(io()).test() }.toList()

        allReAuthentications.forEach { it.await().assertNoErrors().assertComplete() }
        Verify on mockAuthRepository that mockAuthRepository.upsert(any(), any()) was called //asserts called exactly once - can fail if the test took a long time to complete essentially
    }

    @Test
    fun `reAuthenticate() called concurrently with errors returns same error to all subscribers`() {
        val accountId = { randomLong() }.suchThat { it != 0L }()
        val existingRefreshToken = { randomString() }.suchThat { it.isNotBlank() }()

        When calling mockAuthRepository.getRefreshToken(accountId) `it returns` Flowable.just(Optional.of(existingRefreshToken))
        When calling mockRestService.getAccessToken(any(), any()) `it answers` {
            Single.timer(randomLong(min = 0, maxExclusive = 1_000), TimeUnit.MILLISECONDS, io())
                    .flatMap {
                        throw RuntimeException("getAccessToken() error")
                    }
        }

        val allReAuthentications = (0 until 10_000).map { redditAuthenticator.reAuthenticate(accountId).subscribeOn(io()).test() }.toList()

        allReAuthentications.forEach { it.await().assertError(RuntimeException::class.java) }
    }

    @Test
    fun `reAuthenticate() does not cache error results indefinitely`() {
        val accountId = { randomLong() }.suchThat { it != 0L }()
        val existingRefreshToken = { randomString() }.suchThat { it.isNotBlank() }()

        When calling mockAuthRepository.getRefreshToken(accountId) `it returns` Flowable.just(Optional.of(existingRefreshToken))

        val mockRestServiceInvocationCount = AtomicInteger(0) //error on first invocation, but not on second
        When calling mockRestService.getAccessToken(any(), any()) `it answers` {
            when (mockRestServiceInvocationCount.incrementAndGet()) {
                1 -> Single.error(RuntimeException("first invocation"))
                2 -> Single.just(TestRemoteAccountAuthorizations.create())
                else -> throw IllegalArgumentException()
            }
        }

        redditAuthenticator.reAuthenticate(accountId).subscribeOn(io()).test().await().assertErrorMessage("first invocation")
        redditAuthenticator.reAuthenticate(accountId).subscribeOn(io()).test().await().assertComplete()
    }

    @Test
    fun `reAuthenticate() called concurrently for different accounts does not return same result`() {
        val accountIdGen = { randomLong() }.suchThat { it != 0L }
        val account1 = accountIdGen()
        val account2 = accountIdGen.suchThat { it != account1 }()

        val refreshTokenGen = { randomString() }.suchThat { it.isNotBlank() }
        val existingRefreshTokenAcc1 = refreshTokenGen()
        val existingRefreshTokenAcc2 = refreshTokenGen.suchThat { it != existingRefreshTokenAcc1 }()

        When calling mockAuthRepository.getRefreshToken(account1) `it returns` Flowable.just(Optional.of(existingRefreshTokenAcc1))
        When calling mockAuthRepository.getRefreshToken(account2) `it returns` Flowable.just(Optional.of(existingRefreshTokenAcc2))

        When calling mockRestService.getAccessToken(any(), any()) `it answers` {
            Single.just(TestRemoteAccountAuthorizations.create())
        }

        val account1Auth = redditAuthenticator.reAuthenticate(account1)
        val account2Auth = redditAuthenticator.reAuthenticate(account2)

        account1Auth.test().await().completions()
        account2Auth.test().await().completions()

        Verify on mockAuthRepository that mockAuthRepository.upsert(account1.eq(), any()) was called
        Verify on mockAuthRepository that mockAuthRepository.upsert(account2.eq(), any()) was called
    }

    private fun `when getAccessToken() called return`(value: RemoteAccountAuthorization) {
        When calling mockRestService.getAccessToken(any(), any()) `it returns` Single.just(value)
    }
}