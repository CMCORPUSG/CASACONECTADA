package pe.casaconectada

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import dagger.hilt.android.AndroidEntryPoint
import pe.casaconectada.design.CasaTheme
import pe.casaconectada.presentation.CasaApp
import pe.casaconectada.presentation.FamilyViewModel
import pe.casaconectada.account.AccountViewModel
import pe.casaconectada.account.AccountGateway

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val vm: FamilyViewModel by viewModels()
    private val accountVm: AccountViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { CasaTheme { if (BuildConfig.DEBUG) AccountGateway(vm, accountVm) else CasaApp(vm) } }
    }
}
