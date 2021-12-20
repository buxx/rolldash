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
import java.lang.Exception


class GrabCharacterWorker(appContext: Context, workerParams: WorkerParameters):
    CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        // FIXME : manage case where character is not yet created
        // FIXME : manage case where character is dead
        println("WORKER :: DEBUG PERIODIC EXECUTION")
        val database = RollingDashboardApplication.instance.database

        var systemData = database.systemDataDao().get()
        if (systemData == null) {
            println("WORKER :: first work, donr't check last time")
            systemData = SystemData(last_try_refresh = getCurrentTimestamp().time, current_grab_error = null)
            database.systemDataDao().insert(systemData)
        } else {
            val lastTryRefresh = systemData.last_try_refresh
            systemData.last_try_refresh = getCurrentTimestamp().time
            try {
                database.systemDataDao().update(systemData)
            } catch (e: Exception) {
                println("WORKER :: Unexpected error when save system data: $e")
                return Result.failure()
            }

            val delta = getCurrentTimestamp().time - lastTryRefresh
            if (inputData.getBoolean("NOW", false)) {
                println("WORKER :: Force execution because NOW is true")
            } else {
                println("WORKER :: check last time : $delta < 30_000 ?")
                if (delta < 30_000) {
                    println("WORKER :: Skip grab character (last check minor than 30s)")
                    return Result.success()
                }
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
            database.systemDataDao().updateCurrentGrabError("Erreur de récupération : $e")
            return Result.failure()
        }

        when (accountCharacterResponse.status) {
            HttpStatusCode.Forbidden -> {
                println("WORKER :: Fail to authenticate !")
                database.systemDataDao().updateCurrentGrabError("Erreur d'authentification")
                return Result.failure()
            }
            HttpStatusCode.OK -> {
                // OK
            }
            else -> {
                val statusCode = accountCharacterResponse.status
                println("WORKER :: Unexpected return status code $statusCode !")
                database.systemDataDao().updateCurrentGrabError("Erreur de récupération : Code serveur $statusCode")
                return Result.failure()
            }
        }

        val characterId: String = accountCharacterResponse.readText();
        if (characterId == "") {
            println("WORKER :: Account have no character, abort")
            database.systemDataDao().updateCurrentGrabError("Erreur de récupération : Pas encore de personnage !")
            return Result.success()
        }

        println("WORKER :: Found character id $characterId")
        val characterUrl = "$serverUrl/character/$characterId"
        println("WORKER :: Make request $accountCharacterUrl")
        val characterInfoResponse: HttpResponse =  try {
            httpClient.get(characterUrl)
        } catch (e: Throwable) {
            println("WORKER :: Unexpected error ! $e")
            database.systemDataDao().updateCurrentGrabError("Erreur de récupération : $e")
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

            println("WORKER :: avatar ? ${previousCharacter.avatar_uuid} != ${characterInfo.avatar_uuid} ?")
            if (characterInfo.avatar_uuid != null) {
                val applicationDir = applicationContext.applicationInfo.dataDir
                val avatarFileName = "$applicationDir/avatar.png"
                val avatarFile = File(avatarFileName)
                if (previousCharacter.avatar_uuid != characterInfo.avatar_uuid || !avatarFile.exists()) {
                    val avatarUrl = "$serverUrl/media/character_avatar__original__${characterInfo.avatar_uuid}.png"
                    println("WORKER :: Make avatar request $avatarUrl")
                    val avatarResponse: HttpResponse = try {
                        httpClient.get(avatarUrl)
                    } catch (e: Throwable) {
                        println("WORKER :: Unexpected error ! $e")
                        return Result.failure()
                    }
                    val avatarResponseBody: ByteArray = avatarResponse.receive()
                    println("WORKER :: Write avatar file into $avatarFileName")
                    avatarFile.createNewFile()
                    avatarFile.writeBytes(avatarResponseBody)
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
        database.systemDataDao().updateCurrentGrabError(null)

        return Result.success()
    }
}
