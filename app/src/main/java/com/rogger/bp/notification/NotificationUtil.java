package com.rogger.bp.notification;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresPermission;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.rogger.bp.R;
import com.rogger.bp.data.model.Produto;

import java.util.List;

public class NotificationUtil {
    private static final String TAG = "NotificationUtil";
    public static final String CHANNEL_ID = "validade_channel";

    /**
     * Cria o canal de notificação.
     * DEVE ser chamado o mais cedo possível — na Application.onCreate()
     * e também aqui para garantir. É idempotente: criar um canal que já
     * existe não causa efeito colateral.
     */
    public static void createChannel(Context c) {
        if (Build.VERSION_CODES.P >= Build.VERSION.SDK_INT) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Validade de Produtos",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("Avisos de produtos próximos ao vencimento");
            channel.enableVibration(true);

            NotificationManager nm = c.getSystemService(NotificationManager.class);
            if (nm != null) {
                nm.createNotificationChannel(channel);
                Log.d(TAG, "Canal de notificação criado/confirmado.");
            }
        }
    }

    /**
     * Verifica se o app tem permissão para postar notificações.
     * No Android 13+ (TIRAMISU) a permissão POST_NOTIFICATIONS é runtime.
     * Abaixo disso, sempre retorna true.
     */
    public static boolean temPermissao(Context c) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ActivityCompat.checkSelfPermission(
                    c, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    public static void showVencendo(Context c, List<Produto> produtos) {
        if (produtos == null || produtos.isEmpty()) return;

        if (!temPermissao(c)) {
            Log.w(TAG, "Permissão POST_NOTIFICATIONS não concedida. Notificação ignorada.");
            return;
        }

        // Garante que o canal existe antes de notificar
        createChannel(c);

        String title = produtos.size() == 1
                ? "Produto vencendo"
                : produtos.size() + " produtos vencendo";

        String body = produtos.size() == 1
                ? "'" + produtos.get(0).getNome() + "' está próximo do vencimento."
                : "Você tem " + produtos.size() + " produtos próximos do vencimento.";

        Notification n = new NotificationCompat.Builder(c, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .build();

        if (ActivityCompat.checkSelfPermission(c,
                Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        NotificationManagerCompat.from(c).notify(1001, n);
        Log.d(TAG, "Notificação 'vencendo' exibida: " + produtos.size() + " produto(s)");
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    public static void showVencidos(Context c, List<Produto> produtos) {
        if (produtos == null || produtos.isEmpty()) return;

        if (!temPermissao(c)) {
            Log.w(TAG, "Permissão POST_NOTIFICATIONS não concedida. Notificação ignorada.");
            return;
        }

        // Garante que o canal existe antes de notificar
        createChannel(c);

        String title = produtos.size() == 1
                ? "Produto vencido"
                : produtos.size() + " produtos vencidos";

        String body = produtos.size() == 1
                ? "'" + produtos.get(0).getNome() + "' já venceu!"
                : "Você tem " + produtos.size() + " produtos que já venceram!";

        Notification n = new NotificationCompat.Builder(c, CHANNEL_ID)
                .setSmallIcon(R.drawable.bp_logo_small)
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build();

        NotificationManagerCompat.from(c).notify(1002, n);
        Log.d(TAG, "Notificação 'vencidos' exibida: " + produtos.size() + " produto(s)");
    }
}