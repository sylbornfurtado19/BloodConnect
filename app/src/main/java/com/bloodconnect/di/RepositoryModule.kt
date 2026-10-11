package com.bloodconnect.di

import com.bloodconnect.data.repository.DonorRepository
import com.bloodconnect.data.repository.DonorRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindDonorRepository(
        donorRepositoryImpl: DonorRepositoryImpl
    ): DonorRepository
}
