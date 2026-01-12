package com.smartparking.mobile.di

import android.content.Context
import com.smartparking.mobile.data.local.TokenDataStore
import com.smartparking.mobile.data.websocket.WebSocketService
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
    fun provideTokenDataStore(
        @ApplicationContext context: Context
    ): TokenDataStore {
        return TokenDataStore(context)
    }

    @Provides
    @Singleton
    fun provideWebSocketService(
        tokenDataStore: TokenDataStore
    ): WebSocketService {
        return WebSocketService(tokenDataStore)
    }
}
