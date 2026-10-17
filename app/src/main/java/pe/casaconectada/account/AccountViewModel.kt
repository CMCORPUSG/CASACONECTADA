package pe.casaconectada.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.IOException
import javax.inject.Inject

/** Credenciales/sesión aisladas del almacenamiento Room local anterior. */
data class AccountState(
    val loading: Boolean = true,
    val busy: Boolean = false,
    val profile: AccountProfile? = null,
    val families: List<AccountFamily> = emptyList(),
    val members: List<AccountMember> = emptyList(),
    val familyId: String = "",
    val invitation: String = "",
    val invitedEmail: String = "",
    val message: String = ""
)

@HiltViewModel
class AccountViewModel @Inject constructor(
    private val api: AccountApi,
    private val tokenStore: AccountTokenStore
) : ViewModel() {
    private val _state = MutableStateFlow(AccountState())
    val state: StateFlow<AccountState> = _state.asStateFlow()
    private var bearer: String? = null

    init {
        viewModelScope.launch {
            bearer = tokenStore.load()
            if (bearer == null) _state.update { it.copy(loading = false) }
            else fetchProfile(clearSessionIfUnauthorized = true)
        }
    }

    private fun friendly(ex: Exception): String = when (ex) {
        is AccountApiException -> when (ex.code) {
            "invalid_credentials" -> "Correo o contraseña incorrectos."
            "invalid_input" -> "Revisa los datos. La contraseña requiere de 12 a 128 caracteres."
            "invitation_invalid" -> "La invitación es inválida, venció o no coincide con el correo."
            "registration_failed_or_invitation_used" -> "Ese correo ya existe o la invitación fue utilizada."
            "admin_only" -> "Solo el ADMIN puede invitar integrantes."
            "authentication_required" -> "La sesión venció. Inicia sesión nuevamente."
            "profile_conflict" -> "Ese nombre de usuario ya está ocupado."
            "invalid_display_name" -> "Ingresa un nombre válido."
            else -> "El servidor respondió ${ex.status}: ${ex.code}"
        }
        is IOException -> "No se pudo conectar. Revisa Wrangler y adb reverse en el puerto 8787."
        else -> "Operación no completada: ${ex.localizedMessage ?: "error"}"
    }

    private fun runAction(action: suspend () -> Unit) {
        if (_state.value.busy) return
        viewModelScope.launch {
            _state.update { it.copy(busy = true, message = "", invitation = "") }
            try { action() }
            catch (ex: Exception) { _state.update { it.copy(message = friendly(ex)) } }
            finally { _state.update { it.copy(busy = false, loading = false) } }
        }
    }

    private suspend fun fetchProfile(clearSessionIfUnauthorized: Boolean = false) {
        val token = bearer
        if (token == null) { _state.update { it.copy(loading = false) }; return }
        try {
            val snapshot = parseAccount(api.request("GET", "/api/v1/me", token = token))
            val selected = _state.value.familyId.takeIf { id -> snapshot.families.any { it.id == id } }
                ?: snapshot.families.firstOrNull()?.id.orEmpty()
            _state.update { it.copy(loading = false, profile = snapshot.profile,
                families = snapshot.families, familyId = selected, members = emptyList()) }
            fetchMembers(selected, token)
        } catch (ex: Exception) {
            if (clearSessionIfUnauthorized && ex is AccountApiException && ex.status == 401) {
                clearLocal()
                _state.update { AccountState(loading = false, message = "La sesión venció. Inicia sesión.") }
            } else {
                _state.update { it.copy(loading = false, message = friendly(ex)) }
            }
        }
    }

    private suspend fun fetchMembers(id: String, token: String) {
        if (id.isBlank()) return
        try {
            val array = api.request("GET", "/api/v1/families/$id/members", token = token)
                .optJSONArray("members") ?: return
            val result = (0 until array.length()).mapNotNull { index -> array.optJSONObject(index)?.let {
                AccountMember(it.optString("displayName"), it.optString("role"))
            } }
            _state.update { it.copy(members = result) }
        } catch (_: Exception) {
            // El perfil sigue disponible aunque falle la lista familiar.
        }
    }

    private fun clearLocal() { bearer = null; tokenStore.clear() }

    fun login(email: String, password: String) = runAction {
        val response = api.request("POST", "/api/v1/auth/login", JSONObject()
            .put("email", email.trim()).put("password", password))
        val token = response.getString("accessToken")
        tokenStore.save(token)
        bearer = token
        fetchProfile()
    }

    fun register(invite: String, email: String, name: String, password: String) = runAction {
        val response = api.request("POST", "/api/v1/auth/register-invite", JSONObject()
            .put("invitationToken", invite.trim())
            .put("email", email.trim())
            .put("displayName", name.trim())
            .put("password", password))
        val token = response.getString("accessToken")
        tokenStore.save(token)
        bearer = token
        fetchProfile()
    }

    fun saveProfile(name: String, username: String, phone: String, bio: String) = runAction {
        val token = requireNotNull(bearer)
        api.request("PATCH", "/api/v1/me", JSONObject()
            .put("displayName", name.trim()).put("username", username.trim())
            .put("phone", phone.trim()).put("bio", bio.trim()), token)
        fetchProfile()
        _state.update { it.copy(message = "Perfil actualizado correctamente.") }
    }

    fun changeFamily(familyId: String) {
        val token = bearer ?: return
        if (_state.value.families.none { it.id == familyId }) return
        viewModelScope.launch {
            _state.update { it.copy(familyId = familyId, members = emptyList(), invitation = "") }
            fetchMembers(familyId, token)
        }
    }

    fun invite(email: String) = runAction {
        val token = requireNotNull(bearer)
        val family = _state.value.families.firstOrNull { it.id == _state.value.familyId }
            ?: throw IllegalStateException("Selecciona una familia")
        require(family.role == "ADMIN") { "Solo ADMIN puede invitar" }
        val result = api.request("POST", "/api/v1/families/${family.id}/invitations",
            JSONObject().put("email", email.trim()), token)
        _state.update { it.copy(invitation = result.getString("invitationToken"),
            invitedEmail = email.trim(), message = "Invitación creada. Comparte el código en privado (24 horas).") }
    }

    fun refresh() = runAction { fetchProfile() }

    fun logout() {
        val token = bearer
        clearLocal()
        _state.value = AccountState(loading = false, message = "Sesión cerrada en este dispositivo.")
        if (token != null) viewModelScope.launch {
            try { api.request("POST", "/api/v1/auth/logout", token = token) }
            catch (_: Exception) { /* Servidor sin conexión: el token local ya se eliminó. */ }
        }
    }
}
