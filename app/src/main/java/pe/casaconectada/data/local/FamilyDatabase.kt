package pe.casaconectada.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [MemberEntity::class, TaskEntity::class, EntryEntity::class,
        GoalEntity::class, MethodEntity::class, ContributionEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class FamilyDatabase : RoomDatabase() {
    abstract fun dao(): FamilyDao
}

/** Actualización aditiva: conserva integrantes, puntos, tareas, pagos y metas de la V1. */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE members ADD COLUMN alias TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE members ADD COLUMN phone TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE members ADD COLUMN email TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE members ADD COLUMN notes TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE members ADD COLUMN photoPath TEXT")
    }
}
