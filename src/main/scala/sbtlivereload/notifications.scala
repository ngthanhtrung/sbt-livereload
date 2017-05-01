package sbtlivereload

private[sbtlivereload] sealed abstract class Notification extends Product with Serializable
private[sbtlivereload] case object ReloadStylesheets extends Notification
private[sbtlivereload] case object ReloadPage extends Notification
