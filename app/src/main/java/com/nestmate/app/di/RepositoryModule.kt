package com.nestmate.app.di

import android.content.Context
import com.nestmate.app.data.local.PreferencesManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    
    @Provides
    @Singleton
    fun providePreferencesManager(@ApplicationContext context: Context): PreferencesManager {
        return PreferencesManager(context)
    }

    @Provides
    @Singleton
    fun provideRoomDatabase(@ApplicationContext context: Context): com.nestmate.app.data.local.NestMateDatabase {
        return androidx.room.Room.databaseBuilder(
            context,
            com.nestmate.app.data.local.NestMateDatabase::class.java,
            "nestmate_db"
        ).build()
    }

    @Provides
    @Singleton
    fun provideListingDao(db: com.nestmate.app.data.local.NestMateDatabase): com.nestmate.app.data.local.ListingDao {
        return db.listingDao()
    }

    @Provides
    @Singleton
    fun provideRestaurantRepository(
        firestore: com.google.firebase.firestore.FirebaseFirestore,
    ): com.nestmate.app.data.repository.RestaurantRepository {
        return com.nestmate.app.data.repository.RestaurantRepository(firestore)
    }

    @Provides
    @Singleton
    fun provideConnectionRepository(
        firestore: com.google.firebase.firestore.FirebaseFirestore,
    ): com.nestmate.app.data.repository.ConnectionRepository {
        return com.nestmate.app.data.repository.ConnectionRepository(firestore)
    }

    @Provides
    @Singleton
    fun provideSpendingRepository(
        firestore: com.google.firebase.firestore.FirebaseFirestore,
        @ApplicationContext context: android.content.Context,
    ): com.nestmate.app.data.repository.SpendingRepository {
        return com.nestmate.app.data.repository.SpendingRepository(firestore, context)
    }

    @Provides
    @Singleton
    fun provideLostFoundRepository(
        firestore: com.google.firebase.firestore.FirebaseFirestore,
        auth: com.google.firebase.auth.FirebaseAuth
    ): com.nestmate.app.data.repository.LostFoundRepository {
        return com.nestmate.app.data.repository.LostFoundRepository(firestore, auth)
    }
}
