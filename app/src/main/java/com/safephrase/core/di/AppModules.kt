package com.safephrase.core.di

import android.content.Context
import com.safephrase.BuildConfig
import com.safephrase.data.local.db.SafePhraseDatabase
import com.safephrase.data.local.db.dao.*
import com.safephrase.data.local.preferences.UserPreferencesDataStore
import com.safephrase.data.remote.api.SafePhraseApi
import com.safephrase.data.repository.*
import com.safephrase.domain.repository.*
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): SafePhraseDatabase =
        SafePhraseDatabase.getInstance(ctx)

    @Provides fun provideEmergencyDao(db: SafePhraseDatabase): EmergencyEventDao = db.emergencyEventDao()
    @Provides fun provideContactDao(db: SafePhraseDatabase): TrustedContactDao = db.trustedContactDao()
    @Provides fun provideSafePhraseDao(db: SafePhraseDatabase): SafePhraseDao = db.safePhraseDao()
}

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides @Singleton
    fun provideOkHttp(): OkHttpClient {
        val level = if (BuildConfig.ENABLE_LOGGING)
            HttpLoggingInterceptor.Level.BODY
        else
            HttpLoggingInterceptor.Level.NONE

        return OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().setLevel(level))
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides @Singleton
    fun provideRetrofit(okHttp: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(okHttp)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides @Singleton
    fun provideApi(retrofit: Retrofit): SafePhraseApi =
        retrofit.create(SafePhraseApi::class.java)
}

@Module
@InstallIn(SingletonComponent::class)
object PreferencesModule {
    @Provides @Singleton
    fun provideDataStore(@ApplicationContext ctx: Context) = UserPreferencesDataStore(ctx)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds @Singleton abstract fun bindEmergencyRepo(impl: EmergencyRepositoryImpl): EmergencyRepository
    @Binds @Singleton abstract fun bindConfigRepo(impl: ConfigRepositoryImpl): ConfigRepository
    @Binds @Singleton abstract fun bindPreferencesRepo(impl: PreferencesRepositoryImpl): PreferencesRepository
    @Binds @Singleton abstract fun bindMonitoringRepo(impl: MonitoringRepositoryImpl): MonitoringRepository
}
