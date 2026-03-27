package com.rogger.bp.notification;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import androidx.annotation.RequiresPermission;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.rogger.bp.R;
import com.rogger.bp.data.model.Produto;

import java.util.List;

public class NotificationUtil {

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
            nm.createNotificationChannel(channel);
        }
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    public static void showVencendo(Context c, List<Produto> produtos) {

        String title = produtos.size() == 1
                ? "Produto vencendo"
                : produtos.size() + " produtos vencendo";

        String body = produtos.get(0).getNome();

        Notification n =
                new NotificationCompat.Builder(c, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_launcher_foreground)
                        .setContentTitle(title)
                        .setContentText(body)
                        .setAutoCancel(true)
                        .build();

        NotificationManagerCompat.from(c)
                .notify(1001, n);
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    public static void showVencidos(Context c, List<Produto> produtos) {

        String title = produtos.size() == 1
                ? "Produto vencido"
                : produtos.size() + " produtos vencidos";

        Notification n =
                new NotificationCompat.Builder(c, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_add_categore)
                        .setContentTitle(title)
                        .setAutoCancel(true)
                        .build();

        NotificationManagerCompat.from(c)
                .notify(1002, n);
    }
}
