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
     * Inicia o agendamento das notificações de validade no horário definido.
     */
    public static void start(Context c) {
        // 🕒 DEFINA O HORÁRIO AQUI (Ex: 14:52)
        int hour = 15;
        int minute = 28;

        long initialDelay = calculateInitialDelay(hour, minute);
        
        // Garante que o canal de notificação esteja criado
        NotificationUtil.createChannel(c);

        Constraints constraints = new Constraints.Builder()
                .setRequiresBatteryNotLow(false)
                .build();

        // Agendamento Periódico (Diário)
        PeriodicWorkRequest periodicWork =
                new PeriodicWorkRequest.Builder(
                        ExpirationWorker.class,
                        1,
                        TimeUnit.DAYS
                )
                        .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                        .setConstraints(constraints)
                        .addTag(TAG)
                        .build();

        Log.d(TAG, "Agendando verificação para as " + hour + ":" + minute + 
              ". O worker rodará em " + (initialDelay / 60000) + " minutos.");

        // ✅ Usando UPDATE para que a mudança de horário seja aplicada imediatamente
        WorkManager.getInstance(c).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE, 
                periodicWork
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
     * Calcula o delay exato até o próximo horário definido.
     */
    private static long calculateInitialDelay(int hour, int minute) {
        Calendar now = Calendar.getInstance();
        Calendar nextRun = Calendar.getInstance();

        nextRun.set(Calendar.HOUR_OF_DAY, hour);
        nextRun.set(Calendar.MINUTE, minute);
        nextRun.set(Calendar.SECOND, 0);
        nextRun.set(Calendar.MILLISECOND, 0);

        // Se o horário já passou hoje, agenda para o mesmo horário amanhã
        if (nextRun.before(now)) {
            nextRun.add(Calendar.DAY_OF_MONTH, 1);
        }

        return nextRun.getTimeInMillis() - now.getTimeInMillis();
    }
}
