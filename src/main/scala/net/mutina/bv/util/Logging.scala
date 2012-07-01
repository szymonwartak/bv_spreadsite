package net.mutina.bv.util

import ch.qos.logback.classic._
import org.slf4j.LoggerFactory

trait Logging {
  val log = LoggerFactory.getLogger(getClass)
  def debug(msg: => String) = if (log.isDebugEnabled) log.debug(getLogBase+" "+msg)
  def error(msg: => String, e:Throwable) = if (log.isErrorEnabled) log.error(getLogBase+" "+msg,e)
	def getLogBase() = ""
}