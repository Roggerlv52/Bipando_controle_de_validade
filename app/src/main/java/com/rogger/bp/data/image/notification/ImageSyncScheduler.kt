package com.rogger.bp.data.image.notification

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager

/*
 * Desenvolvido por Roger de Oliveira
 * Data: 04/06/2026
 * Hora: 12:34
 */
object ImageSyncScheduler {
    fun start(context: Context) {
        // Define as restrições: exige internet ativa
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncWorkRequest = OneTimeWorkRequest.Builder(ImageSyncWorker::class.java)
            .setConstraints(constraints)
            .build()

        // Executa de forma única: se já houver uma tarefa idêntica na fila, mantém a atual
        WorkManager.getInstance(context).enqueueUniqueWork(
            "image_offline_sync",
            ExistingWorkPolicy.KEEP,
            syncWorkRequest
        )
    }
}