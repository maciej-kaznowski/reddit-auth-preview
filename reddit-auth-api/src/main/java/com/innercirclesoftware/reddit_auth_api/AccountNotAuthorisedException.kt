package com.innercirclesoftware.reddit_auth_api

/**
 * Thrown when an account's authorisation does not exist in the database, for example for anonymous accounts.
 * However if an accounts access has been denied, and we then delete the authorization for that account then this
 * exception will be returned: it does not guarantee the account is anonymous, just that they need to connect their account
 */
class AccountNotAuthorisedException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

/**
 * Thrown when the user operating the account has revoked permission for our client to access it.
 * The credentials are not removed from the DB, so if this happens then they should be deleted using [com.innercirclesoftware.reddit_auth.RedditAuthenticator.deleteForAccount]
 */
class AccountAccessDeniedException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)