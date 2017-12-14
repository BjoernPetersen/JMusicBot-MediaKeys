package com.github.bjoernpetersen.mediakeys

import com.github.bjoernpetersen.jmusicbot.*
import com.github.bjoernpetersen.jmusicbot.config.Config
import com.github.bjoernpetersen.jmusicbot.config.Config.Entry
import com.github.bjoernpetersen.jmusicbot.platform.Platform
import com.github.bjoernpetersen.jmusicbot.platform.Support
import com.github.bjoernpetersen.jmusicbot.playback.PauseState
import com.github.bjoernpetersen.jmusicbot.playback.PlayState
import com.github.zafarkhaja.semver.Version
import com.tulskiy.keymaster.common.MediaKey
import com.tulskiy.keymaster.common.Provider
import java.io.IOException

private const val NAME = "Media Key Support"

class MediaKeyPlugin : AdminPlugin, Loggable {
  private var provider: Provider? = null

  override fun getReadableName(): String = NAME

  override fun getSupport(platform: Platform): Support = when (platform) {
    Platform.WINDOWS, Platform.LINUX -> Support.YES
    Platform.ANDROID -> Support.NO
    Platform.UNKNOWN -> Support.MAYBE
  }

  override fun getMinSupportedVersion(): Version {
    return Version.forIntegers(0, 14, 0)
  }

  override fun initializeConfigEntries(config: Config): List<Entry> = emptyList()

  override fun destructConfigEntries() {}

  override fun getMissingConfigEntries(): List<Entry> = emptyList()

  @Throws(InitializationException::class)
  override fun initialize(initWriter: InitStateWriter, musicBot: MusicBot) {
    val player = musicBot.player
    provider = Provider.getCurrentProvider(false).apply {
      register(MediaKey.MEDIA_NEXT_TRACK, { player.next() })
      register(MediaKey.MEDIA_PLAY_PAUSE, {
        when (player.state) {
          is PlayState -> player.pause()
          is PauseState -> player.play()
        }
      })
    }
  }


  @Throws(IOException::class)
  override fun close() {
    provider?.apply {
      reset()
      stop()
    }
    provider = null
  }
}
