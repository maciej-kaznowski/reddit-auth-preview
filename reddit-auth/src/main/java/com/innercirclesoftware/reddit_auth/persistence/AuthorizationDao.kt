package com.innercirclesoftware.reddit_auth.persistence

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import io.reactivex.Flowable

@Dao
internal abstract class AuthorizationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun upsert(entity: AuthorizationEntity)

    @Query("DELETE FROM authorization_scopes WHERE account_id = :accountId")
    abstract fun deleteScopesForAccount(accountId: Long)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    abstract fun insertScopes(scopes: List<AuthorizationScopeEntity>)

    @Transaction
    open fun upsertAuthorizationForAccount(
            accountId: Long,
            authorizationEntity: AuthorizationEntity,
            newScopes: List<AuthorizationScopeEntity>,
    ) {
        require(newScopes.all { it.id == 0L })

        upsert(authorizationEntity)
        deleteScopesForAccount(accountId)
        insertScopes(newScopes)
    }


    @Query("SELECT refresh_token FROM authorization WHERE account_id = :accountId")
    abstract fun getRefreshToken(accountId: Long): Flowable<List<String>>

    @Query("SELECT account_id FROM authorization WHERE account_id = :accountId LIMIT 1")
    abstract fun existsByAccountId(accountId: Long): Array<Long>

    @Query("DELETE FROM authorization WHERE account_id = :accountId")
    abstract fun deleteByAccountId(accountId: Long): Int

    @Query("UPDATE authorization SET access_token = :newAccessToken WHERE account_id = :accountId AND access_token != :newAccessToken")
    abstract fun replaceAccessToken(accountId: Long, newAccessToken: String): Int

    @Query("SELECT access_token FROM authorization WHERE account_id = :accountId")
    abstract fun getAccessToken(accountId: Long): Flowable<Array<String>>


    @Query("SELECT * FROM authorization WHERE account_id = :accountId")
    abstract fun getAuthorizationByAccount(accountId: Long): AuthorizationEntity?

    @Query("SELECT * FROM authorization_scopes WHERE account_id = :accountId")
    abstract fun getScopesForAccount(accountId: Long): List<AuthorizationScopeEntity>

}