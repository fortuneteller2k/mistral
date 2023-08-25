package commands

import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.messages.EmbedBuilder
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.build.Commands

class Ping : InteractiveCommand {
  override val data = Commands.slash("ping", "WebSocket/REST API latency")

  override suspend fun interact(event: SlashCommandInteractionEvent) {
    val rest = event.jda.restPing.await()
    val ws = event.jda.gatewayPing

    val eb = EmbedBuilder {
      field {
        name = "WebSocket"
        value = String.format("%d ms", ws)
        inline = false
      }

      field {
        name = "REST"
        value = String.format("%d ms", rest)
        inline = false
      }
    }

    event.hook.editOriginalEmbeds(eb.build()).await()
  }
}
