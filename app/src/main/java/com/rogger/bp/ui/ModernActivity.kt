package com.rogger.bp.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.rogger.bp.ui.naviation.BipandoNavGraph
import com.rogger.bp.ui.theme.BipandoTheme

class ModernActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BipandoTheme {
                val navController = rememberNavController()
                BipandoNavGraph(navController = navController)
            }
        }
    }
}
