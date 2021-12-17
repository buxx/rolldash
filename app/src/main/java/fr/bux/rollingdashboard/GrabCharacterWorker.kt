package fr.bux.rollingdashboard

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.android.*
import io.ktor.client.features.auth.*
import io.ktor.client.features.auth.providers.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.*
import java.io.File


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
        println("WORKER :: DEBUG PERIODIC EXECUTION")
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
                println("WORKER :: Skip grab character (last check minor than 30s)")
                return Result.success()
            }
        }

        if (!isInternetAvailable(applicationContext)) {
            println("WORKER :: Grab character worker called but there is no internet access !")
            return Result.failure()
        }

        val accountConfiguration = database.accountConfigurationDao().get()
        if (accountConfiguration == null) {
            println("WORKER :: Grab character worker called but there is no account configuration !")
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
        println("WORKER :: Make request $accountCharacterUrl")
        val accountCharacterResponse: HttpResponse = try {
            httpClient.get(accountCharacterUrl)
        } catch (e: Throwable) {
            println("WORKER :: Unexpected error ! $e")
            return Result.failure()
        }

        when (accountCharacterResponse.status) {
            HttpStatusCode.Forbidden -> {
                // FIXME UI must display this problem
                println("WORKER :: Fail to authenticate !")
                return Result.failure()
            }
            HttpStatusCode.OK -> {
                // OK
            }
            else -> {
                // FIXME UI must display this problem
                val statusCode = accountCharacterResponse.status
                println("WORKER :: Unexpected return status code $statusCode !")
                return Result.failure()
            }
        }

        val characterId: String = accountCharacterResponse.readText();
        if (characterId == "") {
            // FIXME UI must display this problem
            println("WORKER :: Account have no character, abort")
            return Result.success()
        }

        println("WORKER :: Found character id $characterId")
        val characterUrl = "$serverUrl/character/$characterId"
        println("WORKER :: Make request $accountCharacterUrl")
        val characterInfoResponse: HttpResponse =  try {
            httpClient.get(characterUrl)
        } catch (e: Throwable) {
            println("WORKER :: Unexpected error ! $e")
            return Result.failure()
        }
        val characterInfoResponseJsonString = characterInfoResponse.readText()
        val characterInfo: CharacterInfo = kotlinx.serialization.json.Json {
            ignoreUnknownKeys = true
        }.decodeFromString<CharacterInfo>(characterInfoResponseJsonString)

        val previousCharacter = database.characterDao().get()
        val updatedCharacter = Character(
            id = characterId,
            name = characterInfo.name,
            action_points = characterInfo.action_points,
            hungry = characterInfo.is_hunger,
            thirsty = characterInfo.is_thirsty,
            tired = characterInfo.is_tired,
            exhausted = characterInfo.is_exhausted,
            last_refresh = getCurrentTimestamp().time,
            avatar_uuid = characterInfo.avatar_uuid,
        )

        if (previousCharacter != null) {
            val isNowHungry = !previousCharacter.hungry && characterInfo.is_hunger
            val isNowThirsty = !previousCharacter.thirsty && characterInfo.is_thirsty
            val isNowMaxAp = (
                previousCharacter.action_points != characterInfo.action_points
                && characterInfo.action_points == characterInfo.max_action_points
            )

            if (previousCharacter.avatar_uuid != characterInfo.avatar_uuid) {
                if (characterInfo.avatar_uuid != null) {
                    val avatarUrl = "$serverUrl/media/character_avatar__original__${characterInfo.avatar_uuid}.png"
                    println("WORKER :: Make avatar request $avatarUrl")
                    val avatarResponse: HttpResponse = try {
                        httpClient.get(avatarUrl)
                    } catch (e: Throwable) {
                        println("WORKER :: Unexpected error ! $e")
                        return Result.failure()
                    }
                    val avatarResponseBody: ByteArray = avatarResponse.receive()
                    val applicationDir = applicationContext.applicationInfo.dataDir
                    val fileName = "$applicationDir/avatar.png"
                    val file = File(fileName)
                    file.createNewFile()
                    file.writeBytes(avatarResponseBody)
                }
            }

            if (isNowHungry || isNowThirsty || isNowMaxAp) {
                var notificationText = ""
                if (isNowHungry) {
                    println("WORKER :: Character is now HUNGRY")
                    notificationText += " Faim!"
                }
                if (isNowThirsty) {
                    println("WORKER :: Character is now THIRSTY")
                    notificationText += " Soif!"
                }
                if (isNowMaxAp) {
                    println("WORKER :: Character is now MAX AP")
                    notificationText += " MaxAP!"
                }

                val notificationManager = applicationContext.getSystemService(NOTIFICATION_SERVICE) as NotificationManager

                val intent = Intent(applicationContext, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                val pendingIntent: PendingIntent = PendingIntent.getActivity(applicationContext, 0, intent, 0)
                val builder = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                    .setSmallIcon(R.drawable.rolling_dashboard_notification_icon)
                    .setContentTitle(characterInfo.name)
                    .setContentText(notificationText)
                    .setContentIntent(pendingIntent)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)

                notificationManager.notify(NOTIFICATION_CHARACTER_ID, builder.build())

            } else {
                println("WORKER :: no changes")
            }
        }

        println("WORKER :: Update database with character")
        database.characterDao().clear()
        database.characterDao().insert(updatedCharacter)

        // FIXME : if work cant be done (network, etc)
        // see https://developer.android.com/topic/libraries/architecture/workmanager/basics#kts
        return Result.success()
    }
}
