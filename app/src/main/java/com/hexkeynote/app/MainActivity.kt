package com.hexkeynote.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.hexkeynote.app.ui.HexKeyNoteApp
import com.hexkeynote.app.ui.theme.HexKeyNoteTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HexKeyNoteTheme {
                HexKeyNoteApp()
            }
        }
    }
}
