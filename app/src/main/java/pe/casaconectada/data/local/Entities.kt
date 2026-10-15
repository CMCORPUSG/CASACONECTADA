package pe.casaconectada.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "members")
data class MemberEntity(@PrimaryKey val id: String, val name: String, val points: Int = 0)

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: String,
    val title: String,
    val responsibleId: String,
    val dueAt: Long,
    val repeat: String,
    val status: String,
    val evidencePath: String?,
    val completedAt: Long?,
)

@Entity(tableName = "money_entries")
data class EntryEntity(
    @PrimaryKey val id: String,
    val concept: String,
    val amountCents: Long,
    val dueAt: Long,
    val kind: String,
    val ownerId: String?,
    val periodicity: String,
    val isPaid: Boolean,
    val photoPath: String?,
)

@Entity(tableName = "saving_goals")
data class GoalEntity(
    @PrimaryKey val id: String,
    val name: String,
    val targetCents: Long,
    val deadline: Long,
    val createdBy: String,
)

/** Los campos financieros guardados llevan cifrado AES-GCM por campo. */
@Entity(tableName = "goal_methods")
data class MethodEntity(
    @PrimaryKey val id: String,
    val goalId: String,
    val type: String,
    val holder: String,
    val encryptedPhone: String,
    val encryptedAccount: String,
    val encryptedCci: String,
)

@Entity(tableName = "contributions")
data class ContributionEntity(
    @PrimaryKey val id: String,
    val goalId: String,
    val memberId: String,
    val amountCents: Long,
    val status: String,
    val receiptPath: String?,
    val createdAt: Long,
)
