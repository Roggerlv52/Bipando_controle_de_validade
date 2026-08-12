package com.rogger.bp.notification;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.RequiresPermission;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.net.Uri;
import android.provider.Settings;
import android.media.AudioAttributes;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.rogger.bp.ui.ModernActivity;
import com.rogger.bp.R;
import com.rogger.bp.data.model.PostProduct;

import java.util.List;

public class NotificationUtil {
    // ID base do canal. Concatenamos o tipo para forçar a atualização das configurações no Android 8+
    public static String getChannelId(Context c) {
        int type = NotificationPrefs.getSoundType(c);
        String uri = NotificationPrefs.getSoundUri(c);
        // O ID muda conforme a preferência de som para que o Android aplique a nova config (canais são imutáveis)
        return "validade_channel_v2_" + type + "_" + uri.hashCode();
    }

    /**
     * Cria o canal de notificação baseado nas preferências do usuário.
     */
    public static void createChannel(Context c) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelId = getChannelId(c);
            int soundType = NotificationPrefs.getSoundType(c);
            String soundUriString = NotificationPrefs.getSoundUri(c);

            NotificationManager nm = c.getSystemService(NotificationManager.class);
            if (nm == null) return;

            // Se o canal já existe, não faz nada
            if (nm.getNotificationChannel(channelId) != null) return;

            // Remove canais antigos para não poluir as configurações do sistema
            for (NotificationChannel oldChannel : nm.getNotificationChannels()) {
                if (oldChannel.getId().startsWith("validade_channel_v2_")) {
                    nm.deleteNotificationChannel(oldChannel.getId());
                }
            }

            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            if (soundType == 0) importance = NotificationManager.IMPORTANCE_LOW; // Mudo
            
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    c.getString(R.string.notification_name),
                    importance
            );
            channel.setDescription(c.getString(R.string.notification_description));

            // Configura vibração
            if (soundType == 1 || soundType == 2 || soundType == 3) {
                channel.enableVibration(true);
            } else {
                channel.enableVibration(false);
            }

            // Configura som
            if (soundType == 2 || soundType == 3) {
                Uri soundUri;
                if (soundType == 3 && !soundUriString.isEmpty()) {
                    soundUri = Uri.parse(soundUriString);
                } else {
                    soundUri = Settings.System.DEFAULT_NOTIFICATION_URI;
                }

                AudioAttributes audioAttributes = new AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .build();
                channel.setSound(soundUri, audioAttributes);
            } else {
                channel.setSound(null, null);
            }

            nm.createNotificationChannel(channel);
        }
    }

    /**
     * Verifica se o app tem permissão para postar notificações.
     */
    public static boolean temPermissao(Context c) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ActivityCompat.checkSelfPermission(
                    c, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    private static PendingIntent getPendingIntent(Context c) {
        Intent intent = new Intent(c, ModernActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        
        return PendingIntent.getActivity(c, 0, intent, flags);
    }

    public static void showVencendo(Context c, List<PostProduct> produtos) {
        if (produtos == null || produtos.isEmpty()) return;

        if (!temPermissao(c)) {
            return;
        }

        createChannel(c);
        String channelId = getChannelId(c);
        
        String title = produtos.size() == 1
                ? c.getString(R.string.notification_title)
                : produtos.size() + " "+c.getString(R.string.notification_title_2);

        String body = produtos.size() == 1
                ? c.getString(R.string.notif_vencendo_body_single, produtos.get(0).getName())
                : c.getString(R.string.notif_vencendo_body_multiple, produtos.size());

        NotificationCompat.Builder builder = new NotificationCompat.Builder(c, channelId)
                .setSmallIcon(R.drawable.ic_bp_logo_small)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setColor(ContextCompat.getColor(c, R.color.bipando_color))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setContentIntent(getPendingIntent(c))
                .setAutoCancel(true);

        // Som para versões antigas (< Oreo)
        int soundType = NotificationPrefs.getSoundType(c);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            if (soundType == 2 || soundType == 3) {
                String soundUriString = NotificationPrefs.getSoundUri(c);
                Uri uri = (soundType == 3 && !soundUriString.isEmpty()) 
                        ? Uri.parse(soundUriString) 
                        : Settings.System.DEFAULT_NOTIFICATION_URI;
                builder.setSound(uri);
            }
            if (soundType == 1 || soundType == 2 || soundType == 3) {
                builder.setDefaults(Notification.DEFAULT_VIBRATE);
            }
        }

        if (ActivityCompat.checkSelfPermission(c, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        NotificationManagerCompat.from(c).notify(1001, builder.build());
    }

    public static void showVencidos(Context c, List<PostProduct> produtos) {
        if (produtos == null || produtos.isEmpty()) return;

        if (!temPermissao(c)) {
            return;
        }

        createChannel(c);
        String channelId = getChannelId(c);

        String title = produtos.size() == 1
                ? c.getString(R.string.notif_vencidos_title_single)
                : c.getString(R.string.notif_vencidos_title_multiple, produtos.size());

        String body = produtos.size() == 1
                ? c.getString(R.string.notif_vencidos_body_single, produtos.get(0).getName())
                : c.getString(R.string.notif_vencidos_body_multiple, produtos.size());

        NotificationCompat.Builder builder = new NotificationCompat.Builder(c, channelId)
                .setSmallIcon(R.drawable.ic_bp_logo_small)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setColor(ContextCompat.getColor(c, R.color.bipando_color))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setContentIntent(getPendingIntent(c))
                .setAutoCancel(true);

        // Som para versões antigas (< Oreo)
        int soundType = NotificationPrefs.getSoundType(c);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            if (soundType == 2 || soundType == 3) {
                String soundUriString = NotificationPrefs.getSoundUri(c);
                Uri uri = (soundType == 3 && !soundUriString.isEmpty()) 
                        ? Uri.parse(soundUriString) 
                        : Settings.System.DEFAULT_NOTIFICATION_URI;
                builder.setSound(uri);
            }
            if (soundType == 1 || soundType == 2 || soundType == 3) {
                builder.setDefaults(Notification.DEFAULT_VIBRATE);
            }
        }

        if (ActivityCompat.checkSelfPermission(c, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        NotificationManagerCompat.from(c).notify(1002, builder.build());
    }

    public static void showInvitation(Context c, String senderName, String groupName) {
        if (!temPermissao(c)) return;

        createChannel(c);
        String channelId = getChannelId(c);

        String title = "Novo Convite de Grupo";
        String body = senderName + " convidou você para o grupo " + groupName;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(c, channelId)
                .setSmallIcon(R.drawable.ic_bp_logo_small)
                .setContentTitle(title)
                .setContentText(body)
                .setColor(ContextCompat.getColor(c, R.color.bipando_color))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(getPendingIntent(c));

        if (ActivityCompat.checkSelfPermission(c, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        NotificationManagerCompat.from(c).notify(1003, builder.build());
    }
}
