package com.innercirclesoftware.reddit_auth_api

import okhttp3.Credentials

data class AuthHeader(val clientId: String, val password: String = "") {

    private val header: String = Credentials.basic(clientId, password)

    override fun toString(): String = header

}