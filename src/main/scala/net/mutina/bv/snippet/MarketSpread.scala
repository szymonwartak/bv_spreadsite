package net.mutina.bv.snippet

import scala.xml.{NodeSeq,Text}
import scala.collection.immutable._
import net.liftweb.common._
import net.mutina.bv.model.{User,OrderBlock}
import net.liftweb.mapper._
import net.liftweb.util.Helpers._

import net.liftweb.http.{S,SHtml}
import net.liftweb.http.js.{JsCmd,Jx,JsExp}
import net.liftweb.http.js.JsCmds.{Run,JsCrVar,Alert,SetExp}
import net.liftweb.http.js.JE.{JsRaw,JsArray,JsObj}

import net.liftweb.json.Serialization.write
import net.liftweb.json.JsonDSL._
import net.liftweb.json.JsonAST._

import org.scala_tools.time.Imports._
import org.scala_tools.time.Implicits._
import net.mutina.bv.Scraper

class MarketSpread {
	object Currencies extends Enumeration {
	  type Currencies = Value
	  val Pound = Value("£")
	  val Euro = Value("€")
	  val Dollar = Value("$")
	}
	var currentValue = Currencies.Pound // starting value
	implicit val formats = net.liftweb.json.DefaultFormats
	val theChart = "graphSpace"
	val chartWidth = "800"
	val chartHeight = "400"
	var currencyCombo = Currencies.Pound.toString()
	var graphCurrency = Currencies.Pound
	def view = <span>market spread viewing</span>
	
	def timestamp = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
	def restTimestamp (ob : OrderBlock) : String = timestamp.format(ob.blockDate.is)
		
	def getOrderBlocks : String = {
		println("getting blocks")
		import Scraper.scrapers
		val obs = scrapers("GBP")("AGXLN")
		println("num blocks:"+obs.marketData)
		val buffer = new scala.collection.mutable.ListBuffer[JValue]
		obs.marketData.foreach(ob=>buffer+=("orderBlock" ->
    		("blockDate" -> ob.blockDate.is.getTime) ~
    		("sellOrBuy" -> ob.sellOrBuy.is) ~
    		("quantity" -> ob.quantity.is) ~
    		("price" -> ob.price.is) ~
    		("closePrice" -> ob.closePrice.is)
    ))
		val json = buffer.toList
	    compact(render(JArray(json)))
	}
	
	def updateData : JsCmd = {
		println("gc:"+graphCurrency)
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
				"rangeCombo" -> SHtml.ajaxSelect(Currencies.values.toList.map(r=>(r.toString(),r.toString())),
						Full(""), updateGraphCurrency _),
				"currency" -> Text(graphCurrency.toString())
		))
	}
}

