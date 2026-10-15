package pe.casaconectada.presentation

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect
import pe.casaconectada.R
import pe.casaconectada.design.*
import pe.casaconectada.domain.model.*
import java.io.File
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.Locale

private enum class Section(val label: String) { INICIO("Inicio"), TAREAS("Tareas"), CALENDARIO("Calendario"), FINANZAS("Finanzas"), METAS("Metas"), FAMILIA("Familia") }

private val rounded = RoundedCornerShape(20.dp)
private val esPe = Locale("es", "PE")
private fun amount(value: Long) = String.format(esPe, "S/ %,.2f", value / 100.0)
private fun cents(text: String): Long? = try {
    val x = BigDecimal(text.trim().replace(',', '.')).multiply(BigDecimal(100))
    val v = x.setScale(0, RoundingMode.UNNECESSARY).longValueExact()
    v.takeIf { it > 0 }
} catch (_: Exception) { null }
private fun whenText(epoch: Long) = Instant.ofEpochMilli(epoch).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("dd MMM · HH:mm", esPe))
private fun dayText(epoch: Long) = Instant.ofEpochMilli(epoch).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("dd MMM yyyy", esPe))
private fun dayKey(epoch: Long): LocalDate = Instant.ofEpochMilli(epoch).atZone(ZoneId.systemDefault()).toLocalDate()

@Composable
fun CasaApp(vm: FamilyViewModel) {
    val members by vm.members.collectAsStateWithLifecycle()
    val tasks by vm.tasks.collectAsStateWithLifecycle()
    val entries by vm.entries.collectAsStateWithLifecycle()
    val goals by vm.goals.collectAsStateWithLifecycle()
    val contributions by vm.contributions.collectAsStateWithLifecycle()
    val methods by vm.methods.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var section by rememberSaveable { mutableStateOf(Section.INICIO) }
    var dialog by rememberSaveable { mutableStateOf("") }
    var selectedGoalId by rememberSaveable { mutableStateOf("") }
    var taskToPhotograph by remember { mutableStateOf("") }
    var paymentToPhotograph by remember { mutableStateOf("") }
    val taskPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { photo: Uri? ->
        if (photo != null && taskToPhotograph.isNotBlank()) vm.completeTask(taskToPhotograph, photo)
        taskToPhotograph = ""
    }
    val receiptPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { photo: Uri? ->
        if (photo != null && paymentToPhotograph.isNotBlank()) vm.markPaid(paymentToPhotograph, photo)
        paymentToPhotograph = ""
    }

    LaunchedEffect(vm) { vm.feedback.collect { snackbarHostState.showSnackbar(it) } }

    Scaffold(
        containerColor = Ivory,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { CasaTopBar(section.label) },
        bottomBar = {
            NavigationBar(containerColor = Color.White, tonalElevation = 1.dp) {
                val nav = listOf(
                    Section.INICIO to Icons.Outlined.Home,
                    Section.TAREAS to Icons.Outlined.TaskAlt,
                    Section.CALENDARIO to Icons.Outlined.CalendarMonth,
                    Section.FINANZAS to Icons.Outlined.AccountBalanceWallet,
                    Section.METAS to Icons.Outlined.Flag,
                    Section.FAMILIA to Icons.Outlined.Groups,
                )
                nav.forEach { (s, icon) ->
                    NavigationBarItem(selected = section == s, onClick = { section = s },
                        icon = { Icon(icon, contentDescription = s.label) },
                        label = { Text(s.label, fontSize = 9.sp, maxLines = 1) },
                        alwaysShowLabel = true,
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = Jade, indicatorColor = Mint))
                }
            }
        },
    ) { inner ->
        Box(Modifier.padding(inner).fillMaxSize()) {
            when (section) {
                Section.INICIO -> InicioScreen(tasks, entries, goals, contributions, members,
                    onTask = { section = Section.TAREAS },
                    onNewTask = { dialog = "task" },
                    onNewPayment = { dialog = "entry" },
                    onGoals = { section = Section.METAS },
                    onFamily = { section = Section.FAMILIA })
                Section.TAREAS -> TareasScreen(tasks, members,
                    onAdd = { dialog = "task" },
                    onPhoto = { taskToPhotograph = it; taskPicker.launch("image/*") })
                Section.CALENDARIO -> CalendarioScreen(tasks, entries, members)
                Section.FINANZAS -> FinanzasScreen(entries,
                    onAdd = { dialog = "entry" },
                    onPay = { paymentToPhotograph = it; dialog = "pay" })
                Section.METAS -> MetasScreen(goals, contributions,
                    onAdd = { dialog = "goal" },
                    onOpen = { selectedGoalId = it; dialog = "goalDetail" })
                Section.FAMILIA -> FamiliaScreen(members, tasks, onAdd = { dialog = "member" })
            }
        }
    }

    when (dialog) {
        "member" -> MemberDialog(onDismiss = { dialog = "" }, onSave = { vm.addMember(it); dialog = "" })
        "task" -> TaskDialog(members, onDismiss = { dialog = "" }, onSave = { name, who, date, repeat ->
            vm.addTask(name, who, date, repeat); dialog = "" })
        "entry" -> EntryDialog(onDismiss = { dialog = "" }, onSave = { name, cents, date, kind ->
            vm.addEntry(name, cents, date, kind); dialog = "" })
        "pay" -> AlertDialog(onDismissRequest = { dialog = "" },
            title = { Text("Registrar pago") },
            text = { Text("Puedes adjuntar un comprobante o guardar el pago sin foto.") },
            confirmButton = { TextButton(onClick = {
                dialog = ""; receiptPicker.launch("image/*")
            }) { Text("Agregar foto") } },
            dismissButton = { TextButton(onClick = {
                vm.markPaid(paymentToPhotograph); paymentToPhotograph = ""; dialog = ""
            }) { Text("Sin foto") } })
        "goal" -> GoalDialog(members, onDismiss = { dialog = "" }, onSave = { name, target, date, who ->
            vm.addGoal(name, target, date, who); dialog = "" })
        "goalDetail" -> goals.find { it.id == selectedGoalId }?.let { goal ->
            GoalDetailDialog(goal, methods.filter { it.goalId == goal.id }, contributions.filter { it.goalId == goal.id }, members,
                onDismiss = { dialog = "" },
                onAddMethod = { dialog = "method" },
                onContribute = { dialog = "contribute" },
                onConfirm = { vm.confirmContribution(it) },
                onMessage = { scope.launch { snackbarHostState.showSnackbar(it) } })
        }
        "method" -> MethodDialog(onDismiss = { dialog = "goalDetail" }, onSave = { type, holder, phone, acc, cci ->
            vm.addMethod(selectedGoalId, type, holder, phone, acc, cci); dialog = "goalDetail" })
        "contribute" -> ContributionDialog(members, onDismiss = { dialog = "goalDetail" }, onSave = { who, v, photo ->
            vm.addContribution(selectedGoalId, who, v, photo); dialog = "goalDetail" })
    }
}

@Composable
private fun CasaTopBar(sectionName: String) {
    Row(Modifier.fillMaxWidth().background(Ivory).padding(horizontal = 18.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            Image(painterResource(R.mipmap.ic_launcher), contentDescription = "Logo Casa Conectada",
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)))
            Column {
                Text("Casa Conectada", fontWeight = FontWeight.Bold, color = Ink, fontSize = 15.sp)
                Text(sectionName, color = Muted, fontSize = 11.sp)
            }
        }
        Surface(color = Mint, shape = CircleShape) {
            Text("YO", Modifier.padding(10.dp), color = Jade, fontWeight = FontWeight.Bold, fontSize = 11.sp)
        }
    }
    HorizontalDivider(color = Line)
}

@Composable
private fun Intro(kicker: String, title: String, description: String, action: String? = null, onAction: () -> Unit = {}) {
    Text("✧  ${kicker.uppercase(esPe)}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Jade)
    Spacer(Modifier.height(8.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, color = Ink, lineHeight = 32.sp)
            Text(description, color = Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 5.dp))
        }
        if (action != null) Button(onClick = onAction, shape = RoundedCornerShape(13.dp)) {
            Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(17.dp))
            Text(action, fontSize = 12.sp)
        }
    }
    Spacer(Modifier.height(23.dp))
}

@Composable
private fun InfoPanel(content: @Composable ColumnScope.() -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = rounded,
        border = BorderStroke(1.dp, Line), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(17.dp), content = content)
    }
}

@Composable
private fun EmptyHint(message: String) {
    Text(message, fontSize = 12.sp, color = Muted, modifier = Modifier.padding(vertical = 15.dp))
}

@Composable
private fun TwoLine(label: String, detail: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, fontWeight = FontWeight.Bold, color = Ink, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(detail, color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun GreenStat(label: String, value: String, bg: Color, modifier: Modifier = Modifier) {
    Column(modifier.clip(RoundedCornerShape(18.dp)).background(bg).padding(15.dp)) {
        Text(label, fontSize = 11.sp, color = Muted)
        Spacer(Modifier.height(8.dp))
        Text(value, color = Ink, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
    }
}

@Composable
private fun InicioScreen(tasks: List<TaskItem>, entries: List<MoneyEntry>, goals: List<SavingGoal>, contributions: List<Contribution>, members: List<FamilyMember>,
    onTask: () -> Unit, onNewTask: () -> Unit, onNewPayment: () -> Unit, onGoals: () -> Unit, onFamily: () -> Unit) {
    val pending = tasks.count { it.status != "COMPLETADA" }
    val completed = tasks.count { it.status == "COMPLETADA" }
    val progress = if (tasks.isEmpty()) 0f else completed.toFloat() / tasks.size
    LazyColumn(contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Intro("Hoy en casa", "Hola, familia 👋", "Aquí está lo importante de hoy.")
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(23.dp))
                .background(Brush.horizontalGradient(listOf(Color(0xFF0B8578), Color(0xFF115B5B)))).padding(25.dp)) {
                Column {
                    Text("HOY EN CASA", color = Mint, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(9.dp))
                    Text("$pending tareas\npendientes", color = Color.White, fontSize = 25.sp,
                        fontWeight = FontWeight.ExtraBold, lineHeight = 28.sp)
                    Text("¡Vamos en equipo!", color = Mint, fontSize = 12.sp, modifier = Modifier.padding(vertical = 7.dp))
                    LinearProgressIndicator(progress = { progress }, color = Color(0xFFC1F5DA),
                        trackColor = Color.White.copy(alpha = .3f), modifier = Modifier.fillMaxWidth().padding(vertical = 11.dp))
                    Button(onClick = onTask, colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Jade)) {
                        Text("Ver mis tareas")
                    }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onNewTask, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Outlined.Add, null, Modifier.size(17.dp)); Text("Nueva tarea", fontSize = 11.sp)
                }
                OutlinedButton(onClick = onNewPayment, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Outlined.ReceiptLong, null, Modifier.size(16.dp)); Text("Nuevo pago", fontSize = 11.sp)
                }
            }
            if (members.isEmpty()) TextButton(onClick = onFamily) { Text("Primero, agrega integrantes de la casa") }
        }
        item {
            InfoPanel {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Tareas", fontWeight = FontWeight.Bold)
                    TextButton(onClick = onTask) { Text("Ver todas") }
                }
                if (tasks.isEmpty()) EmptyHint("Todavía no hay tareas. Crea la primera.")
                tasks.take(3).forEach { task ->
                    HorizontalDivider(color = Line)
                    TwoLine(task.title, "${members.firstOrNull { it.id == task.responsibleId }?.name ?: "Sin responsable"} · ${whenText(task.dueAt)}",
                        Modifier.padding(vertical = 10.dp))
                }
            }
        }
        item {
            InfoPanel {
                Text("Próximos pagos", fontWeight = FontWeight.Bold)
                if (entries.none { it.kind == "PAGO" && !it.isPaid }) EmptyHint("No hay pagos pendientes.")
                entries.filter { it.kind == "PAGO" && !it.isPaid }.take(3).forEach {
                    Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        TwoLine(it.concept, dayText(it.dueAt))
                        Text(amount(it.amountCents), fontWeight = FontWeight.Bold, color = Ink)
                    }
                }
            }
        }
        item {
            InfoPanel {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Nuestras metas", fontWeight = FontWeight.Bold)
                    TextButton(onClick = onGoals) { Text("Ver metas") }
                }
                if (goals.isEmpty()) EmptyHint("Crea una meta familiar para empezar a ahorrar.")
                goals.take(2).forEach { goal ->
                    val balance = confirmedBalance(contributions, goal.id)
                    Text("${goal.name} · ${amount(balance)} de ${amount(goal.targetCents)}", fontSize = 12.sp, color = Ink)
                    LinearProgressIndicator(progress = { (balance.toFloat()/goal.targetCents.coerceAtLeast(1)).coerceIn(0f,1f) },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp), color = Jade, trackColor = Mint)
                }
            }
        }
        item { Text("Datos guardados en este celular. La sincronización familiar aún no está habilitada.", color = Muted, fontSize = 10.sp) }
    }
}

@Composable
private fun TareasScreen(tasks: List<TaskItem>, members: List<FamilyMember>, onAdd: () -> Unit, onPhoto: (String) -> Unit) {
    var filter by remember { mutableStateOf("Todas") }
    val visible = when (filter) {
        "Pendientes" -> tasks.filter { it.status != "COMPLETADA" }
        "Hechas" -> tasks.filter { it.status == "COMPLETADA" }
        else -> tasks
    }
    LazyColumn(contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
        item {
            Intro("Mis responsabilidades", "Tareas", "Cocinar, limpiar, pasear y más.", "Crear", onAdd)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("Todas", "Pendientes", "Hechas").forEach {
                    FilterChip(selected = filter == it, onClick = { filter = it }, label = { Text(it, fontSize = 11.sp) })
                }
            }
        }
        if (visible.isEmpty()) item { InfoPanel { EmptyHint("Sin tareas en esta sección.") } }
        items(visible, key = { it.id }) { task ->
            InfoPanel {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(color = Mint, shape = RoundedCornerShape(12.dp)) {
                        Icon(Icons.Outlined.HomeWork, null, Modifier.padding(10.dp), tint = Jade)
                    }
                    TwoLine(task.title,
                        "${members.find { it.id == task.responsibleId }?.name ?: "Sin responsable"} · ${whenText(task.dueAt)} · ${task.repeat}",
                        Modifier.weight(1f))
                    if (task.status == "COMPLETADA") {
                        Text("✓ Hecha", color = Jade, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    } else {
                        FilledTonalButton(onClick = { onPhoto(task.id) }, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)) {
                            Icon(Icons.Outlined.PhotoCamera, null, Modifier.size(16.dp))
                            Text("Hacer", fontSize = 11.sp)
                        }
                    }
                }
                if (task.evidencePath != null) {
                    Spacer(Modifier.height(8.dp))
                    AsyncImage(model = File(task.evidencePath), contentDescription = "Evidencia de ${task.title}",
                        modifier = Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(12.dp)))
                }
            }
        }
    }
}

@Composable
private fun CalendarioScreen(tasks: List<TaskItem>, entries: List<MoneyEntry>, members: List<FamilyMember>) {
    val events = (tasks.map { Triple(it.dueAt, it.title, members.find { m -> m.id == it.responsibleId }?.name ?: "Tarea") } +
        entries.filter { it.kind == "PAGO" }.map { Triple(it.dueAt, it.concept, "Pago ${amount(it.amountCents)}") })
        .sortedBy { it.first }
    LazyColumn(contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
        item { Intro("Organiza tu semana", "Calendario", "Actividades y pagos, todo en un lugar.") }
        if (events.isEmpty()) item { InfoPanel { EmptyHint("Cuando registres tareas y pagos aparecerán aquí.") } }
        events.groupBy { dayKey(it.first) }.forEach { (day, group) ->
            item(key = day.toString()) {
                InfoPanel {
                    Text(day.format(DateTimeFormatter.ofPattern("EEEE dd 'de' MMMM", esPe)).replaceFirstChar { it.uppercase() },
                        color = Jade, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    group.forEach { event ->
                        HorizontalDivider(Modifier.padding(top = 12.dp), color = Line)
                        TwoLine(event.second, "${whenText(event.first)} · ${event.third}", Modifier.padding(top = 10.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun FinanzasScreen(entries: List<MoneyEntry>, onAdd: () -> Unit, onPay: (String) -> Unit) {
    var tab by remember { mutableStateOf("PAGO") }
    val visible = entries.filter { it.kind == tab }
    LazyColumn(contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Intro("Dinero del hogar", "Pagos y gastos", "Todo claro, sin olvidos.", "Añadir", onAdd)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GreenStat("Por pagar", amount(entries.filter { it.kind == "PAGO" && !it.isPaid }.sumOf { it.amountCents }), Mint, Modifier.weight(1f))
                GreenStat("Gastos", amount(entries.filter { it.kind == "GASTO" }.sumOf { it.amountCents }), Peach, Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = tab == "PAGO", onClick = { tab = "PAGO" }, label = { Text("Pagos") })
                FilterChip(selected = tab == "GASTO", onClick = { tab = "GASTO" }, label = { Text("Gastos") })
            }
        }
        if (visible.isEmpty()) item { InfoPanel { EmptyHint("No tienes registros en esta sección.") } }
        items(visible, key = { it.id }) { entry ->
            InfoPanel {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = Mint, shape = RoundedCornerShape(11.dp)) {
                        Icon(Icons.Outlined.ReceiptLong, null, Modifier.padding(9.dp), tint = Jade)
                    }
                    Spacer(Modifier.width(10.dp))
                    TwoLine(entry.concept, "${dayText(entry.dueAt)}${if (entry.isPaid) " · Pagado" else ""}", Modifier.weight(1f))
                    Column(horizontalAlignment = Alignment.End) {
                        Text(amount(entry.amountCents), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        if (entry.kind == "PAGO" && !entry.isPaid) {
                            TextButton(onClick = { onPay(entry.id) }) { Text("Pagar", fontSize = 11.sp) }
                        }
                    }
                }
                if (entry.photoPath != null) {
                    AsyncImage(model = File(entry.photoPath), contentDescription = "Comprobante de pago",
                        modifier = Modifier.fillMaxWidth().height(140.dp).clip(RoundedCornerShape(11.dp)))
                }
            }
        }
    }
}

@Composable
private fun MetasScreen(goals: List<SavingGoal>, contributions: List<Contribution>, onAdd: () -> Unit, onOpen: (String) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
        item { Intro("Lo que queremos lograr", "Nuestros ahorros", "Metas de mes o de año.", "Crear", onAdd) }
        if (goals.isEmpty()) item { InfoPanel { EmptyHint("Aún no hay metas. Crea una para empezar.") } }
        items(goals, key = { it.id }) { goal ->
            val confirmed = confirmedBalance(contributions, goal.id)
            val percent = (confirmed.toDouble() / goal.targetCents.coerceAtLeast(1) * 100).toInt()
            Card(onClick = { onOpen(goal.id) }, shape = rounded,
                colors = CardDefaults.cardColors(containerColor = if (goals.indexOf(goal) == 0) Color(0xFFF1F9F3) else Color.White),
                border = BorderStroke(1.dp, Line), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(19.dp)) {
                    Text("✧  META ${if (goals.indexOf(goal) == 0) "FAMILIAR" else "DE AHORRO"}", color = Jade, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(10.dp))
                    Text(goal.name, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Ink)
                    Spacer(Modifier.height(5.dp))
                    Text("${amount(confirmed)} de ${amount(goal.targetCents)}", fontSize = 15.sp, color = Ink)
                    LinearProgressIndicator(progress = { (confirmed.toFloat() / goal.targetCents.coerceAtLeast(1)).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().padding(top = 15.dp), color = Jade, trackColor = Mint)
                    Row(Modifier.fillMaxWidth().padding(top = 9.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("$percent% ahorrado", color = Jade, fontSize = 11.sp)
                        Text(dayText(goal.deadline), color = Muted, fontSize = 11.sp)
                    }
                    TextButton(onClick = { onOpen(goal.id) }) { Text("Aportar / Ver detalles") }
                }
            }
        }
    }
}

@Composable
private fun FamiliaScreen(members: List<FamilyMember>, tasks: List<TaskItem>, onAdd: () -> Unit) {
    LazyColumn(contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Intro("Nuestro equipo", "Mi familia", "Cada persona cuenta.", "Agregar", onAdd) }
        if (members.isEmpty()) item { InfoPanel { EmptyHint("Agrega a las personas que viven en casa.") } }
        items(members, key = { it.id }) { member ->
            InfoPanel {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = CircleShape, color = Mint) {
                        Box(Modifier.size(50.dp), contentAlignment = Alignment.Center) {
                            Text(member.name.take(2).uppercase(esPe), color = Jade, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    TwoLine(member.name, "${tasks.count { it.responsibleId == member.id && it.status == "COMPLETADA" }} tareas realizadas",
                        Modifier.weight(1f))
                    Text("★ ${member.points}", color = Jade, fontWeight = FontWeight.Bold)
                }
            }
        }
        item { Text("Estos son perfiles locales, no cuentas verificadas ni invitaciones remotas.", fontSize = 11.sp, color = Muted) }
    }
}

@Composable
private fun CasaDialog(title: String, onDismiss: () -> Unit, enabled: Boolean,
    onSave: () -> Unit, buttonLabel: String = "Guardar", content: @Composable ColumnScope.() -> Unit) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(22.dp), color = Color.White, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.heightIn(max = 610.dp).verticalScroll(rememberScrollState()).padding(19.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(title, fontSize = 21.sp, fontWeight = FontWeight.Bold, color = Ink)
                content()
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancelar") }
                    Spacer(Modifier.width(9.dp))
                    Button(onClick = onSave, enabled = enabled) { Text(buttonLabel) }
                }
            }
        }
    }
}

@Composable
private fun Selection(label: String, values: List<Pair<String, String>>, selected: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(label, fontSize = 11.sp, color = Muted)
        Box {
            OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(13.dp)) {
                Text(values.firstOrNull { it.first == selected }?.second ?: "Seleccionar",
                    modifier = Modifier.weight(1f), maxLines = 1, color = Ink)
                Icon(Icons.Outlined.ArrowDropDown, null)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                values.forEach { (id, name) ->
                    DropdownMenuItem(text = { Text(name) }, onClick = { onSelect(id); expanded = false })
                }
            }
        }
    }
}

@Composable
private fun PickDateTime(label: String, at: Long, onChange: (Long) -> Unit) {
    val context = LocalContext.current
    val current = remember(at) { java.util.Calendar.getInstance().apply { timeInMillis = at } }
    Column {
        Text(label, fontSize = 11.sp, color = Muted)
        OutlinedButton(onClick = {
            DatePickerDialog(context, { _, year, month, day ->
                TimePickerDialog(context, { _, hour, minute ->
                    val selected = LocalDateTime.of(year, month + 1, day, hour, minute)
                    onChange(selected.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
                }, current.get(java.util.Calendar.HOUR_OF_DAY), current.get(java.util.Calendar.MINUTE), true).show()
            }, current.get(java.util.Calendar.YEAR), current.get(java.util.Calendar.MONTH),
                current.get(java.util.Calendar.DAY_OF_MONTH)).show()
        }, shape = RoundedCornerShape(13.dp)) {
            Icon(Icons.Outlined.Event, null, Modifier.size(17.dp))
            Spacer(Modifier.width(6.dp))
            Text(whenText(at))
        }
    }
}

@Composable
private fun MoneyInput(value: String, onChange: (String) -> Unit) {
    OutlinedTextField(value = value, onValueChange = onChange, label = { Text("Monto en soles (S/)") },
        singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
}

@Composable
private fun MemberDialog(onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    CasaDialog("Nuevo integrante", onDismiss, name.trim().length >= 2, { onSave(name.trim()) }) {
        OutlinedTextField(name, { name = it }, label = { Text("Nombre") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Text("Se crea un perfil en este dispositivo. Las invitaciones por QR requieren el futuro servidor familiar.", fontSize = 11.sp, color = Muted)
    }
}

@Composable
private fun TaskDialog(members: List<FamilyMember>, onDismiss: () -> Unit, onSave: (String, String, Long, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var memberId by remember(members) { mutableStateOf(members.firstOrNull()?.id.orEmpty()) }
    var due by remember { mutableLongStateOf(System.currentTimeMillis() + 3600_000) }
    var repeat by remember { mutableStateOf("Única") }
    CasaDialog("Nueva tarea", onDismiss, name.isNotBlank() && memberId.isNotBlank(),
        { onSave(name, memberId, due, repeat) }) {
        OutlinedTextField(name, { name = it }, label = { Text("¿Qué hay que hacer?") },
            singleLine = true, modifier = Modifier.fillMaxWidth())
        if (members.isEmpty()) Text("Primero crea un integrante en Familia.", color = Color(0xFFAD6233), fontSize = 12.sp)
        Selection("Responsable", members.map { it.id to it.name }, memberId, { memberId = it })
        PickDateTime("Fecha y hora", due, { due = it })
        Selection("Frecuencia", listOf("Única", "Diaria", "Semanal", "Mensual", "Rotativa").map { it to it }, repeat, { repeat = it })
        Text("La frecuencia queda registrada. La programación automática de repeticiones y turnos se implementará después.", fontSize = 11.sp, color = Muted)
    }
}

@Composable
private fun EntryDialog(onDismiss: () -> Unit, onSave: (String, Long, Long, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var due by remember { mutableLongStateOf(System.currentTimeMillis() + 3600_000) }
    var type by remember { mutableStateOf("PAGO") }
    CasaDialog("Nuevo pago o gasto", onDismiss, name.isNotBlank() && cents(price) != null,
        { cents(price)?.let { onSave(name, it, due, type) } }) {
        Selection("Tipo", listOf("PAGO" to "Pago pendiente", "GASTO" to "Gasto realizado"), type, { type = it })
        OutlinedTextField(name, { name = it }, label = { Text("Concepto (luz, agua, compras...)") },
            singleLine = true, modifier = Modifier.fillMaxWidth())
        MoneyInput(price, { price = it })
        PickDateTime(if (type == "PAGO") "Fecha de vencimiento" else "Fecha de gasto", due, { due = it })
    }
}

@Composable
private fun GoalDialog(members: List<FamilyMember>, onDismiss: () -> Unit, onSave: (String, Long, Long, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var due by remember { mutableLongStateOf(System.currentTimeMillis() + 30L * 24 * 3600_000) }
    var owner by remember(members) { mutableStateOf(members.firstOrNull()?.id.orEmpty()) }
    CasaDialog("Nueva meta familiar", onDismiss, name.isNotBlank() && cents(price) != null && owner.isNotBlank(),
        { cents(price)?.let { onSave(name, it, due, owner) } }) {
        OutlinedTextField(name, { name = it }, label = { Text("Nombre de la meta") },
            singleLine = true, modifier = Modifier.fillMaxWidth())
        MoneyInput(price, { price = it })
        PickDateTime("Fecha objetivo", due, { due = it })
        Selection("Creado por", members.map { it.id to it.name }, owner, { owner = it })
        if (members.isEmpty()) Text("Primero agrega un integrante.", color = Muted)
    }
}

@Composable
private fun GoalDetailDialog(goal: SavingGoal, methods: List<GoalMethod>, contributions: List<Contribution>, members: List<FamilyMember>,
    onDismiss: () -> Unit, onAddMethod: () -> Unit, onContribute: () -> Unit,
    onConfirm: (String) -> Unit, onMessage: (String) -> Unit) {
    val clipboard = LocalClipboardManager.current
    val confirmed = confirmedBalance(contributions, goal.id)
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(22.dp), color = Color.White, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.heightIn(max = 640.dp).verticalScroll(rememberScrollState()).padding(19.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(goal.name, fontSize = 21.sp, fontWeight = FontWeight.Bold, color = Ink)
                Text("Confirmado: ${amount(confirmed)} de ${amount(goal.targetCents)}", color = Jade)
                LinearProgressIndicator(progress = { (confirmed.toFloat() / goal.targetCents.coerceAtLeast(1)).coerceIn(0f, 1f) },
                    color = Jade, trackColor = Mint, modifier = Modifier.fillMaxWidth())
                Text("Dónde aportar", fontWeight = FontWeight.Bold, color = Ink)
                if (methods.isEmpty()) EmptyHint("Todavía no se han registrado Yape, Plin o cuentas bancarias.")
                methods.forEach { m ->
                    Surface(color = Ivory, shape = RoundedCornerShape(12.dp)) {
                        Column(Modifier.fillMaxWidth().padding(10.dp)) {
                            Text("${m.type} · ${m.holder}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            listOf("Número" to m.phone, "Cuenta" to m.account, "CCI" to m.cci).forEach { (label, value) ->
                                if (value.isNotBlank()) Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    Text("$label: $value", Modifier.weight(1f), fontSize = 12.sp)
                                    IconButton(onClick = {
                                        clipboard.setText(AnnotatedString(value)); onMessage("$label copiado")
                                    }) { Icon(Icons.Outlined.ContentCopy, "Copiar $label", tint = Jade) }
                                }
                            }
                        }
                    }
                }
                OutlinedButton(onClick = onAddMethod, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Outlined.Add, null); Text("Agregar Yape, Plin o banco")
                }
                Button(onClick = onContribute, modifier = Modifier.fillMaxWidth()) { Text("Registrar aporte") }
                Text("Aportes registrados", fontWeight = FontWeight.Bold, color = Ink)
                if (contributions.isEmpty()) EmptyHint("Ningún aporte por ahora.")
                contributions.forEach { a ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(amount(a.amountCents), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("${members.find { it.id == a.memberId }?.name ?: "Integrante"} · ${a.status}", fontSize = 10.sp, color = Muted)
                        }
                        if (a.status == "PENDIENTE") {
                            TextButton(onClick = { onConfirm(a.id) }) { Text("Confirmar", fontSize = 11.sp) }
                        } else Icon(Icons.Outlined.CheckCircle, "Confirmado", tint = Jade)
                    }
                    HorizontalDivider(color = Line)
                }
                Text("Versión local de prueba: aún no existen roles ni verificación bancaria. Confirma únicamente aportes comprobados.", fontSize = 10.sp, color = Muted)
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) { Text("Cerrar") }
            }
        }
    }
}

@Composable
private fun MethodDialog(onDismiss: () -> Unit, onSave: (String, String, String, String, String) -> Unit) {
    val types = listOf("Yape", "Plin", "BCP", "Interbank", "BBVA", "Banco de la Nación", "Otro banco")
    var type by remember { mutableStateOf(types.first()) }
    var holder by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var account by remember { mutableStateOf("") }
    var cci by remember { mutableStateOf("") }
    CasaDialog("Dónde recibir aportes", onDismiss,
        holder.isNotBlank() && (phone.isNotBlank() || account.isNotBlank() || cci.isNotBlank()),
        { onSave(type, holder, phone, account, cci) }) {
        Selection("Método", types.map { it to it }, type, { type = it })
        OutlinedTextField(holder, { holder = it }, label = { Text("Nombre del titular") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        if (type == "Yape" || type == "Plin") {
            OutlinedTextField(phone, { phone = it }, label = { Text("Número telefónico") },
                singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth())
        } else {
            OutlinedTextField(account, { account = it }, label = { Text("Número de cuenta (opcional)") },
                singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(cci, { cci = it }, label = { Text("CCI (opcional)") },
                singleLine = true, modifier = Modifier.fillMaxWidth())
        }
        Text("Estos campos se cifran localmente con Android Keystore. No escribas claves ni contraseñas bancarias.", fontSize = 11.sp, color = Muted)
    }
}

@Composable
private fun ContributionDialog(members: List<FamilyMember>, onDismiss: () -> Unit,
    onSave: (String, Long, Uri?) -> Unit) {
    var price by remember { mutableStateOf("") }
    var member by remember(members) { mutableStateOf(members.firstOrNull()?.id.orEmpty()) }
    var receipt by remember { mutableStateOf<Uri?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { receipt = it }
    CasaDialog("Registrar aporte", onDismiss, member.isNotBlank() && cents(price) != null,
        { cents(price)?.let { onSave(member, it, receipt) } }, "Registrar") {
        Selection("Quién aportó", members.map { it.id to it.name }, member, { member = it })
        MoneyInput(price, { price = it })
        OutlinedButton(onClick = { picker.launch("image/*") }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Outlined.PhotoCamera, null); Spacer(Modifier.width(8.dp)); Text(if (receipt == null) "Adjuntar comprobante (opcional)" else "Comprobante seleccionado")
        }
        Text("El aporte se guardará PENDIENTE. Solo sumará al saldo después de confirmarlo.", fontSize = 11.sp, color = Muted)
    }
}
