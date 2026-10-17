package pe.casaconectada.presentation

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import pe.casaconectada.data.local.EvidenceStore
import pe.casaconectada.domain.model.*
import pe.casaconectada.domain.repository.FamilyRepository
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class FamilyViewModel @Inject constructor(
    private val repository: FamilyRepository,
    private val photos: EvidenceStore,
) : ViewModel() {
    val members = repository.members.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val tasks = repository.tasks.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val entries = repository.entries.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val goals = repository.goals.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val contributions = repository.contributions.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val methods = repository.methods.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    private val _feedback = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val feedback = _feedback.asSharedFlow()

    private fun perform(successMessage: String, action: suspend () -> Unit) {
        viewModelScope.launch {
            try { action(); _feedback.emit(successMessage) }
            catch (ex: Exception) { _feedback.emit("No se guardó: ${ex.localizedMessage ?: "error desconocido"}") }
        }
    }

    fun addMember(name: String) {
        if (name.isBlank()) return
        perform("Integrante agregado") { repository.addMember(name) }
    }
    fun saveMemberProfile(member: FamilyMember, name: String, alias: String,
        phone: String, email: String, notes: String, photo: Uri?) {
        if (name.isBlank()) return
        perform("Perfil actualizado") {
            val photoPath = photo?.let { photos.save(it) } ?: member.photoPath
            repository.updateMember(member.copy(
                name = name.trim(), alias = alias.trim(), phone = phone.trim(),
                email = email.trim(), notes = notes.trim(), photoPath = photoPath
            ))
        }
    }
    fun addTask(title: String, responsibleId: String, dueAt: Long, repeat: String) {
        if (title.isBlank() || responsibleId.isBlank()) return
        perform("Tarea creada") {
            repository.addTask(TaskItem(UUID.randomUUID().toString(), title.trim(), responsibleId, dueAt, repeat))
        }
    }
    fun completeTask(taskId: String, photo: Uri) {
        perform("Tarea completada con foto") {
            repository.completeTask(taskId, photos.save(photo))
        }
    }
    fun addEntry(concept: String, cents: Long, dueAt: Long, kind: String) {
        if (concept.isBlank() || cents <= 0) return
        perform("${if (kind == "PAGO") "Pago" else "Gasto"} creado") {
            repository.addEntry(MoneyEntry(UUID.randomUUID().toString(), concept.trim(), cents, dueAt, kind))
        }
    }
    fun markPaid(id: String, photo: Uri? = null) {
        perform("Pago registrado") { repository.payEntry(id, photo?.let { photos.save(it) }) }
    }
    fun addGoal(name: String, target: Long, date: Long, memberId: String) {
        if (name.isBlank() || target <= 0) return
        perform("Meta creada") {
            repository.addGoal(SavingGoal(UUID.randomUUID().toString(), name.trim(), target, date, memberId))
        }
    }
    fun addMethod(goalId: String, type: String, holder: String, phone: String, account: String, cci: String) {
        perform("Método guardado con cifrado local") {
            repository.addMethod(GoalMethod(UUID.randomUUID().toString(), goalId, type, holder.trim(), phone.trim(), account.trim(), cci.trim()))
        }
    }
    fun addContribution(goalId: String, memberId: String, amount: Long, photo: Uri? = null) {
        if (amount <= 0) return
        perform("Aporte pendiente de confirmación") {
            repository.addContribution(Contribution(UUID.randomUUID().toString(), goalId, memberId, amount,
                receiptPath = photo?.let { photos.save(it) }))
        }
    }
    fun confirmContribution(id: String) {
        perform("Aporte confirmado manualmente") { repository.confirmContribution(id) }
    }
}
