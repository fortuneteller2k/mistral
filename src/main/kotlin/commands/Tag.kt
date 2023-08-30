package commands

import com.jagrosh.jagtag.JagTag
import com.jagrosh.jagtag.Method
import commands.TagTable.content
import commands.TagTable.guildId
import commands.TagTable.ident
import commands.TagTable.ownerId
import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.interactions.components.getOption
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.security.SecureRandom
import java.util.*

typealias TagGuild = Long
typealias TagOwner = Long
typealias TagIdent = String
typealias TagContent = String

object TagTable : Table() {
  val ident = varchar("tag_ident", 20).check { it.isNotNull() }
  val content = varchar("tag_content", 4000).check { it.isNotNull() }
  val ownerId = long("owner_id").check { it.isNotNull() }
  val guildId = long("guild_id").check { it.isNotNull() }
}
class Tag : InteractiveCommand {
  private val tags: MutableMap<Pair<TagGuild, TagIdent>, Pair<TagOwner, TagContent>> = mutableMapOf()

  override val data = Commands.slash("tag", "Content tagging").addSubcommands(
    SubcommandData("get", "Retrieve tag contents")
      .addOption(OptionType.STRING, "name", "Tag name", true),

    SubcommandData("set", "Create/edit tag contents")
      .addOption(OptionType.STRING, "name", "Tag name", true)
      .addOption(OptionType.STRING, "content", "Tag contents", true),

    SubcommandData("list", "Retrieve tags for server")
  )

  init {
    transaction {
      SchemaUtils.createMissingTablesAndColumns(TagTable)
      TagTable.selectAll().forEach { tag -> tags[tag[guildId] to tag[ident]] = tag[ownerId] to tag[content] }
    }
  }

  override suspend fun interact(event: SlashCommandInteractionEvent) {
    if (event.guild == null) {
      event.hook.editOriginal("Tags are only available on servers.").await()
      return
    }

    val guildIdLong = event.guild!!.idLong
    val userIdLong = event.user.idLong
    val name = event.getOption<String>("name") ?: ""
    val members = event.guild!!.loadMembers().await()

    val parser = JagTag.newDefaultBuilder().apply {
      addMethods(arrayListOf(
        Method("author") { _ -> event.user.name },
        Method("m_author") { _ -> event.user.asMention },
        Method("guild") { _ -> event.guild!!.name },
        Method("rand_member") { _ -> members[SecureRandom().nextInt(members.size)].effectiveName },
      ))
    }.build()

    when (event.subcommandName) {
      "get" -> if (guildIdLong to name in tags) {
        val (_, content) = tags[guildIdLong to name]!!
        event.hook.editOriginal(parser.parse(content)).await()
      } else {
        event.hook.editOriginal("No tag named `$name`.").await()
      }

      "set" -> {
        val value = event.getOption<String>("content")!!

        if (guildIdLong to name in tags) {
          val (owner, _) = tags[guildIdLong to name]!!

          if (owner != userIdLong) {
            event.hook.editOriginal("You don't own this tag.").await()
          } else {
            tags[guildIdLong to name] = userIdLong to value

            transaction {
              TagTable.update({ ownerId.eq(userIdLong) and ident.eq(name)}) { it[content] = value }
            }

            event.hook.editOriginal("Tag updated.").await()
          }
        } else {
          tags[guildIdLong to name] = userIdLong to value

          transaction {
            TagTable.insert {
              it[guildId] = guildIdLong
              it[ownerId] = userIdLong
              it[ident] = name
              it[content] = value
            }
          }

          event.hook.editOriginal("Tag created.").await()
        }
      }

      "list" -> {
        if (tags.isEmpty()) {
          event.hook.editOriginal("No tags available for this server.").await()
          return
        }

        val msg = StringJoiner(", ").apply {
          for ((k, v) in tags) {
            val (guild, tag) = k
            val (owner, _) = v

            val user = event.jda.retrieveUserById(owner).await().effectiveName
            if (guildIdLong == guild) add("`$tag` by $user")
          }
        }.toString()

        event.hook.editOriginal(msg).await()
      }
    }
  }
}