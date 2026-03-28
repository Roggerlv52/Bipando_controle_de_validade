package com.rogger.bp.notification;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.rogger.bp.R;
import com.rogger.bp.data.model.Produto;

import java.util.List;

public class NotificationUtil {
    private static final String TAG = "NotificationUtil";
    private static final String CHANNEL_ID = "validade_channel";

    public static void createChannel(Context c) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel =
                    new NotificationChannel(
                            CHANNEL_ID,
                            "Validade de Produtos",
                            NotificationManager.IMPORTANCE_DEFAULT
                    );

            NotificationManager nm =
                    c.getSystemService(NotificationManager.class);
            if (nm != null) {
                nm.createNotificationChannel(channel);
            }
        }
    }

    public static void showVencendo(Context c, List<Produto> produtos) {
        if (produtos == null || produtos.isEmpty()) return;

        // Verifica permissão no Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(c, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "Permissão de notificação não concedida.");
                return;
            }
        }

        String title = produtos.size() == 1
                ? "Produto vencendo"
                : produtos.size() + " produtos vencendo";

        String body = produtos.size() == 1
                ? "O produto '" + produtos.get(0).getNome() + "' está próximo do vencimento."
                : "Você tem " + produtos.size() + " produtos próximos do vencimento.";

        Notification n =
                new NotificationCompat.Builder(c, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_launcher_foreground)
                        .setContentTitle(title)
                        .setContentText(body)
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .setAutoCancel(true)
                        .build();

        NotificationManagerCompat.from(c).notify(1001, n);
    }

    public static void showVencidos(Context c, List<Produto> produtos) {
        if (produtos == null || produtos.isEmpty()) return;

        // Verifica permissão no Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(c, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "Permissão de notificação não concedida.");
                return;
            }
        }

        String title = produtos.size() == 1
                ? "Produto vencido"
                : produtos.size() + " produtos vencidos";

        String body = produtos.size() == 1
                ? "O produto '" + produtos.get(0).getNome() + "' já venceu!"
                : "Você tem " + produtos.size() + " produtos que já venceram!";

        Notification n =
                new NotificationCompat.Builder(c, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_add_categore)
                        .setContentTitle(title)
                        .setContentText(body)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setAutoCancel(true)
                        .build();

        NotificationManagerCompat.from(c).notify(1002, n);
    }
}