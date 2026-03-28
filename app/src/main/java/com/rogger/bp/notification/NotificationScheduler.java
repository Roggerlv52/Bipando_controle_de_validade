package com.rogger.bp.notification;

import android.content.Context;
import android.util.Log;

import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

public class NotificationScheduler {
    private static final String TAG = "NotificationScheduler";
    private static final String WORK_NAME = "expiration_worker";
    private static final String ONE_TIME_WORK_NAME = "expiration_worker_immediate";

    /**
     * Inicia o agendamento das notificações de validade.
     */
    public static void start(Context c) {
        // Horário padrão para execução (pode ser configurado via UI no futuro)
        int hour = 9;
        int minute = 0;

        long initialDelay = calculateInitialDelay(hour, minute);
        
        // Garante que o canal de notificação esteja criado
        NotificationUtil.createChannel(c);

        Constraints constraints = new Constraints.Builder()
                .setRequiresBatteryNotLow(false)
                .build();

        // 1. Agendamento Periódico (Diário)
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

        Log.d(TAG, "Agendando worker periódico para rodar em " + (initialDelay / 60000) + " minutos.");

        // ✅ Usando UPDATE para que mudanças no código/configuração reflitam imediatamente
        WorkManager.getInstance(c).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE, 
                periodicWork
        );

        // 2. Agendamento Imediato (Apenas para testes ou primeira execução)
        // Isso ajuda a ver se a lógica está funcionando sem esperar o delay do sistema
        OneTimeWorkRequest immediateWork = new OneTimeWorkRequest.Builder(ExpirationWorker.class)
                .setConstraints(constraints)
                .build();
        
        WorkManager.getInstance(c).enqueueUniqueWork(
                ONE_TIME_WORK_NAME,
                ExistingWorkPolicy.KEEP, // Só roda se não houver um imediato pendente
                immediateWork
        );
    }

    /**
     * Para o agendamento das notificações.
     */
    public static void stop(Context c) {
        Log.d(TAG, "Cancelando agendamento de notificações.");
        WorkManager.getInstance(c).cancelUniqueWork(WORK_NAME);
        WorkManager.getInstance(c).cancelUniqueWork(ONE_TIME_WORK_NAME);
    }

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
