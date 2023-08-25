package commands

import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.interactions.components.getOption
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.utils.FileUpload
import org.scilab.forge.jlatexmath.TeXConstants
import org.scilab.forge.jlatexmath.TeXFormula
import java.awt.Color
import java.awt.Insets
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import javax.swing.JLabel

class Math : InteractiveCommand {
  override val data = Commands.slash("math", "Render LaTeX expressions")
    .addOption(OptionType.STRING, "expression", "Expression to render", true)

  override suspend fun interact(event: SlashCommandInteractionEvent) {
    TeXFormula.setDPITarget(200F)

    event.getOption<String>("expression")?.let { expr ->
      val icon = TeXFormula(expr).createTeXIcon(TeXConstants.STYLE_DISPLAY, 200F).apply {
        insets = Insets(10, 10, 10 ,10)
      }

      val image = BufferedImage(icon.iconWidth, icon.iconHeight, BufferedImage.TYPE_INT_ARGB)

      val graphics = image.createGraphics().apply {
        color = Color(0x313338)
        fillRect(0, 0, icon.iconWidth, icon.iconHeight)
      }

      icon.paintIcon(JLabel().apply { foreground = Color.white }, graphics, 0, 0)

      val imageData = ByteArrayOutputStream()

      ImageIO.createImageOutputStream(imageData).use { stream ->
        ImageIO.getImageWritersByFormatName("png").next().run {
          output = stream
          write(image)
        }
      }

      event.hook.editOriginal("Rendered: `${expr}`")
        .setAttachments(FileUpload.fromData(imageData.toByteArray(), "output.png"))
        .await()
    }
  }
}