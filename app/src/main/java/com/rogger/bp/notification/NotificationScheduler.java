package com.rogger.bp.notification;

import android.content.Context;

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
     * Agenda (ou reagenda) o worker diário no horário definido pelo usuário.
     * @param forceReschedule Se true, cancela o anterior e agenda um novo (usado ao mudar configurações).
     *                        Se false, mantém o agendamento atual se já existir (usado no boot/abertura do app).
     */
    public static void start(Context c, boolean forceReschedule) {
        if (!NotificationPrefs.getAlert(c)) {
            stop(c);
            return;
        }

        NotificationUtil.createChannel(c);

        int hour   = NotificationPrefs.getHour(c);
        int minute = NotificationPrefs.getMinute(c);

        long initialDelay = calculateInitialDelay(hour, minute);

        Constraints constraints = new Constraints.Builder()
                .setRequiresBatteryNotLow(false)
                .build();

        PeriodicWorkRequest periodicWork = new PeriodicWorkRequest.Builder(
                ExpirationWork.class,
                24, TimeUnit.HOURS
        )
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .setConstraints(constraints)
                .addTag(TAG)
                .build();

        // Se forceReschedule for falso, usamos KEEP para não resetar o delay inicial desnecessariamente
        ExistingPeriodicWorkPolicy policy = forceReschedule 
                ? ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE 
                : ExistingPeriodicWorkPolicy.KEEP;

        WorkManager.getInstance(c).enqueueUniquePeriodicWork(
                WORK_NAME,
                policy,
                periodicWork
        );
    }

    // Sobrecarga para compatibilidade
    public static void start(Context c) {
        start(c, false);
    }

    /**
     * Cancela o agendamento de notificações.
     */
    public static void stop(Context c) {
        WorkManager.getInstance(c).cancelUniqueWork(WORK_NAME);
    }

    /**
     * Calcula o delay em ms até o próximo disparo no horário configurado.
     * Se o horário já passou hoje, agenda para o mesmo horário amanhã.
     * Adiciona 5s de margem para evitar edge cases de arredondamento.
     */
    public static long calculateInitialDelay(int hour, int minute) {
        Calendar now     = Calendar.getInstance();
        Calendar nextRun = Calendar.getInstance();

        nextRun.set(Calendar.HOUR_OF_DAY, hour);
        nextRun.set(Calendar.MINUTE,      minute);
        nextRun.set(Calendar.SECOND,      0);
        nextRun.set(Calendar.MILLISECOND, 0);

        // Se o horário já passou hoje, empurra para amanhã
        if (!nextRun.after(now)) {
            nextRun.add(Calendar.DAY_OF_MONTH, 1);
        }

        long delayMs = nextRun.getTimeInMillis() - now.getTimeInMillis();

        // Removida a margem de 5s para ser mais pontual
        return Math.max(0, delayMs);
    }
}