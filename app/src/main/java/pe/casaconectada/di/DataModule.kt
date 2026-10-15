package pe.casaconectada.di

import android.content.Context
import androidx.room.Room
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import pe.casaconectada.data.local.FamilyDao
import pe.casaconectada.data.local.FamilyDatabase
import pe.casaconectada.data.repository.RoomFamilyRepository
import pe.casaconectada.domain.repository.FamilyRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds abstract fun bindFamilyRepository(impl: RoomFamilyRepository): FamilyRepository
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides @Singleton
    fun provideDatabase(@ApplicationContext context: Context): FamilyDatabase =
        Room.databaseBuilder(context, FamilyDatabase::class.java, "casa_conectada.db")
            .build()

    @Provides fun provideDao(database: FamilyDatabase): FamilyDao = database.dao()
}
