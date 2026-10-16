package com.flatnotes

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.flatnotes.ui.FlatNotesApp
import com.flatnotes.ui.theme.FlatPalette
import com.flatnotes.ui.theme.FlatTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                AndroidColor.TRANSPARENT,
                AndroidColor.TRANSPARENT,
            ),
            navigationBarStyle = SystemBarStyle.light(
                AndroidColor.TRANSPARENT,
                AndroidColor.TRANSPARENT,
            ),
        )
        setContent {
            FlatTheme {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(FlatPalette.Background),
                ) {
                    FlatNotesApp()
                }
            }
        }
    }
}
