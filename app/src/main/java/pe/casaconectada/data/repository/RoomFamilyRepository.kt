package pe.casaconectada.data.repository

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import pe.casaconectada.data.local.*
import pe.casaconectada.domain.model.*
import pe.casaconectada.domain.repository.FamilyRepository
import pe.casaconectada.security.SensitiveValueCipher
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomFamilyRepository @Inject constructor(
    private val database: FamilyDatabase,
    private val dao: FamilyDao,
    private val cipher: SensitiveValueCipher,
) : FamilyRepository {
    override val members: Flow<List<FamilyMember>> = dao.observeMembers().map { list ->
        list.map { FamilyMember(it.id, it.name, it.points, it.alias, it.phone, it.email, it.notes, it.photoPath) }
    }
    override val tasks: Flow<List<TaskItem>> = dao.observeTasks().map { list ->
        list.map { TaskItem(it.id, it.title, it.responsibleId, it.dueAt, it.repeat, it.status, it.evidencePath, it.completedAt) }
    }
    override val entries: Flow<List<MoneyEntry>> = dao.observeEntries().map { list ->
        list.map { MoneyEntry(it.id, it.concept, it.amountCents, it.dueAt, it.kind, it.ownerId, it.periodicity, it.isPaid, it.photoPath) }
    }
    override val goals: Flow<List<SavingGoal>> = dao.observeGoals().map { list ->
        list.map { SavingGoal(it.id, it.name, it.targetCents, it.deadline, it.createdBy) }
    }
    override val contributions: Flow<List<Contribution>> = dao.observeContributions().map { list ->
        list.map { Contribution(it.id, it.goalId, it.memberId, it.amountCents, it.status, it.receiptPath, it.createdAt) }
    }
    override val methods: Flow<List<GoalMethod>> = dao.observeMethods().map { list ->
        list.map {
            GoalMethod(it.id, it.goalId, it.type, it.holder,
                cipher.decrypt(it.encryptedPhone),
                cipher.decrypt(it.encryptedAccount),
                cipher.decrypt(it.encryptedCci))
        }
    }

    override suspend fun addMember(name: String) {
        dao.addMember(MemberEntity(UUID.randomUUID().toString(), name.trim()))
    }
    override suspend fun updateMember(member: FamilyMember) {
        require(dao.updateMember(member.id, member.name, member.alias, member.phone,
            member.email, member.notes, member.photoPath) == 1) {
            "El integrante no existe"
        }
    }
    override suspend fun addTask(task: TaskItem) {
        dao.addTask(TaskEntity(task.id, task.title, task.responsibleId, task.dueAt, task.repeat, task.status, task.evidencePath, task.completedAt))
    }
    override suspend fun completeTask(id: String, evidencePath: String) {
        database.withTransaction {
            val owner = dao.taskOwner(id)
            if (dao.completeTask(id, evidencePath, System.currentTimeMillis()) > 0 && owner != null) {
                dao.givePoints(owner)
            }
        }
    }
    override suspend fun addEntry(entry: MoneyEntry) {
        dao.addEntry(EntryEntity(entry.id, entry.concept, entry.amountCents, entry.dueAt, entry.kind, entry.ownerId, entry.periodicity, entry.isPaid, entry.photoPath))
    }
    override suspend fun payEntry(id: String, evidencePath: String?) { dao.payEntry(id, evidencePath) }
    override suspend fun addGoal(goal: SavingGoal) {
        dao.addGoal(GoalEntity(goal.id, goal.name, goal.targetCents, goal.deadline, goal.createdBy))
    }
    override suspend fun addMethod(method: GoalMethod) {
        dao.addMethod(MethodEntity(method.id, method.goalId, method.type, method.holder,
            cipher.encrypt(method.phone), cipher.encrypt(method.account), cipher.encrypt(method.cci)))
    }
    override suspend fun addContribution(contribution: Contribution) {
        dao.addContribution(ContributionEntity(contribution.id, contribution.goalId, contribution.memberId,
            contribution.amountCents, contribution.status, contribution.receiptPath, contribution.createdAt))
    }
    override suspend fun confirmContribution(id: String) { dao.confirmContribution(id) }
}
