package com.rogger.bipando.notification;

import android.content.Context;

import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

public class NotificationScheduler {

    public static void start(Context c) {
        int hour = 23;
        int minute = 44;

        long initialDelay = calculateInitialDelay(hour, minute);
        NotificationUtil.createChannel(c);
        Constraints constraints = new Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .build();

        PeriodicWorkRequest work =
                new PeriodicWorkRequest.Builder(
                        ExpirationWorker.class,
                        1,
                        TimeUnit.DAYS
                )
                        .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                        .setConstraints(constraints)
                        .build();

        WorkManager.getInstance(c).enqueueUniquePeriodicWork(
                "expiration_worker",
                ExistingPeriodicWorkPolicy.UPDATE,
                work
        );
    }

    public static void stop(Context c) {
        WorkManager.getInstance(c)
                .cancelUniqueWork("expiration_worker");
    }

    /**
     * Calcula quanto tempo falta até a próxima execução no horário escolhido
     */
    private static long calculateInitialDelay(int hour, int minute) {
        Calendar now = Calendar.getInstance();
        Calendar nextRun = Calendar.getInstance();

        nextRun.set(Calendar.HOUR_OF_DAY, hour);
        nextRun.set(Calendar.MINUTE, minute);
        nextRun.set(Calendar.SECOND, 0);

        if (nextRun.before(now)) {
            nextRun.add(Calendar.DAY_OF_MONTH, 1);
        }

        return nextRun.getTimeInMillis() - now.getTimeInMillis();
    }
}

