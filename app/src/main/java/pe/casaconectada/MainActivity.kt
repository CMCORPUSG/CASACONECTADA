package pe.casaconectada

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import dagger.hilt.android.AndroidEntryPoint
import pe.casaconectada.design.CasaTheme
import pe.casaconectada.presentation.CasaApp
import pe.casaconectada.presentation.FamilyViewModel

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val vm: FamilyViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { CasaTheme { CasaApp(vm) } }
    }
}
