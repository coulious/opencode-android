package com.example.opencode

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.rememberNavController
import com.example.opencode.data.local.SettingsStore
import com.example.opencode.ui.navigation.AppNavHost
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var settingsStore: SettingsStore

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val settings by settingsStore.settings.collectAsState(initial = null)
            val darkTheme = isSystemInDarkTheme()
            val context = LocalContext.current

            val baseScheme = when {
                android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S -> {
                    if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
                }
                darkTheme -> darkColorScheme()
                else -> lightColorScheme()
            }

            val themeColor = settings?.themeColor
            val colorScheme = if (themeColor != null && themeColor != 0) {
                val c = Color(themeColor)
                baseScheme.copy(
                    primary = c,
                    primaryContainer = c.copy(alpha = 0.3f),
                )
            } else baseScheme

            MaterialTheme(colorScheme = colorScheme) {
                val navController = rememberNavController()
                AppNavHost(navController = navController)
            }
        }
    }
}
