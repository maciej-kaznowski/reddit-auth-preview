package com.innercirclesoftware.reddit_auth.persistence

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import java.time.Instant


@Database(
        entities = [AuthorizationEntity::class, AuthorizationScopeEntity::class],
        version = 1,
)
@TypeConverters(InstantTypeConverter::class)
internal abstract class AuthorizationDatabase : RoomDatabase() {

    abstract fun authorizationDao(): AuthorizationDao

}

private object InstantTypeConverter {

    @TypeConverter
    fun toLong(instant: Instant): Long {
        return instant.toEpochMilli()
    }

    @TypeConverter
    fun fromLong(time: Long): Instant {
        return Instant.ofEpochMilli(time)
    }
}