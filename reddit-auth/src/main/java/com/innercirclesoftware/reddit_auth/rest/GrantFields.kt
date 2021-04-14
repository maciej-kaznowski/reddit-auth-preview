package com.innercirclesoftware.reddit_auth.rest


internal class GrantFields private constructor(fields: Map<String, String>) : Map<String, String> by fields {

    companion object {

        fun fromRefreshToken(refreshToken: String) = GrantFields(mapOf(
                Fields.GRANT_TYPE to GrantTypes.TOKEN,
                "refresh_token" to refreshToken,
        )
        )

        fun fromCode(code: String, redirectUri: String) = GrantFields(mapOf(
                Fields.GRANT_TYPE to GrantTypes.CODE,
                "code" to code,
                "redirect_uri" to redirectUri,
        )
        )
    }

    override fun equals(other: Any?): Boolean {
        return super.equals(other)
    }

    override fun hashCode(): Int {
        return super.hashCode()
    }
}

private object Fields {

    const val GRANT_TYPE = "grant_type"

}

private object GrantTypes {

    const val TOKEN = "refresh_token"
    const val CODE = "authorization_code"

}