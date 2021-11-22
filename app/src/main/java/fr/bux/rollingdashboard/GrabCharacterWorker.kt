package fr.bux.rollingdashboard

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

class GrabCharacterWorker(appContext: Context, workerParams: WorkerParameters):
    Worker(appContext, workerParams) {
    override fun doWork(): Result {

        // Do the work ...
        // See https://www.raywenderlich.com/6994782-android-networking-with-kotlin-tutorial-getting-started
        // https://kotlinlang.org/docs/kmm-use-ktor-for-networking.html#configure-the-client
        println("DEBUG PERIODIC EXECUTION")

        // FIXME : if work cant be done (network, etc)
        // see https://developer.android.com/topic/libraries/architecture/workmanager/basics#kts
        return Result.success()
    }
}
