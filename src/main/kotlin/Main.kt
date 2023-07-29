import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import com.github.philippheuer.credentialmanager.domain.OAuth2Credential
import com.github.twitch4j.TwitchClientBuilder
import com.github.twitch4j.events.ChannelGoLiveEvent
import dev.kord.core.Kord
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.supplier.EntitySupplyStrategy
import dev.kord.gateway.Intent
import dev.kord.gateway.PrivilegedIntent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Paths
import kotlinx.datetime.Clock
import kotlinx.datetime.toJavaInstant
import org.slf4j.LoggerFactory
import java.io.PrintStream
import java.io.PrintWriter
import java.io.StringWriter
import java.time.format.DateTimeFormatterBuilder
import javax.swing.JOptionPane
import kotlin.system.exitProcess

val logger: org.slf4j.Logger = LoggerFactory.getLogger("Bot")

suspend fun main() = try {
    setupLogging()

    val discordClient = Kord(DiscordBotConfig.discordToken)
    logger.info("Discord client started.")

    CoroutineScope(discordClient.coroutineContext).launch {
        discordClient.login {
            @OptIn(PrivilegedIntent::class)
            intents += Intent.MessageContent
        }
    }

    application {
        setupTwitchBot(discordClient)
        Window(
            state = WindowState(size = DpSize(350.dp, 150.dp)),
            title = "TwitchLiveNotificationBot",
            onCloseRequest = ::exitApplication,
            resizable = false
        ) {
            App()
        }
    }
} catch (e: Throwable) {
    JOptionPane.showMessageDialog(
        null,
        e.message + "\n" + StringWriter().also { e.printStackTrace(PrintWriter(it)) },
        "InfoBox: File Debugger",
        JOptionPane.INFORMATION_MESSAGE
    )
    logger.error("Error while executing program.", e)
    exitProcess(0)
}

fun setupTwitchBot(discordClient: Kord) {
    val twitchClient = TwitchClientBuilder.builder()
        .withEnableHelix(true)
        .withEnableChat(true)
        .withDefaultAuthToken(OAuth2Credential("twitch", TwitchBotConfig.chatAccountToken))
        .build()

    for(channel in TwitchBotConfig.channels) {
        twitchClient.chat.run {
            connect()
            joinChannel(channel)
            logger.info("Connected to channel $channel")
        }
        twitchClient.clientHelper.enableStreamEventListener(channel)
    }


    twitchClient.eventManager.onEvent(ChannelGoLiveEvent::class.java) { goLiveEvent ->
        CoroutineScope(Dispatchers.IO).launch {
            sendMessageToLiveNotificationChannel(goLiveEvent, discordClient)
        }
    }
}

suspend fun sendMessageToLiveNotificationChannel(goLiveEvent: ChannelGoLiveEvent, discordClient: Kord) {
    val discordChannel = discordClient.getChannelOf<TextChannel>(DiscordBotConfig.liveNotificationChannelId, EntitySupplyStrategy.cacheWithCachingRestFallback)
        ?: error("Invalid channel ID.")

    val discordChannelName = discordChannel.name
    val discordChannelId = discordChannel.id

    logger.info("User name for live notification: ${goLiveEvent.channel.name} | Channel Name: $discordChannelName | Channel ID: $discordChannelId")

    discordChannel.createMessage(
        "User \"${goLiveEvent.channel.name}\" just went live with title \"${goLiveEvent.stream.title}\"" +
                if(goLiveEvent.stream.gameName.isNotEmpty()) {
                    " and game \"${goLiveEvent.stream.gameName}\""
                } else {
                    ""
                } +
                " on\n" +
                "\nhttps://www.twitch.tv/${goLiveEvent.channel.name}"
    )

    logger.info("Message created on Discord Channel $discordChannelName")
}

// Logging
private const val LOG_DIRECTORY = "logs"

fun setupLogging() {
    Files.createDirectories(Paths.get(LOG_DIRECTORY))

    val logFileName = DateTimeFormatterBuilder()
        .appendInstant(0)
        .toFormatter()
        .format(Clock.System.now().toJavaInstant())
        .replace(':', '-')

    val logFile = Paths.get(LOG_DIRECTORY, "${logFileName}.log").toFile().also {
        if (!it.exists()) {
            it.createNewFile()
        }
    }

    System.setOut(PrintStream(MultiOutputStream(System.out, FileOutputStream(logFile))))

    logger.info("Log file '${logFile.name}' has been created.")
}