import java.io.File
import java.util.*

object TwitchBotConfig {
    private val properties = Properties().apply {
        load(File("data\\twitchBotConfig.properties").inputStream())
    }

    val chatAccountToken = File("data\\twitchToken.txt").readText()
    val channels: List<String> = properties.getProperty("channels").split(",")
}