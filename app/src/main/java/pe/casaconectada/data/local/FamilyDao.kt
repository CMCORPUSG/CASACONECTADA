package pe.casaconectada.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FamilyDao {
    @Query("SELECT * FROM members ORDER BY name")
    fun observeMembers(): Flow<List<MemberEntity>>

    @Query("SELECT * FROM tasks ORDER BY dueAt, title")
    fun observeTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM money_entries ORDER BY dueAt, concept")
    fun observeEntries(): Flow<List<EntryEntity>>

    @Query("SELECT * FROM saving_goals ORDER BY deadline")
    fun observeGoals(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goal_methods ORDER BY type")
    fun observeMethods(): Flow<List<MethodEntity>>

    @Query("SELECT * FROM contributions ORDER BY createdAt DESC")
    fun observeContributions(): Flow<List<ContributionEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun addMember(entity: MemberEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun addTask(entity: TaskEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun addEntry(entity: EntryEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun addGoal(entity: GoalEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun addMethod(entity: MethodEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun addContribution(entity: ContributionEntity)

    @Query("UPDATE tasks SET status = 'COMPLETADA', evidencePath = :evidence, completedAt = :timestamp WHERE id = :id AND status != 'COMPLETADA'")
    suspend fun completeTask(id: String, evidence: String, timestamp: Long): Int

    @Query("UPDATE members SET points = points + 10 WHERE id = :memberId")
    suspend fun givePoints(memberId: String)

    @Query("SELECT responsibleId FROM tasks WHERE id = :taskId")
    suspend fun taskOwner(taskId: String): String?

    @Query("UPDATE money_entries SET isPaid = 1, photoPath = :evidence WHERE id = :id AND isPaid = 0")
    suspend fun payEntry(id: String, evidence: String?): Int

    @Query("UPDATE contributions SET status = 'CONFIRMADO' WHERE id = :id AND status = 'PENDIENTE'")
    suspend fun confirmContribution(id: String): Int
}
