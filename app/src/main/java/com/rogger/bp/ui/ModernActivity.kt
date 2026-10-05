package com.rogger.bp.ui

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
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

        // Configura Edge-to-Edge forçando ícones e texto claros (brancos) na barra de status no topo
        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )

        // No Android 10 (API 29) até Android 14 (API 34), desativa o contraste forçado da barra de navegação.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && Build.VERSION.SDK_INT < 35) {
            @Suppress("DEPRECATION")
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
