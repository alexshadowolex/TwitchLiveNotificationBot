import dev.kord.common.entity.Snowflake
import java.io.File
import java.util.*

object DiscordBotConfig {
    private val properties = Properties().apply {
        load(File("data\\discordBotConfig.properties").inputStream())
    }

    val discordToken = File("data\\discordToken.txt").readText()
    val liveNotificationChannelId = Snowflake(properties.getProperty("live_notification_channel_id").toLong())
}