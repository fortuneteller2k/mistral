
import commands.Info
import commands.Math
import commands.Ping
import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.events.listener
import dev.minn.jda.ktx.jdabuilder.light
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.session.ReadyEvent
import org.slf4j.LoggerFactory

suspend fun main(args: Array<String>) {
  light(args[0], enableCoroutines = true) {
    setBulkDeleteSplittingEnabled(false)
  }.also {
    it.initialize()
  }
}

suspend fun JDA.initialize() {
  val log = LoggerFactory.getLogger(this::class.java)
  val commands = listOf(Math(), Info(), Ping())

  commands.forEach { upsertCommand(it.data).await() }

  listener<ReadyEvent> {
    log.info("Ready.")
  }

  listener<SlashCommandInteractionEvent> {
    if (it.isAcknowledged) return@listener
    it.deferReply().await()

    commands.forEach { command ->
      if (it.name != command.data.name) return@forEach

      try {
        command.interact(it)
      } catch (ex: RuntimeException) {
        val msg = String.format("naay something: %s", ex.message)
        it.hook.editOriginal(msg).await()
      }
    }
  }
}