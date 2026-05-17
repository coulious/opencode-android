package com.example.opencode.di

import android.content.Context
import com.example.opencode.data.local.ConnectionConfigStore
import com.example.opencode.data.remote.HttpClientFactory
import com.example.opencode.data.repository.ConnectionRepository
import com.example.opencode.data.repository.SessionRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideConnectionConfigStore(
        @ApplicationContext context: Context,
    ): ConnectionConfigStore {
        return ConnectionConfigStore(context)
    }

    @Provides
    @Singleton
    fun provideHttpClientFactory(): HttpClientFactory {
        return HttpClientFactory()
    }

    @Provides
    @Singleton
    fun provideConnectionRepository(
        configStore: ConnectionConfigStore,
        httpClientFactory: HttpClientFactory,
    ): ConnectionRepository {
        return ConnectionRepository(configStore, httpClientFactory)
    }

    @Provides
    @Singleton
    fun provideSessionRepository(): SessionRepository {
        return SessionRepository()
    }
}
