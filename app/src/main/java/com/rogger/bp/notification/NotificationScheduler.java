package com.rogger.bp.notification;

import android.content.Context;
import android.util.Log;

import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

public class NotificationScheduler {
    private static final String TAG = "NotificationScheduler";
    private static final String WORK_NAME = "expiration_worker";

    /**
     * Inicia o agendamento das notificações de validade.
     * O agendamento é feito para rodar uma vez por dia.
     */
    public static void start(Context c) {
        int hour = 14;
        int minute = 52;

        long initialDelay = calculateInitialDelay(hour, minute);
        
        // Garante que o canal de notificação esteja criado
        NotificationUtil.createChannel(c);

        Constraints constraints = new Constraints.Builder()
                .setRequiresBatteryNotLow(false) // Mudado para false para ser mais resiliente
                .build();

        PeriodicWorkRequest work =
                new PeriodicWorkRequest.Builder(
                        ExpirationWorker.class,
                        1,
                        TimeUnit.DAYS
                )
                        .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                        .setConstraints(constraints)
                        .addTag(TAG)
                        .build();

        Log.d(TAG, "Agendando worker para rodar em " + (initialDelay / 60000) + " minutos.");

        WorkManager.getInstance(c).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP, // Mantém o agendamento existente se já houver um
                work
        );
    }

    /**
     * Para o agendamento das notificações.
     */
    public static void stop(Context c) {
        Log.d(TAG, "Cancelando agendamento de notificações.");
        WorkManager.getInstance(c).cancelUniqueWork(WORK_NAME);
    }

    /**
     * Calcula quanto tempo falta até a próxima execução no horário escolhido.
     */
    private static long calculateInitialDelay(int hour, int minute) {
        Calendar now = Calendar.getInstance();
        Calendar nextRun = Calendar.getInstance();

        nextRun.set(Calendar.HOUR_OF_DAY, hour);
        nextRun.set(Calendar.MINUTE, minute);
        nextRun.set(Calendar.SECOND, 0);
        nextRun.set(Calendar.MILLISECOND, 0);

        if (nextRun.before(now)) {
            nextRun.add(Calendar.DAY_OF_MONTH, 1);
        }

        return nextRun.getTimeInMillis() - now.getTimeInMillis();
    }
}
