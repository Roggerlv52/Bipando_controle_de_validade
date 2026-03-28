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
     * Agenda (ou reagenda) o worker diário no horário definido pelo usuário.
     *
     * Deve ser chamado:
     *  - ao ativar o switch de notificações
     *  - ao alterar o horário no TimePicker
     *  - ao alterar os dias de alerta no slider     ← BUG 1 corrigido
     *  - no MainActivity.onCreate() se notificações estiverem ativas
     *
     * Usa CANCEL_AND_REENQUEUE para garantir que o novo initialDelay
     * seja respeitado imediatamente, sem esperar o ciclo anterior acabar.
     * Isso corrige o drift de horário (Bug 2).
     */
    public static void start(Context c) {
        // Garante que o canal de notificação existe ANTES de agendar
        // (Bug 5 corrigido: canal criado aqui e na Application)
        NotificationUtil.createChannel(c);

        int hour   = NotificationPrefs.getHour(c);
        int minute = NotificationPrefs.getMinute(c);

        long initialDelay = calculateInitialDelay(hour, minute);

        Constraints constraints = new Constraints.Builder()
                .setRequiresBatteryNotLow(false)
                .build();

        PeriodicWorkRequest periodicWork = new PeriodicWorkRequest.Builder(
                ExpirationWorker.class,
                24, TimeUnit.HOURS          // intervalo diário
        )
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .setConstraints(constraints)
                .addTag(TAG)
                .build();

        long minutesUntilRun = initialDelay / 60_000;
        Log.d(TAG, "Agendando para " + hour + ":" + String.format("%02d", minute)
                + ". Próximo disparo em " + minutesUntilRun + " minuto(s).");

        // CANCEL_AND_REENQUEUE garante que o initialDelay calculado acima
        // seja aplicado imediatamente, corrigindo o drift de horário (Bug 2/3).
        WorkManager.getInstance(c).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
                periodicWork
        );
    }

    /**
     * Cancela o agendamento de notificações.
     */
    public static void stop(Context c) {
        Log.d(TAG, "Cancelando agendamento de notificações.");
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

        // Margem de segurança de 5 segundos
        return delayMs + 5_000;
    }
}