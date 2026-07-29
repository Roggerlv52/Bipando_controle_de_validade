package com.rogger.bp.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.rogger.bp.MainActivity
import com.rogger.bp.ui.naviation.BipandoNavGraph
import com.rogger.bp.ui.naviation.Routes
import com.rogger.bp.ui.theme.BipandoTheme

class ModernActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BipandoTheme {
                val navController = rememberNavController()
                BipandoNavGraph(navController = navController)

                // Observar navegação para Home para abrir a MainActivity antiga
                navController.addOnDestinationChangedListener { _, destination, _ ->
                    if (destination.route == Routes.HOME) {
                        startActivity(Intent(this@ModernActivity, MainActivity::class.java))
                        finish()
                    }
                }
            }
        }
    }
}
