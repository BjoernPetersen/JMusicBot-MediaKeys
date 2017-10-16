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
import jxgrabkey.HotkeyListener
import jxgrabkey.JXGrabKey
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

private const val NAME = "Media Key Support"
private const val DLLNAME = "jnimediakeys"

@Throws(InitializationException::class)
private fun dumpDll(from: String, suffix: String = ".dll"): File {
  val file: File = try {
    File.createTempFile(DLLNAME, suffix)
  } catch (e: IOException) {
    throw InitializationException(e)
  }

  val buffer = ByteArray(8192)
  try {
    WindowsMediaKeyPlugin::class.java.getResourceAsStream(from).use { input ->
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
  return file
}

class WindowsMediaKeyPlugin : AdminPlugin, Loggable {

  private var file: File? = null
  private var listener: IntellitypeListener? = null
  private var player: Player? = null

  override fun getReadableName(): String = NAME

  override fun getSupport(platform: Platform): Support {
    return if (platform == Platform.WINDOWS) Support.YES else Support.NO
  }

  override fun getMinSupportedVersion(): Version {
    return Version.forIntegers(0, 11, 0)
  }

  override fun initializeConfigEntries(config: Config): List<Entry> = emptyList()

  override fun destructConfigEntries() {}

  override fun getMissingConfigEntries(): List<Entry> = emptyList()

  @Throws(InitializationException::class)
  override fun initialize(initWriter: InitStateWriter, musicBot: MusicBot) {
    this.player = musicBot.player
    initWriter.state("Dumping DLL into temporary dir...")

    file = dumpDll("/JIntellitype" + (if (is64bitJava()) "64" else "") + ".dll")
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

  private fun is64bitJava(): Boolean = System.getProperty("sun.arch.data.model") == "64"

  @Throws(IOException::class)
  override fun close() {
    JIntellitype.getInstance().removeIntellitypeListener(listener)
    listener = null
    JIntellitype.getInstance().cleanUp()
    if (file?.delete() == false) file?.deleteOnExit()
    file = null
    this.player = null
  }
}

// TODO this does not work yet
class LinuxMediaKeyPlugin : AdminPlugin {

  private var file: File? = null
  private var listener: HotkeyListener? = null

  override fun getReadableName(): String = NAME

  override fun getSupport(platform: Platform): Support =
      if (platform == Platform.LINUX) Support.YES else Support.NO

  override fun initializeConfigEntries(p0: Config): List<Entry> = emptyList()

  override fun getMissingConfigEntries(): List<Entry> = emptyList()

  override fun destructConfigEntries() = Unit

  @Throws(InitializationException::class)
  override fun initialize(initWriter: InitStateWriter, musicBot: MusicBot) {
    file = dumpDll("/libjxgrabkey.so", ".so")
    System.load(file!!.absolutePath)

    JXGrabKey.getInstance().addHotkeyListener { println("PRINTED $it") }
  }

  override fun close() {
    JXGrabKey.getInstance().removeHotkeyListener(listener)
    JXGrabKey.getInstance().cleanUp()
    if (file?.delete() == false) file?.deleteOnExit()
    file = null
  }

}
