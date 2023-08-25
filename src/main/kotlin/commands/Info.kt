package commands

import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.interactions.components.getOption
import dev.minn.jda.ktx.messages.EmbedBuilder
import dev.minn.jda.ktx.messages.InlineEmbed
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.utils.TimeFormat
import java.time.OffsetDateTime
import java.util.*

class Info : InteractiveCommand {
  override val data = Commands.slash("info", "Fetch user information")
    .addOption(OptionType.USER, "user", "User to fetch", true)

  override suspend fun interact(event: SlashCommandInteractionEvent) {
    val entity = if (event.guild != null) event.getOption<Member>("user") else event.getOption<User>("user")

    entity?.let {
      var eb: InlineEmbed? = null

      when (it) {
        is User -> { eb = generateEmbed(it.effectiveName, it.id, it.effectiveAvatarUrl, it.timeCreated, 0x232137) }
        is Member -> {
          val color = if (it.roles.isNotEmpty()) it.roles[0].colorRaw else 0x232137
          eb = generateEmbed(it.user.name, it.id, it.effectiveAvatarUrl, it.timeCreated, color)

          eb.field {
            name = "Join date"
            value = String.format(
              "%s (%s)",
              TimeFormat.DATE_TIME_LONG.atTimestamp(it.timeJoined.toEpochSecond() * 1000).toString(),
              TimeFormat.RELATIVE.atTimestamp(it.timeJoined.toEpochSecond() * 1000).toString()
            )
            inline = false
          }

          if (!it.nickname.isNullOrEmpty()) {
            eb.field {
              name = "Nickname"
              value = it.nickname!!
              inline = false
            }
          }

          if (it.roles.isNotEmpty()) {
            val roleList = StringJoiner("\n")
            it.roles.forEach { role -> roleList.add(role.asMention) }

            eb.field {
              name = "Roles"
              value = roleList.toString()
              inline = false
            }
          }
        }
      }

      event.hook.editOriginalEmbeds(eb?.build()).await()
    }
  }

  private fun generateEmbed(uname: String, id: String, avatar: String, dateCreated: OffsetDateTime, rawColor: Int) = EmbedBuilder {
    title = uname
    description = id
    thumbnail = avatar

    field {
      name = "Creation date"
      value = String.format(
        "%s (%s)",
        TimeFormat.DATE_TIME_LONG.atTimestamp(dateCreated.toEpochSecond() * 1000).toString(),
        TimeFormat.RELATIVE.atTimestamp(dateCreated.toEpochSecond() * 1000).toString()
      )
      inline = false
    }

    color = rawColor
  }
}