package com.github.bjoernpetersen.mediakeys

import com.github.bjoernpetersen.jmusicbot.*
import com.github.bjoernpetersen.jmusicbot.config.Config
import com.github.bjoernpetersen.jmusicbot.config.Config.Entry
import com.github.bjoernpetersen.jmusicbot.platform.Platform
import com.github.bjoernpetersen.jmusicbot.platform.Support
import com.github.bjoernpetersen.jmusicbot.playback.Player
import com.github.bjoernpetersen.jmusicbot.playback.PlayerState
import com.github.zafarkhaja.semver.Version
import com.melloware.jintellitype.IntellitypeListener
import com.melloware.jintellitype.JIntellitype
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

private const val JINTELLITYPE = "JIntellitype"

class MediaKeyPlugin : AdminPlugin, Loggable {

  private var file: File? = null
  private var listener: IntellitypeListener? = null
  private var player: Player? = null

  override fun getReadableName(): String {
    return "Media Key Support"
  }

  override fun getSupport(platform: Platform): Support {
    return if (platform == Platform.WINDOWS) Support.YES else Support.NO
  }

  override fun getMinSupportedVersion(): Version {
    return Version.forIntegers(0, 11, 0)
  }

  override fun initializeConfigEntries(config: Config): List<Entry> {
    return emptyList()
  }

  override fun destructConfigEntries() {}

  override fun getMissingConfigEntries(): List<Entry> {
    return emptyList()
  }

  @Throws(InitializationException::class)
  override fun initialize(initWriter: InitStateWriter, musicBot: MusicBot) {
    this.player = musicBot.player
    initWriter.state("Dumping DLL into temporary dir...")
    try {
      file = File.createTempFile(JINTELLITYPE, ".dll")
    } catch (e: IOException) {
      throw InitializationException(e)
    }

    dumpDll(file!!)
    JIntellitype.setLibraryLocation(file!!.absolutePath)
    initWriter.state("Done dumping. Registering hotkeys...")

    listener = IntellitypeListener {
      val player = this.player ?: return@IntellitypeListener
      when (it) {
        JIntellitype.APPCOMMAND_MEDIA_PLAY_PAUSE -> when (player.state.state) {
          PlayerState.State.PLAY -> player.pause()
          PlayerState.State.PAUSE -> player.play()
          else -> Unit
        }
        JIntellitype.APPCOMMAND_MEDIA_NEXTTRACK -> player.next()
      }
    }
    JIntellitype.getInstance().addIntellitypeListener(listener)
  }

  @Throws(InitializationException::class)
  private fun dumpDll(file: File) {
    val buffer = ByteArray(8192)
    try {
      javaClass.getResourceAsStream("/JIntellitype.dll").use { input ->
        FileOutputStream(file).use { out ->
          var read: Int = input.read(buffer)
          while (read > -1) {
            out.write(buffer, 0, read)
            read = input.read(buffer)
          }
        }
      }
    } catch (e: IOException) {
      throw InitializationException(e)
    }

  }

  @Throws(IOException::class)
  override fun close() {
    JIntellitype.getInstance().removeIntellitypeListener(listener)
    listener = null
    JIntellitype.getInstance().cleanUp()
    file?.delete()
    file = null
    this.player = null
  }
}
