package pe.casaconectada.account

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pe.casaconectada.design.*
import pe.casaconectada.presentation.CasaApp
import pe.casaconectada.presentation.FamilyViewModel

/**
 * El modo Room es SOLO un espacio de datos locales del dispositivo.
 * JAMAS se presenta como información sincronizada del usuario autenticado.
 */
@Composable
fun AccountGateway(localViewModel: FamilyViewModel, accountViewModel: AccountViewModel) {
    var localMode by rememberSaveable { mutableStateOf(false) }
    val state by accountViewModel.state.collectAsStateWithLifecycle()
    if (localMode) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().background(Ink)
                    .padding(horizontal = 14.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("MODO LOCAL DEL DISPOSITIVO", color = Color.White, fontSize = 12.sp,
                        fontWeight = FontWeight.Bold)
                    Text("Datos de demostración: NO vinculados a ninguna cuenta",
                        color = Color.White.copy(alpha = .85f), fontSize = 10.sp)
                }
                TextButton(onClick = { localMode = false }) {
                    Text("Mi cuenta", color = Color.White)
                }
            }
            Box(Modifier.weight(1f)) { CasaApp(localViewModel) }
        }
    } else {
        AccountPilotScreen(state, accountViewModel, onLocalMode = { localMode = true })
    }
}

@Composable
private fun AccountPilotScreen(state: AccountState, vm: AccountViewModel, onLocalMode: () -> Unit) {
    Box(
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Ivory, Mint, Ivory)))
            .statusBarsPadding()
    ) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            BrandHeader()
            if (state.loading) {
                Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Jade)
                }
            } else if (state.profile == null) {
                LoginForm(state, vm)
                InfoCard {
                    Text("Modo local independiente", fontWeight = FontWeight.Bold, color = Ink)
                    Text("Abre la aplicación anterior sin conectar tareas, pagos o metas a una cuenta. " +
                        "Los registros locales aún NO tienen privacidad multiusuario ni sincronización.",
                        color = Muted, fontSize = 12.sp)
                    OutlinedButton(onClick = onLocalMode, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Outlined.Home, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Abrir funciones locales")
                    }
                }
            } else {
                AccountDashboard(state, vm)
            }
            Text("FASE ANDROID LOCAL · Requiere Wrangler y adb reverse TCP 8787. " +
                "No utilizar con datos bancarios privados reales ni distribuir esta APK debug.",
                color = Muted, fontSize = 11.sp)
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun BrandHeader() {
    Row(verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp)) {
        Box(Modifier.size(62.dp).clip(RoundedCornerShape(19.dp)).background(Ink),
            contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.Home, contentDescription = null, tint = Color.White,
                modifier = Modifier.size(34.dp))
        }
        Column {
            Text("CASA CONECTADA", color = Ink, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
            Text("Tu hogar, un equipo", color = Jade, fontWeight = FontWeight.Medium, fontSize = 13.sp)
            Text("Cuentas familiares · Prueba local", color = Muted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun InfoCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(23.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Line)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content)
    }
}

@Composable
private fun Feedback(message: String) {
    if (message.isNotBlank()) {
        Surface(color = Lavender, shape = RoundedCornerShape(12.dp)) {
            Text(message, modifier = Modifier.fillMaxWidth().padding(12.dp), color = Ink, fontSize = 12.sp)
        }
    }
}

@Composable
private fun PasswordField(label: String, password: String, onChange: (String) -> Unit,
    enabled: Boolean = true) {
    var visible by rememberSaveable { mutableStateOf(false) }
    OutlinedTextField(
        value = password, onValueChange = onChange, modifier = Modifier.fillMaxWidth(),
        label = { Text(label) }, singleLine = true, enabled = enabled,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                    contentDescription = if (visible) "Ocultar contraseña" else "Ver contraseña")
            }
        }
    )
}

@Composable
private fun LoginForm(state: AccountState, vm: AccountViewModel) {
    var registering by rememberSaveable { mutableStateOf(false) }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("") }
    var code by rememberSaveable { mutableStateOf("") }
    InfoCard {
        Text(if (registering) "Aceptar invitación" else "Bienvenido a casa",
            fontWeight = FontWeight.ExtraBold, color = Ink, fontSize = 24.sp)
        Text(if (registering) "Cada integrante crea su cuenta personal MEMBER." else
            "Inicia sesión con tu cuenta ADMIN o MEMBER.", color = Muted, fontSize = 13.sp)
        if (registering) {
            OutlinedTextField(code, { code = it }, modifier = Modifier.fillMaxWidth(),
                label = { Text("Código de invitación") }, singleLine = true)
            OutlinedTextField(name, { name = it }, modifier = Modifier.fillMaxWidth(),
                label = { Text("Nombre completo") }, singleLine = true)
        }
        OutlinedTextField(email, { email = it }, modifier = Modifier.fillMaxWidth(),
            label = { Text("Correo electrónico") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
        PasswordField("Contraseña (mínimo 12 caracteres)", password, { password = it }, !state.busy)
        Feedback(state.message)
        Button(
            modifier = Modifier.fillMaxWidth().height(52.dp),
            enabled = !state.busy && email.contains('@') && password.length in 12..128 &&
                (!registering || (code.isNotBlank() && name.isNotBlank())),
            onClick = {
                if (registering) vm.register(code, email, name, password)
                else vm.login(email, password)
            },
            colors = ButtonDefaults.buttonColors(containerColor = Jade),
            shape = RoundedCornerShape(15.dp)
        ) {
            if (state.busy) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White,
                strokeWidth = 2.dp)
            else Text(if (registering) "Crear cuenta e ingresar" else "Iniciar sesión")
        }
        TextButton(onClick = { registering = !registering; password = "" },
            modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text(if (registering) "Ya tengo cuenta" else "Tengo una invitación para registrarme")
        }
        Text("El ADMIN se creó previamente en el backend local. No se registra nuevamente aquí.",
            fontSize = 11.sp, color = Muted)
    }
}

@Composable
private fun AccountDashboard(state: AccountState, vm: AccountViewModel) {
    val profile = state.profile ?: return
    val family = state.families.firstOrNull { it.id == state.familyId }
    var tab by rememberSaveable { mutableStateOf("perfil") }
    var name by remember(profile.id) { mutableStateOf(profile.name) }
    var username by remember(profile.id) { mutableStateOf(profile.username) }
    var phone by remember(profile.id) { mutableStateOf(profile.phone) }
    var bio by remember(profile.id) { mutableStateOf(profile.bio) }
    var invitedEmail by rememberSaveable { mutableStateOf("") }
    val context = LocalContext.current
    LaunchedEffect(profile.name, profile.username, profile.phone, profile.bio) {
        name = profile.name; username = profile.username; phone = profile.phone; bio = profile.bio
    }
    InfoCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(54.dp).clip(CircleShape).background(Mint),
                contentAlignment = Alignment.Center) {
                Text(profile.name.take(1).uppercase(), color = Jade,
                    fontWeight = FontWeight.Bold, fontSize = 23.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(profile.name, fontWeight = FontWeight.Bold, color = Ink, fontSize = 18.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(profile.email, color = Muted, fontSize = 12.sp, maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
                Text("${family?.name ?: "Sin familia"} · ${family?.role ?: "—"}",
                    color = Jade, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = tab == "perfil", onClick = { tab = "perfil" }, label = { Text("Mi perfil") })
            FilterChip(selected = tab == "familia", onClick = { tab = "familia" }, label = { Text("Mi familia") })
        }
        Feedback(state.message)
    }
    if (tab == "perfil") {
        InfoCard {
            Text("Mi perfil personal", fontWeight = FontWeight.Bold, color = Ink, fontSize = 19.sp)
            Text("Solo tú puedes modificar estos datos mediante tu sesión.", color = Muted,
                fontSize = 12.sp)
            OutlinedTextField(name, { name = it }, label = { Text("Nombre visible") },
                singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(username, { username = it }, label = { Text("Username") },
                singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(phone, { phone = it }, label = { Text("Teléfono (opcional)") },
                singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth())
            OutlinedTextField(bio, { bio = it }, label = { Text("Sobre mí (opcional)") },
                modifier = Modifier.fillMaxWidth(), minLines = 2, maxLines = 3)
            Button(onClick = { vm.saveProfile(name, username, phone, bio) },
                modifier = Modifier.fillMaxWidth(), enabled = !state.busy && name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Jade)) {
                Icon(Icons.Outlined.Save, null)
                Spacer(Modifier.width(8.dp))
                Text("Guardar mi perfil")
            }
            Text("Foto de perfil, cambio de correo y verificación por email: pendientes.",
                color = Muted, fontSize = 11.sp)
        }
    } else {
        InfoCard {
            Text("Mi familia", fontWeight = FontWeight.Bold, color = Ink, fontSize = 19.sp)
            if (state.families.size > 1) state.families.forEach { f ->
                FilterChip(selected = state.familyId == f.id, onClick = { vm.changeFamily(f.id) },
                    label = { Text("${f.name} · ${f.role}") })
            }
            if (state.members.isEmpty()) Text("Sin integrantes recibidos por ahora.", color = Muted)
            state.members.forEach { m ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.PersonOutline, null, tint = Jade)
                    Spacer(Modifier.width(10.dp))
                    Text(m.name, modifier = Modifier.weight(1f), color = Ink)
                    Text(m.role, color = Muted, fontSize = 11.sp)
                }
                HorizontalDivider(color = Line)
            }
            OutlinedButton(onClick = vm::refresh, enabled = !state.busy,
                modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.Refresh, null)
                Spacer(Modifier.width(8.dp))
                Text("Actualizar integrantes")
            }
        }
        if (family?.role == "ADMIN") InfoCard {
            Text("Invitar familiar", color = Ink, fontWeight = FontWeight.Bold, fontSize = 19.sp)
            Text("El código vence en 24 horas. Envía el código únicamente por un canal privado.",
                color = Muted, fontSize = 12.sp)
            OutlinedTextField(invitedEmail, { invitedEmail = it },
                modifier = Modifier.fillMaxWidth(), label = { Text("Correo del integrante") },
                singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
            Button(onClick = { vm.invite(invitedEmail) },
                modifier = Modifier.fillMaxWidth(), enabled = !state.busy && invitedEmail.contains('@'),
                colors = ButtonDefaults.buttonColors(containerColor = Jade)) {
                Icon(Icons.Outlined.PersonAdd, null)
                Spacer(Modifier.width(8.dp))
                Text("Crear invitación")
            }
            if (state.invitation.isNotBlank()) {
                Text("Invitación de ${state.invitedEmail}", fontWeight = FontWeight.Bold, color = Ink)
                Text(state.invitation.chunked(16).joinToString(" "), color = Ink, fontSize = 12.sp)
                OutlinedButton(onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Código CASA CONECTADA", state.invitation))
                }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Outlined.ContentCopy, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Copiar código privado")
                }
            }
        }
    }
    InfoCard {
        Text("Tus tareas y pagos aún no están sincronizados con esta cuenta.",
            color = Muted, fontSize = 12.sp)
        OutlinedButton(onClick = vm::logout, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Outlined.Logout, null)
            Spacer(Modifier.width(8.dp))
            Text("Cerrar sesión")
        }
    }
}
