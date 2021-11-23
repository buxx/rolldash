package fr.bux.rollingdashboard

import android.content.Context
import androidx.lifecycle.lifecycleScope
import androidx.work.CoroutineWorker
import androidx.work.Worker
import androidx.work.WorkerParameters
import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.features.auth.*
import io.ktor.client.features.auth.providers.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.util.*

// FIXME : move the fun into utils file
fun Date.toString(format: String, locale: Locale = Locale.getDefault()): String {
    val formatter = SimpleDateFormat(format, locale)
    return formatter.format(this)
}

fun getCurrentDateTime(): Date {
    return Calendar.getInstance().time
}

fun getCurrentTimestamp(): Timestamp {
    return Timestamp(System.currentTimeMillis())
}

fun getSinceString(date1: Date, date2: Date): String {
    val diff: Long = date1.time - date2.time
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24
    println("$seconds ...")
    if (days > 0) {
        return "$days jour(s) et $hours heure(s)"
    }

    if (hours > 0) {
        return "$hours heure(s) et $minutes minute(s)"
    }

    if (minutes > 0) {
        return "$minutes minute(s)"
    }

    return "moins d'une minute"
}

class GrabCharacterWorker(appContext: Context, workerParams: WorkerParameters):
    CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        // FIXME : add a last date check because android execute multiple times this worker when
        // application was closed.
        // TODO : Afficher la date de dernier fetch pour verifier que le worker travaille
        // TODO : mettre un bouton refresh sur la home page

        // Do the work ...
        // See https://www.raywenderlich.com/6994782-android-networking-with-kotlin-tutorial-getting-started
        // https://kotlinlang.org/docs/kmm-use-ktor-for-networking.html#configure-the-client
        println("DEBUG PERIODIC EXECUTION")

        val database = RollingDashboardApplication.instance.database
        val accountConfiguration = database.accountConfigurationDao().get()
        if (accountConfiguration == null) {
            println("Grab character worker called but there is no account configuration !")
            return Result.success()
        }

        // Fake an updated Character
        val character = Character(
            id = "0000-0000-0000-0000",
            name = "Toto",
            action_points = 36.0.toFloat(),
            hungry = false,
            thirsty = false,
            last_refresh = getCurrentTimestamp().time
        )
        database.characterDao().clear()
        database.characterDao().insert(character)

        val dateStr = Date(character.last_refresh).toString("yyyy-MM-dd HH:mm:ss")
        println("date :: $dateStr")

//        val httpClient: HttpClient = HttpClient(Android)  {
//            install(Auth) {
//                basic {
//                    credentials {
//                        BasicAuthCredentials(username = "bux", password = "bux")
//                    }
//                }
//            }
//        }


        // FIXME : if work cant be done (network, etc)
        // see https://developer.android.com/topic/libraries/architecture/workmanager/basics#kts
        return Result.success()
    }
}
