package sbtlivereload

import sbt.Keys._
import sbt._

object LiveReloadPlugin extends AutoPlugin { self =>

  object autoImport {
    lazy val liveReloadEnabled = settingKey[Boolean]("Live-reload enabled.")
    lazy val liveReloadServerHost = settingKey[String]("Live-reload server host.")
    lazy val liveReloadServerPort = settingKey[Int]("Live-reload server port.")
    lazy val reloadStylesheets = taskKey[Unit]("Live-reload stylesheets.")
    lazy val reloadPage = taskKey[Unit]("Live-reload page.")
  }

  import autoImport._

  private var server: Option[LiveReloadServer] = None

  override lazy val projectSettings: Seq[Def.Setting[_]] = Seq(
    liveReloadEnabled := true,
    liveReloadServerHost := "localhost",
    liveReloadServerPort := 27492,
    reloadStylesheets := server.foreach(_.notify(ReloadStylesheets)),
    reloadPage := server.foreach(_.notify(ReloadPage)),

    (onLoad in Global) := (onLoad in Global).value.compose { state =>
      if (liveReloadEnabled.value && server.isEmpty) {
        self.synchronized {
          if (server.isEmpty) {
            server = Some {
              new LiveReloadServer(liveReloadServerHost.value, liveReloadServerPort.value)
            }
          }
        }
      }

      state
    },

    (onUnload in Global) := (onUnload in Global).value.compose { state =>
      if (liveReloadEnabled.value && server.isDefined) {
        self.synchronized {
          if (server.isDefined) {
            server.foreach(_.kill())
            server = None
          }
        }
      }

      state
    }
  )
}
