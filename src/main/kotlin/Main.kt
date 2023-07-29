import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Paths
import kotlinx.datetime.Clock
import kotlinx.datetime.toJavaInstant
import org.slf4j.LoggerFactory
import java.io.PrintStream
import java.time.format.DateTimeFormatterBuilder

val logger: org.slf4j.Logger = LoggerFactory.getLogger("Bot")

fun main() = application {
    setupLogging()

    Window(
        state = WindowState(size = DpSize(350.dp, 150.dp)),
        title = "TwitchLiveNotificationBot",
        onCloseRequest = ::exitApplication,
        resizable = false
    ) {
        App()
    }
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