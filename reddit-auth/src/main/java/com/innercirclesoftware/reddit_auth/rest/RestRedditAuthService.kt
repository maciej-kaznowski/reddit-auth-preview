package com.innercirclesoftware.reddit_auth.rest

import com.innercirclesoftware.reddit_api.auth.RemoteAccountAuthorization
import com.innercirclesoftware.reddit_auth_api.AuthHeader
import io.reactivex.Single
import retrofit2.http.FieldMap
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Header
import retrofit2.http.POST

internal interface RestRedditAuthService {

    @POST("api/v1/access_token?raw_json=1")
    @FormUrlEncoded
    fun getAccessToken(
            @Header("Authorization") authHeader: AuthHeader,
            @FieldMap grantFields: GrantFields,
    ): Single<RemoteAccountAuthorization>

}
