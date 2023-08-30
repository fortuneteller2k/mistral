
import commands.Info
import commands.Math
import commands.Ping
import commands.Tag
import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.events.listener
import dev.minn.jda.ktx.jdabuilder.intents
import dev.minn.jda.ktx.jdabuilder.light
import kotlinx.coroutines.runBlocking
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.requests.GatewayIntent
import org.jetbrains.exposed.sql.Database
import org.slf4j.LoggerFactory
import kotlin.io.path.Path
import kotlin.io.path.createParentDirectories

suspend fun main(): Unit = runBlocking {
  val log = LoggerFactory.getLogger(this::class.java)
  val token = System.getenv("MTL_TOKEN")
  val dbPath = System.getenv("MTL_DB")

  Path(dbPath).createParentDirectories()

  log.info("Starting SQLite database at $dbPath")
  Database.connect("jdbc:sqlite:$dbPath", "org.sqlite.JDBC", "moni")

  light(token, enableCoroutines = true) {
    intents += listOf(GatewayIntent.GUILD_MEMBERS)

    setBulkDeleteSplittingEnabled(false)
    setActivity(Activity.watching("you"))
  }.also {
    it.initialize()
  }
}

suspend fun JDA.initialize() {
  val log = LoggerFactory.getLogger(this::class.java)
  val commands = listOf(Math(), Info(), Ping(), Tag())

  commands.forEach { upsertCommand(it.data).await() }

  listener<SlashCommandInteractionEvent> {
    log.info("${it.name} ${it.subcommandName}")
    if (it.isAcknowledged) return@listener
    it.deferReply().await()

    commands.forEach { command ->
      if (it.name != command.data.name) return@forEach

      try {
        command.interact(it)
      } catch (ex: RuntimeException) {
        val msg = String.format("naay something: %s", ex.toString())
        it.hook.editOriginal(msg).await()
      }
    }
  }
}