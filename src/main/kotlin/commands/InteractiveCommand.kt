package commands

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData

interface InteractiveCommand {
  val data: SlashCommandData

  suspend fun interact(event: SlashCommandInteractionEvent)
}