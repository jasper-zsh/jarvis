package pro.sihao.jarvis.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import pro.sihao.jarvis.data.database.JarvisDatabase
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideJarvisDatabase(@ApplicationContext context: Context): JarvisDatabase {
        return JarvisDatabase.getDatabase(context)
    }

    @Provides
    fun provideMessageDao(database: JarvisDatabase) = database.messageDao()
}