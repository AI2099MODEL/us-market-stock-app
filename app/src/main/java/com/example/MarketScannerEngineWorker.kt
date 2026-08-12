package com.example

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MarketScannerEngineWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            MarketEngine.runEngineCycle(applicationContext)
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext Result.retry()
        }
        Result.success()
    }
}
