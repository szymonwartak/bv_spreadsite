package net.mutina.bv.snippet

import scala.xml.{NodeSeq,Text}
import net.liftweb.common._
import net.mutina.bv.model.OrderBlock
import net.liftweb.util.Helpers._

import net.liftweb.http.SHtml
import net.liftweb.http.js.JsCmd
import net.liftweb.http.js.JsCmds.Run

import net.liftweb.json.JsonDSL._
import net.liftweb.json.JsonAST._
import net.mutina.bv.Scraper


class MarketSpread {
	object Currencies extends Enumeration {
	  type Currencies = Value
	  val Pound = Value("GBP")
	  val Euro = Value("EUR")
	  val Dollar = Value("USD")
	}
	var currentValue = Currencies.Pound // starting value
	implicit val formats = net.liftweb.json.DefaultFormats
	val theChart = "graphSpace"
	val chartWidth = "800"
	val chartHeight = "400"
	var currencyCombo = Currencies.Pound.toString
	var graphCurrency = Currencies.Pound
	def view = <span>market spread viewing</span>
	
	def timestamp = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
	def restTimestamp (ob : OrderBlock) : String = timestamp.format(ob.blockDate)
		
	def getOrderBlocks : String = {
		import Scraper.scrapers
		val obs = scrapers(graphCurrency.toString)("AGXLN")
		val buffer = new scala.collection.mutable.ListBuffer[JValue]
		obs.marketData.foreach(ob=>buffer+=("orderBlock" ->
    		("blockDate" -> ob.blockDate.getTime) ~
    		("sellOrBuy" -> ob.sellOrBuy) ~
    		("quantity" -> ob.quantity) ~
    		("price" -> ob.price) ~
    		("closePrice" -> ob.closePrice)
    ))
		val json = buffer.toList
		compact(render(JArray(json)))
	}
	
	def updateData : JsCmd = {
	  Run("updateData('"+getOrderBlocks+"','"+theChart+"')")
	}
	def updateGraphCurrency(newCurrency : String) : JsCmd = {
	  graphCurrency = Currencies.withName(newCurrency)
	  updateData
	}
	
	def orderBlocks (xhtml : NodeSeq) : NodeSeq = {
		SHtml.ajaxForm(
			bind("chart", xhtml,
				"chart" -> <head><link type="text/css" href="/css/ui-lightness/jquery-ui-1.8.17.custom.css" rel="stylesheet" />
					<script type="text/javascript" src="/js/util.js"></script>
					<script type="text/javascript" src="/js/chart.js"></script></head>
					<canvas id={theChart} width={chartWidth} height={chartHeight}></canvas>,
				"submit" -> (SHtml.hidden(updateData _, new SHtml.BasicElemAttr("id","refreshChartButton")) ++ <input type="submit" value="refresh"/>),
				"rangeCombo" -> SHtml.ajaxSelect(Currencies.values.toList.map(r=>(r.toString, r.toString)),
						Full(""), updateGraphCurrency _),
				"currency" -> Text(graphCurrency.toString)
		))
	}
}

