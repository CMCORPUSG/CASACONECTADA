package pe.casaconectada.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [MemberEntity::class, TaskEntity::class, EntryEntity::class,
        GoalEntity::class, MethodEntity::class, ContributionEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class FamilyDatabase : RoomDatabase() {
    abstract fun dao(): FamilyDao
}
