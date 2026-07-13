package com.smartnotes.app.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.smartnotes.app.data.ai.FakeAiRepositoryImpl
import com.smartnotes.app.data.local.AppDatabase
import com.smartnotes.app.data.local.NoteDao
import com.smartnotes.app.data.local.TagDao
import com.smartnotes.app.data.preferences.UserPreferencesRepositoryImpl
import com.smartnotes.app.data.repository.NoteRepositoryImpl
import com.smartnotes.app.domain.repository.AiRepository
import com.smartnotes.app.domain.repository.NoteRepository
import com.smartnotes.app.domain.repository.UserPreferencesRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "smartnotes_prefs")

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "smartnotes.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideNoteDao(database: AppDatabase): NoteDao = database.noteDao()

    @Provides
    fun provideTagDao(database: AppDatabase): TagDao = database.tagDao()

    @Provides
    @Singleton
    fun provideNoteRepository(
        noteDao: NoteDao,
        tagDao: TagDao,
        appDatabase: AppDatabase
    ): NoteRepository = NoteRepositoryImpl(noteDao, tagDao, appDatabase)

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        context.dataStore

    @Provides
    @Singleton
    fun provideUserPreferencesRepository(
        dataStore: DataStore<Preferences>
    ): UserPreferencesRepository = UserPreferencesRepositoryImpl(dataStore)

    @Provides
    @Singleton
    fun provideAiRepository(): AiRepository = FakeAiRepositoryImpl()
}
