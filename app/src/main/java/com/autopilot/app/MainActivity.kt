package com.autopilot.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.autopilot.app.ui.navigation.AutoPilotNavShell
import com.autopilot.app.ui.theme.AutoPilotTheme
import com.autopilot.app.ui.viewmodel.AutoPilotViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as AutoPilotApplication
        val factory = AutoPilotViewModel.Factory(
            application = app,
            repository = app.container.repository,
            engine = app.container.automationEngine
        )

        setContent {
            val viewModel: AutoPilotViewModel = viewModel(factory = factory)
            val userSettings by viewModel.userSettings.collectAsStateWithLifecycle()

            AutoPilotTheme(themePreference = userSettings.theme) {
                AutoPilotNavShell(viewModel = viewModel)
            }
        }
    }
}
