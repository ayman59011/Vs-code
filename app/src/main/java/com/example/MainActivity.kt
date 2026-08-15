package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.WorkspaceScreen
import com.example.ui.theme.CodeEditorTheme
import com.example.viewmodels.CodeEditorViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: CodeEditorViewModel = viewModel()
            
            // Observe custom settings
            val currentTheme by viewModel.settingsManager.theme.collectAsState()
            val currentThemeColor by viewModel.settingsManager.themeColor.collectAsState()
            val currentLanguage by viewModel.settingsManager.language.collectAsState()

            // Dynamic RTL layout direction based on selected Arabic/English language setting
            val layoutDirection = if (currentLanguage == "ar") {
                LayoutDirection.Rtl
            } else {
                LayoutDirection.Ltr
            }

            CodeEditorTheme(
                darkTheme = currentTheme == "dark",
                accentColorHex = currentThemeColor
            ) {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                        WorkspaceScreen(
                            viewModel = viewModel,
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
            }
        }
    }
}
