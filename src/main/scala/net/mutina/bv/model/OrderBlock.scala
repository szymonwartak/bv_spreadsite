package net.mutina.bv.model

import net.liftweb.common.{Logger, Full}
import net.liftweb.http._
import S._
import net.liftweb.util._
import Helpers._
import scala.xml._
import net.liftweb.mapper._
import java.math.MathContext
import java.util.Date

case class OrderBlock(blockDate:Date, sellOrBuy:String, security:String, quantity:Double, currency:String, price:Int, highPrice:Double, lowPrice:Double, closePrice:Double)
