package com.innercirclesoftware.reddit_auth.persistence

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.innercirclesoftware.randoms.randomListOfSize
import com.innercirclesoftware.randoms.randomLong
import com.innercirclesoftware.randoms.randomSetOfSize
import com.innercirclesoftware.randoms.randomString
import com.innercirclesoftware.randoms.suchThat
import com.innercirclesoftware.reddit_auth.TestAuthorizationEntity
import com.innercirclesoftware.reddit_auth.TestAuthorizationScopeEntity
import org.amshove.kluent.shouldBeEmpty
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldHaveSize
import org.amshove.kluent.shouldNotBeNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AuthorizationDaoTest {

    private lateinit var dao: AuthorizationDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AuthorizationDatabase::class.java).build()
        dao = db.authorizationDao()
    }

    @Test
    fun testInsertAuthorizationForNewAccount() {
        val accountId = randomInsertedAccountId()
        dao.getAuthorizationByAccount(accountId).shouldBeNull()

        val authorizationEntity = TestAuthorizationEntity.create(accountId = accountId)
        dao.upsertAuthorizationForAccount(
                accountId = accountId,
                authorizationEntity = authorizationEntity,
                newScopes = randomListOfSize() { TestAuthorizationScopeEntity.create(accountId = accountId) }
        )

        val inserted = dao.getAuthorizationByAccount(accountId)
        inserted.shouldNotBeNull()
        inserted.accountId shouldBeEqualTo accountId
    }

    @Test
    fun testInsertAuthorizationForExistingAccountDeletesExistingScopes() {
        val accountId = randomInsertedAccountId()
        dao.getAuthorizationByAccount(accountId).shouldBeNull()
        dao.getScopesForAccount(accountId).shouldBeEmpty()

        val authorizationEntity = TestAuthorizationEntity.create(accountId = accountId)
        dao.upsertAuthorizationForAccount(
                accountId = accountId,
                authorizationEntity = authorizationEntity,
                newScopes = randomSetOfSize(3) { TestAuthorizationScopeEntity.create(accountId = accountId) }.toList() //don't insert duplicate scopes
        )
        dao.getAuthorizationByAccount(accountId).shouldNotBeNull()
        dao.getScopesForAccount(accountId) shouldHaveSize 3

        val newAuthorizationEntity = TestAuthorizationEntity.create(accountId = accountId)
        val newScopes = randomSetOfSize(5) { TestAuthorizationScopeEntity.create(accountId = accountId) }.toList()
        dao.upsertAuthorizationForAccount(
                accountId = accountId,
                authorizationEntity = newAuthorizationEntity,
                newScopes = newScopes,
        )
        dao.getAuthorizationByAccount(accountId) shouldBeEqualTo newAuthorizationEntity
        dao.getScopesForAccount(accountId) shouldHaveSize 5
    }

    @Test
    fun testReplaceAccessTokenForExistingAccount() {
        val accountId = randomInsertedAccountId()
        val authorizationEntity = TestAuthorizationEntity.create(accountId = accountId)
        dao.upsertAuthorizationForAccount(
                accountId = accountId,
                authorizationEntity = authorizationEntity,
                newScopes = randomListOfSize { TestAuthorizationScopeEntity.create(accountId = accountId) }
        )

        val existingAccessToken = dao.getAccessToken(accountId).blockingFirst().first()
        existingAccessToken shouldBeEqualTo authorizationEntity.accessToken

        val replaceWithToken = { randomString() }.suchThat { it != existingAccessToken }()
        dao.replaceAccessToken(
                accountId = accountId,
                newAccessToken = replaceWithToken
        )
        val replacedAccessToken = dao.getAccessToken(accountId).blockingFirst().first()
        replacedAccessToken shouldBeEqualTo replaceWithToken
    }
}


private fun randomInsertedAccountId() = { randomLong() }.suchThat { it != 0L }()
