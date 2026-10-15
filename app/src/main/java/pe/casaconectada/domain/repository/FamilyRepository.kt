package pe.casaconectada.domain.repository

import kotlinx.coroutines.flow.Flow
import pe.casaconectada.domain.model.*

/** Contrato de dominio. La presentación no depende de Room. */
interface FamilyRepository {
    val members: Flow<List<FamilyMember>>
    val tasks: Flow<List<TaskItem>>
    val entries: Flow<List<MoneyEntry>>
    val goals: Flow<List<SavingGoal>>
    val contributions: Flow<List<Contribution>>
    val methods: Flow<List<GoalMethod>>

    suspend fun addMember(name: String)
    suspend fun addTask(task: TaskItem)
    suspend fun completeTask(id: String, evidencePath: String)
    suspend fun addEntry(entry: MoneyEntry)
    suspend fun payEntry(id: String, evidencePath: String?)
    suspend fun addGoal(goal: SavingGoal)
    suspend fun addMethod(method: GoalMethod)
    suspend fun addContribution(contribution: Contribution)
    suspend fun confirmContribution(id: String)
}
