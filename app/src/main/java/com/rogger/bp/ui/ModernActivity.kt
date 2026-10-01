package com.rogger.bp.ui

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.rogger.bp.data.image.notification.ImageSyncScheduler
import com.rogger.bp.notification.NotificationScheduler
import com.rogger.bp.notification.NotificationUtil
import com.rogger.bp.ui.commun.AnalyticsManager
import com.rogger.bp.ui.naviation.BipandoNavGraph
import com.rogger.bp.ui.theme.BipandoTheme

class ModernActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (intent?.getBooleanExtra("from_notification", false) == true) {
            AnalyticsManager.logNotificationOpened()
        }
        
        // Inicializações fundamentais
        ImageSyncScheduler.start(this)
        NotificationUtil.createChannel(this)
        NotificationScheduler.start(this)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )

        // No Android 10 (API 29) ou superior, desativa o contraste forçado da barra de navegação
        // para garantir transparência total permitindo visualizar os itens por trás da barra inferior.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        setContent {
            BipandoTheme {
                val navController = rememberNavController()
                BipandoNavGraph(navController = navController)
            }
        }
    }
}
