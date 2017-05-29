package sbtlivereload

import sbt.Keys._
import sbt._

object LiveReloadPlugin extends AutoPlugin { self =>

  object autoImport {
    lazy val liveReloadServerHost = settingKey[String]("Live-reload server host.")
    lazy val liveReloadServerPort = settingKey[Int]("Live-reload server port.")

    lazy val startLiveReloadAtLoad = settingKey[Boolean]("Start Live-reload at load.")
    lazy val startLiveReload = taskKey[Unit]("Start live-reload.")
    lazy val stopLiveReload = taskKey[Unit]("Stop live-reload.")

    lazy val reloadStylesheets = taskKey[Unit]("Live-reload stylesheets.")
    lazy val reloadPage = taskKey[Unit]("Live-reload page.")
  }

  import autoImport._

  private var server: Option[LiveReloadServer] = None

  private def start(host: String, port: Int): Unit = {
    if (server.isEmpty) {
      self.synchronized {
        if (server.isEmpty) {
          server = Some(
            new LiveReloadServer(host, port)
          )
        }
      }
    }
  }

  private def stop(): Unit = {
    if (server.isDefined) {
      self.synchronized {
        if (server.isDefined) {
          server.foreach(_.kill())
          server = None
        }
      }
    }
  }

  override lazy val projectSettings: Seq[Def.Setting[_]] = Seq(
    liveReloadServerHost := "localhost",
    liveReloadServerPort := 27492,

    startLiveReloadAtLoad := false,
    startLiveReload := start(liveReloadServerHost.value, liveReloadServerPort.value),
    stopLiveReload := stop(),

    reloadStylesheets := server.foreach(_.notify(ReloadStylesheets)),
    reloadPage := server.foreach(_.notify(ReloadPage)),

    (onLoad in Global) := (onLoad in Global).value.compose { state =>
      if (startLiveReloadAtLoad.value) {
        start(liveReloadServerHost.value, liveReloadServerPort.value)
      }

      state
    },

    (onUnload in Global) := (onUnload in Global).value.compose { state =>
      stop()
      state
    }
  )
}
