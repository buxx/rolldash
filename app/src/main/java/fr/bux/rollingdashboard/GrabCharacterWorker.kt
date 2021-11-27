package fr.bux.rollingdashboard

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.features.auth.*
import io.ktor.client.features.auth.providers.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.serialization.*


class GrabCharacterWorker(appContext: Context, workerParams: WorkerParameters):
    CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        // FIXME : add a last date check (failure or not !!) because android execute multiple times this worker when
        // application was closed.
        // TODO : Afficher la date de dernier fetch pour verifier que le worker travaille
        // TODO : mettre un bouton refresh sur la home page
        // FIXME : manage case where character is not yet created
        // FIXME : manage case where character is dead

        // Do the work ...
        // See https://www.raywenderlich.com/6994782-android-networking-with-kotlin-tutorial-getting-started
        // https://kotlinlang.org/docs/kmm-use-ktor-for-networking.html#configure-the-client
        println("DEBUG PERIODIC EXECUTION")
        val database = RollingDashboardApplication.instance.database

        var systemData = database.systemDataDao().get()
        if (systemData == null) {
            systemData = SystemData(last_try_refresh = getCurrentTimestamp().time)
            database.systemDataDao().insert(systemData)
        } else {
            val lastTryRefresh = systemData.last_try_refresh
            systemData.last_try_refresh = getCurrentTimestamp().time
            database.systemDataDao().clear()
            database.systemDataDao().insert(systemData)

            if (lastTryRefresh < 30_000) {
                println("Skip grab character (last check minor than 30s)")
                return Result.success()
            }
        }

        if (!isInternetAvailable(applicationContext)) {
            println("Grab character worker called but there is no internet access !")
            return Result.failure()
        }

        val accountConfiguration = database.accountConfigurationDao().get()
        if (accountConfiguration == null) {
            println("Grab character worker called but there is no account configuration !")
            return Result.success()
        }

        // Grab over api
        val httpClient: HttpClient = HttpClient(Android)  {
            expectSuccess = false
            install(JsonFeature) {
                KotlinxSerializer(
                    kotlinx.serialization.json.Json {
                        prettyPrint = true
                        ignoreUnknownKeys = true
                    }
                )
            }
            install(Auth) {
                basic {
                    credentials {
                        BasicAuthCredentials(
                            username = accountConfiguration.user_name,
                            password = accountConfiguration.password,
                        )
                    }
                }
            }
        }
        var serverUrl: String = if (
            accountConfiguration.server_address.startsWith("https://")
            // TODO : Support non SSL for local servers
            || accountConfiguration.server_address.startsWith("http://")
        ) {
            accountConfiguration.server_address
        } else {
            val serverAddress = accountConfiguration.server_address
            "https://$serverAddress"
        }
        serverUrl = serverUrl.trimEnd('/')
        val accountCharacterUrl = "$serverUrl/account/current_character_id"
        println("Make request $accountCharacterUrl")
        val accountCharacterResponse: HttpResponse = try {
            httpClient.get(accountCharacterUrl)
        } catch (e: Throwable) {
            println("Unexpected error ! $e")
            return Result.failure()
        }

        when (accountCharacterResponse.status) {
            HttpStatusCode.Forbidden -> {
                // FIXME UI must display this problem
                println("Fail to authenticate !")
                return Result.failure()
            }
            HttpStatusCode.OK -> {
                println("OK")
                // OK
            }
            else -> {
                // FIXME UI must display this problem
                val statusCode = accountCharacterResponse.status
                println("Unexpected return status code $statusCode !")
                return Result.failure()
            }
        }

        val characterId: String = accountCharacterResponse.readText();
        if (characterId == "") {
            // FIXME UI must display this problem
            println("Account have no character, abort")
            return Result.success()
        }

        println("Found character id $characterId")

        val characterUrl = "$serverUrl/character/$characterId"
        println("Make request $accountCharacterUrl")

        val characterInfoResponse: HttpResponse =  try {
            httpClient.get(characterUrl)
        } catch (e: Throwable) {
            println("Unexpected error ! $e")
            return Result.failure()
        }
        val characterInfoResponseJsonString = characterInfoResponse.readText()
        val characterInfo: CharacterInfo = kotlinx.serialization.json.Json {
            ignoreUnknownKeys = true
        }.decodeFromString<CharacterInfo>(characterInfoResponseJsonString)

        println(characterInfo)

        // Fake an updated Character
        val character = Character(
            id = characterId,
            name = characterInfo.name,
            action_points = characterInfo.action_points,
            hungry = characterInfo.is_hunger,
            thirsty = characterInfo.is_thirsty,
            tired = characterInfo.is_tired,
            exhausted = characterInfo.is_exhausted,
            last_refresh = getCurrentTimestamp().time
        )
        database.characterDao().clear()
        database.characterDao().insert(character)

        // FIXME : if work cant be done (network, etc)
        // see https://developer.android.com/topic/libraries/architecture/workmanager/basics#kts
        return Result.success()
    }
}
