package net.mutina.bv

import net.mutina.bv.util.Logging
import net.mutina.bv.model.OrderBlock
import java.io._
import java.net.URL
import java.util.Date
import xml.XML
import net.liftweb.mapper.{DB, Schemifier, DefaultConnectionIdentifier}
import net.liftweb.util._
import collection.immutable.HashMap
import org.scala_tools.time.Imports._
import actors.Actor

object Scraper extends Logging {
	val markets = List("GBP"->"AGXLN", "USD"->"AGXLN", "EUR"->"AGXLN")
	var scrapers = Map[String,Map[String,Scraper]]()
	var started = false
	var numThreads = 0

	def go() = {
		def addAndStartScraper(marketPair: Pair[String,String]) = {
			val scraper = new Scraper(marketPair._1, marketPair._2);
			scrapers += marketPair._1->Map(marketPair._2->scraper)
			numThreads += 1
			scraper.start
		}
		this.synchronized {
			if(!started) markets.foreach(addAndStartScraper)
			started = true
		}
	}
	go()
}

class Scraper(currency:String, security:String) extends Actor with Logging {
	val pricesUrl = "http://www.galmarley.com/prices/CHART_BAR_HLC/" + security.substring(0,3) + "/" + currency + "/5/Update"
	val marketUrl = "http://www.bullionvault.com/view_market_depth.do?considerationCurrency=" + currency + "&securityId="+security+"&marketWidth=8&priceInterval=1"

	var currentPrices = Map[String, Double]()
	var marketData = List[OrderBlock]()

	def act() {
		while (true) {
			scrapeSpotPrices(pricesUrl)
			scrapeMarketDepth(marketUrl)
			marketData = marketData.filter(_.blockDate.after(DateTime.now.minusMinutes(120).toDate))
			debug("now sleep. marketData size: "+marketData.size+" numThreads: "+Scraper.numThreads)
			Thread.sleep(20000)
		}
	}
	override def getLogBase() = currency+"->"+security

	def readUrl(url: String): String = {
		val (received, body) = request(url)
		var doc = ""
		val input = new BufferedReader(new InputStreamReader(body))
		Stream.continually(input.readLine()).takeWhile(_ ne null) foreach {
			line => doc += line
		}
		body.close
		doc
	}

	def scrapeSpotPrices(url: String): Boolean = {
		val (received, body) = request(url)
		if (received) {
			val xml = XML.load(body)
			body.close()
			var count = 0
			var lowTotal = 0.0
			var highTotal = 0.0
			var closeTotal = 0.0
			xml \\ "bar" foreach {
				(bar) =>
					val lowStr = (bar \ "low").text.trim
					val highStr = (bar \ "high").text.trim
					val closeStr = (bar \ "close").text.trim
					if (lowStr != null && highStr != null && closeStr != null && lowStr != "" && highStr != "" && closeStr != "") {
						count += 1
						lowTotal += lowStr.toDouble
						highTotal += highStr.toDouble
						closeTotal += closeStr.toDouble
					}
			}
			currentPrices = Map("low" -> (lowTotal / count), "high" -> (highTotal / count), "close" -> (closeTotal / count))
		}
		received
	}

	def scrapeMarketDepth(url: String): Boolean = {
		val scrapeTime = new Date()
		val (received, body) = request(url)

		if (received) {
			var doc = ""
			val input = new BufferedReader(new InputStreamReader(body))
			Stream.continually(input.readLine()).takeWhile(_ ne null) foreach {
				line => doc += line
			}
			body.close
			var blocknum = 0
			doc.split("top.showOrder").foreach {
				block => if (blocknum > 1) {
					parseBlock(scrapeTime, block.substring(block.indexOf("(") + 2, block.indexOf(")") - 1))
				}
				blocknum += 1
			}
		}
		received
	}

	case class OrderBlock(blockDate:Date, sellOrBuy:String, security:String, quantity:Double, currency:String, price:Int,
												highPrice:Double, lowPrice:Double, closePrice:Double)

	def parseBlock(scrapeTime: Date, blockStr: String) {
		val map = parseHttpParams(blockStr)
		val block = OrderBlock(scrapeTime, map("actionIndicator"), map("securityId"), map("quantity").toDouble, map("considerationCurrency"), map("limit").toDouble.toInt,
			currentPrices("high"), currentPrices("low"), currentPrices("close"))
		logBlock.debug(block.toString)
		// add new block and iterate to get rid of values older than the threshold (assumes blocks are enqueued in ascending date order)
		marketData = block :: marketData
	}

	def parseHttpParams(url: String): HashMap[String, String] = {
		var params = new HashMap[String, String]()
		if (url.indexOf("?") >= 0) {
			url.substring(url.indexOf("?") + 1, url.length()).split("&").foreach {
				param =>
					params += param.substring(0, param.indexOf("=")) -> param.substring(param.indexOf("=") + 1)
			}
		}
		params
	}

	def request(urlString: String): (Boolean, InputStream) =
		try {
			val url = new URL(urlString)
			val conn = url.openConnection()
			conn.setRequestProperty("Cookie", "unitOfWeight=KG;")
			val body = conn.getInputStream
			(true, body)
		}
		catch {
			case ex: Exception => (false, null)
		}
}
