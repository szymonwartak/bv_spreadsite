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

class OrderBlock extends LongKeyedMapper[OrderBlock] with IdPK {
	def getSingleton = OrderBlock
	object blockDate extends MappedDateTime(this)
	object sellOrBuy extends MappedString(this,1)
	object security extends MappedString(this,10)
	object quantity extends MappedDecimal(this, MathContext.DECIMAL64, 3)
	object currency extends MappedString(this,10)
	object price extends MappedInt(this)
	object highPrice extends MappedDecimal(this, MathContext.DECIMAL64, 2)
	object lowPrice extends MappedDecimal(this, MathContext.DECIMAL64, 2)
	object closePrice extends MappedDecimal(this, MathContext.DECIMAL64, 2)	
}

object OrderBlock extends OrderBlock with LongKeyedMetaMapper[OrderBlock] {
	override def dbTableName = "order_block"
	override def fieldOrder = List(blockDate, sellOrBuy, security, quantity, currency, price, highPrice, lowPrice, closePrice)
	
	def getOrderBlockRange(startDate:Date, endDate:Date) : List[OrderBlock] = {
		OrderBlock.findAll(BySql("blockDate between ? and ?", IHaveValidatedThisSQL("szymon","2012-02-11"), 
				startDate, endDate))
	}
}
