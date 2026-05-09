package com.musicplayer.di

import com.musicplayer.data.repository.ProfileRepository
import com.musicplayer.profile.ProfileManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dependency injection module for profile-related components.
 * Provides ProfileRepository and ProfileManager as singletons.
 */
@Module
@InstallIn(SingletonComponent::class)
object ProfileModule {

    @Provides
    @Singleton
    fun provideProfileManager(
        profileRepository: ProfileRepository,
        @ProfileDataStore profileDataStore: androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences>
    ): ProfileManager {
        return ProfileManager(profileRepository, profileDataStore)
    }
}
