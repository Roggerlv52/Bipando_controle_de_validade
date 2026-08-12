package com.rogger.bp.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.rogger.bp.data.image.notification.ImageSyncScheduler
import com.rogger.bp.notification.NotificationScheduler
import com.rogger.bp.notification.NotificationUtil
import com.rogger.bp.ui.naviation.BipandoNavGraph
import com.rogger.bp.ui.theme.BipandoTheme

class ModernActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Inicializações fundamentais
        ImageSyncScheduler.start(this)
        NotificationUtil.createChannel(this)
        NotificationScheduler.start(this)

        enableEdgeToEdge()
        setContent {
            BipandoTheme {
                val navController = rememberNavController()
                BipandoNavGraph(navController = navController)
            }
        }
    }
}
