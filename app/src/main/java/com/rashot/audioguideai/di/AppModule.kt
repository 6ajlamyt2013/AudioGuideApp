package com.rashot.audioguideai.di

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun providePOIDatabase(@ApplicationContext context: Context): POIDatabase {
        return POIDatabase.getDatabase(context)
    }

    @Provides
    @Singleton
    fun providePOIDao(database: POIDatabase): POIDao {
        return database.poiDao()
    }

    @Provides
    @Singleton
    fun provideRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://maps.googleapis.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun providePlacesApiService(retrofit: Retrofit): PlacesApiService {
        return retrofit.create(PlacesApiService::class.java)
    }
}