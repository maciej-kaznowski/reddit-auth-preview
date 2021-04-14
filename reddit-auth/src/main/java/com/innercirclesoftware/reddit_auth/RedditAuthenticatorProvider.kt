package com.innercirclesoftware.reddit_auth

import android.content.Context
import com.innercirclesoftware.reddit_auth_api.AuthHeader
import com.innercirclesoftware.reddit_auth_api.RedditAuthenticator
import retrofit2.Retrofit

object RedditAuthenticatorProvider {

    fun createRedditAuthenticator(
            context: Context,
            redirectUri: String,
            authHeader: AuthHeader,
            retrofit: Retrofit,
    ): RedditAuthenticator {

        val component = DaggerRedditAuthComponent.builder()
                .bindsAuthHeader(authHeader)
                .bindsContext(context)
                .bindsRedirectUri(redirectUri)
                .bindsRetrofit(retrofit)
                .build()

        return component.redditAuthenticator
    }

}