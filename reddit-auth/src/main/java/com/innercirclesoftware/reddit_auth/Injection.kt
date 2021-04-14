package com.innercirclesoftware.reddit_auth

import android.content.Context
import androidx.room.Room
import com.innercirclesoftware.reddit_auth.persistence.AuthRepository
import com.innercirclesoftware.reddit_auth.persistence.AuthRepositoryImpl
import com.innercirclesoftware.reddit_auth.persistence.AuthorizationDao
import com.innercirclesoftware.reddit_auth.persistence.AuthorizationDatabase
import com.innercirclesoftware.reddit_auth.rest.RestRedditAuthService
import com.innercirclesoftware.reddit_auth_api.AuthHeader
import com.innercirclesoftware.reddit_auth_api.RedditAuthenticator
import dagger.Binds
import dagger.BindsInstance
import dagger.Component
import dagger.Module
import dagger.Provides
import retrofit2.Retrofit
import retrofit2.create
import javax.inject.Named
import javax.inject.Singleton

@Singleton
@Component(modules = [RedditAuthModule::class, DatabaseModule::class, NetworkModule::class])
internal interface RedditAuthComponent {

    val redditAuthenticator: RedditAuthenticator

    @Component.Builder
    interface Builder {

        @BindsInstance
        fun bindsContext(context: Context): Builder

        @BindsInstance
        fun bindsRedirectUri(@Named("redirect-uri") redirectUri: String): Builder

        @BindsInstance
        fun bindsAuthHeader(authHeader: AuthHeader): Builder

        @BindsInstance
        fun bindsRetrofit(retrofit: Retrofit): Builder

        fun build(): RedditAuthComponent

    }
}

@Module
internal interface RedditAuthModule {

    @Binds
    fun bindsRedditAuthenticator(impl: RedditAuthenticatorImpl): RedditAuthenticator

    @Binds
    fun bindsAuthRepository(impl: AuthRepositoryImpl): AuthRepository

}


@Module
internal object DatabaseModule {

    @Provides
    @Singleton
    fun providesDatabase(context: Context): AuthorizationDatabase {
        return Room.databaseBuilder(context, AuthorizationDatabase::class.java, "reddit-auth-db").build()
    }

    @Provides
    @Singleton
    fun providesAuthDao(database: AuthorizationDatabase): AuthorizationDao {
        return database.authorizationDao()
    }
}

@Module
internal object NetworkModule {

    @Provides
    @Singleton
    fun providesRestService(retrofit: Retrofit): RestRedditAuthService = retrofit.create()

}