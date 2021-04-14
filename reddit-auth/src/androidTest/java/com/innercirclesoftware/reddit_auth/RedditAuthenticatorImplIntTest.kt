package com.innercirclesoftware.reddit_auth

import android.content.Context
import android.os.StrictMode
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.innercirclesoftware.mock_utils.MockResponses
import com.innercirclesoftware.mock_utils.dispatcher
import com.innercirclesoftware.randoms.randomLong
import com.innercirclesoftware.randoms.suchThat
import com.innercirclesoftware.reddit_api.moshi.addAdapters
import com.innercirclesoftware.reddit_auth.persistence.AuthRepositoryImpl
import com.innercirclesoftware.reddit_auth.persistence.AuthorizationDatabase
import com.innercirclesoftware.reddit_auth.rest.RestRedditAuthService
import com.innercirclesoftware.reddit_auth_api.AuthHeader
import com.squareup.moshi.Moshi
import io.reactivex.schedulers.Schedulers
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.moshi.MoshiConverterFactory
import java.time.Duration

@RunWith(AndroidJUnit4::class)
class RedditAuthenticatorImplIntTest {

    private lateinit var authenticator: RedditAuthenticatorImpl
    private lateinit var webServer: MockWebServer

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AuthorizationDatabase::class.java).build()

        webServer = MockWebServer().apply {
            StrictMode.setThreadPolicy(
                    StrictMode.ThreadPolicy.Builder(StrictMode.ThreadPolicy.LAX)
                            .permitNetwork()
                            .build()
            )
        }

        webServer.dispatcher { request ->
            when {
                isRequestForAuthFromCode(request) -> MockResponse().apply {
                    setResponseCode(200)
                    setBody("""
                        {
                          "access_token": "access_token_for_auth_code",
                          "token_type": "bearer",
                          "expires_in": 3600,
                          "refresh_token": "refresh_token_for_auth_code",
                          "scope": "edit flair history identity modconfig modflair modlog modposts modwiki mysubreddits privatemessages read report save submit subscribe vote wikiedit wikiread"
                        }
                    """.trimIndent())
                }
                isRequestForAuthFromRefreshToken(request) -> MockResponse().apply {
                    setResponseCode(200)
                    setBody("""
                        {
                          "access_token": "access_token_from_refresh_token",
                          "token_type": "bearer",
                          "expires_in": 3600,
                          "refresh_token": "refresh_token_from_refresh_token",
                          "scope": "edit flair history identity modconfig modflair modlog modposts modwiki mysubreddits privatemessages read report save submit subscribe vote wikiedit wikiread"
                        }
                    """.trimIndent())
                }
                else -> MockResponses.notFound()
            }
        }

        webServer.start()

        val moshi = Moshi.Builder().addAdapters().build()
        val okHttpClient = OkHttpClient.Builder()
                .addInterceptor(HttpLoggingInterceptor())
                .connectTimeout(Duration.ofSeconds(60))
                .readTimeout(Duration.ofSeconds(60))
                .writeTimeout(Duration.ofSeconds(60))
                .build()

        val retrofit = Retrofit.Builder()
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .addCallAdapterFactory(RxJava2CallAdapterFactory.createWithScheduler(Schedulers.io()))
                .client(okHttpClient)
                .baseUrl(webServer.url("/"))
                .build()

        val restService = retrofit.create(RestRedditAuthService::class.java)

        authenticator = RedditAuthenticatorImpl(
                restService = restService,
                authRepository = AuthRepositoryImpl(db.authorizationDao()),
                authHeader = AuthHeader(clientId = "test"),
                redirectUri = "com.innercirclesoftware.reddit://reddit_oauth_redirect"
        )
    }

    private fun isRequestForAuthFromRefreshToken(request: RecordedRequest): Boolean {
        val url = request.requestUrl ?: return false

        //check path
        return when {
            "api/v1/access_token" != url.pathSegments.joinToString("/") -> false

            //query params
            "1" != url.queryParameter("raw_json") -> false

            //headers
            "Basic dGVzdDo=" != request.getHeader("Authorization") -> false
            "application/x-www-form-urlencoded" != request.getHeader("Content-Type") -> false

            //request form fields (form url encoded)
            "grant_type=refresh_token&refresh_token=refresh_token_for_auth_code" != request.body.snapshot().utf8() -> false

            //success!!
            else -> true
        }

    }

    private fun isRequestForAuthFromCode(request: RecordedRequest): Boolean {
        val url = request.requestUrl ?: return false

        //check path
        return when {
            "api/v1/access_token" != url.pathSegments.joinToString("/") -> false

            //query params
            "1" != url.queryParameter("raw_json") -> false

            //headers
            "Basic dGVzdDo=" != request.getHeader("Authorization") -> false
            "application/x-www-form-urlencoded" != request.getHeader("Content-Type") -> false

            //request form fields (form url encoded)
            "grant_type=authorization_code&code=oauth_code&redirect_uri=com.innercirclesoftware.reddit%3A%2F%2Freddit_oauth_redirect" != request.body.snapshot().utf8() -> false

            //success!!
            else -> true
        }
    }

    @After
    fun after() {
        webServer.close()
    }

    @Test
    fun testAuthenticationFlow() {
        val code = "oauth_code"
        val authorization = authenticator.authorizeNewAccount(code).blockingGet()

        val accountId = { randomLong() }.suchThat { it != 0L }()
        authenticator.authorizeAccount(
                accountId = accountId,
                authorization = authorization
        )

        var accessToken = authenticator.getAccessToken(accountId).blockingFirst().get()
        accessToken shouldBeEqualTo "access_token_for_auth_code"

        authenticator.reAuthenticate(accountId).blockingAwait()

        accessToken = authenticator.getAccessToken(accountId).blockingFirst().get()
        accessToken shouldBeEqualTo "access_token_from_refresh_token"
    }
}