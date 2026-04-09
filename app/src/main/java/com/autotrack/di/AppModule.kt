package com.autotrack.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.autotrack.data.local.AutoTrackDatabase
import com.autotrack.data.local.dao.FuelEntryDao
import com.autotrack.data.local.dao.ServiceRecordDao
import com.autotrack.data.local.dao.VehicleDao
import com.autotrack.data.local.repository.NhtsaApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences>
        by preferencesDataStore(name = "autotrack_prefs")

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): AutoTrackDatabase =
        Room.databaseBuilder(ctx, AutoTrackDatabase::class.java, "autotrack_db")
            .build()

    @Provides @Singleton
    fun provideVehicleDao(db: AutoTrackDatabase): VehicleDao = db.vehicleDao()

    @Provides @Singleton
    fun provideRecordDao(db: AutoTrackDatabase): ServiceRecordDao = db.serviceRecordDao()

    @Provides @Singleton
    fun provideFuelDao(db: AutoTrackDatabase): FuelEntryDao = db.fuelEntryDao()

    @Provides @Singleton
    fun provideDataStore(@ApplicationContext ctx: Context): DataStore<Preferences> =
        ctx.dataStore

    @Provides @Singleton
    fun provideOkHttp(): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(
            HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
        )
        .build()

    @Provides @Singleton
    fun provideRetrofit(okHttp: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl("https://vpic.nhtsa.dot.gov/api/")
        .client(okHttp)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    @Provides @Singleton
    fun provideNhtsaApi(retrofit: Retrofit): NhtsaApiService =
        retrofit.create(NhtsaApiService::class.java)
}