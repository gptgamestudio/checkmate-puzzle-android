package com.example

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.ui.LandscapeChessScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.ChessPuzzleViewModel

class MainActivity : ComponentActivity() {

    private val chessViewModel: ChessPuzzleViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme(darkTheme = true) {
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .safeDrawingPadding(),
                    containerColor = Color(0xFF0F1015)
                ) { innerPadding ->
                    LandscapeChessScreen(
                        viewModel = chessViewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}
