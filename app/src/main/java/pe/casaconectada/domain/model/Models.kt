package pe.casaconectada.domain.model

data class FamilyMember(
    val id: String,
    val name: String,
    val points: Int = 0,
)

data class TaskItem(
    val id: String,
    val title: String,
    val responsibleId: String,
    val dueAt: Long,
    val repeat: String = "Única",
    val status: String = "PENDIENTE",
    val evidencePath: String? = null,
    val completedAt: Long? = null,
)

data class MoneyEntry(
    val id: String,
    val concept: String,
    val amountCents: Long,
    val dueAt: Long,
    val kind: String = "PAGO",
    val ownerId: String? = null,
    val periodicity: String = "Única",
    val isPaid: Boolean = false,
    val photoPath: String? = null,
)

data class SavingGoal(
    val id: String,
    val name: String,
    val targetCents: Long,
    val deadline: Long,
    val createdBy: String,
)

data class GoalMethod(
    val id: String,
    val goalId: String,
    val type: String,
    val holder: String,
    val phone: String = "",
    val account: String = "",
    val cci: String = "",
)

data class Contribution(
    val id: String,
    val goalId: String,
    val memberId: String,
    val amountCents: Long,
    val status: String = "PENDIENTE",
    val receiptPath: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
)

/** Solo los aportes CONFIRMADOS forman parte del saldo, nunca los pendientes. */
fun confirmedBalance(contributions: List<Contribution>, goalId: String): Long =
    contributions.filter { it.goalId == goalId && it.status == "CONFIRMADO" }
        .sumOf { it.amountCents }
