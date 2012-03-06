package net.mutina.bv.util

import org.slf4j.Logger
import org.slf4j.LoggerFactory

trait Logging {
  val log = LoggerFactory.getLogger(getClass)
  def debug(msg: => String) = if (log.isDebugEnabled) log.debug(msg)
  def error(msg: => String, e:Throwable) = if (log.isErrorEnabled) log.error(msg,e)
}