package com.swastik.hobby.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import com.swastik.hobby.data.converter.Converters
import com.swastik.hobby.data.dao.*
import com.swastik.hobby.data.entity.*

@Database(
    entities = [
        Project::class,
        ProjectStep::class,
        Material::class,
        ProjectMaterial::class,
        ProjectPhoto::class,
        ProjectTemplate::class,
        TemplateStep::class,
        TimeEntry::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class HobbyDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun materialDao(): MaterialDao
    abstract fun templateDao(): TemplateDao

    companion object {
        @Volatile
        private var INSTANCE: HobbyDatabase? = null

        fun getDatabase(context: Context): HobbyDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    HobbyDatabase::class.java,
                    "hobby_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}