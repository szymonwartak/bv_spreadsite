package net.mutina.bv

import net.mutina.bv.util.Logging
import net.mutina.bv.model.OrderBlock
import java.io._
import java.net.URL
import java.util.{Date}
import java.sql.{Connection, DriverManager}
import java.text.SimpleDateFormat
import org.apache.http.{NameValuePair, HttpResponse, HttpEntity}
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.{HttpPost, HttpGet}
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.message.BasicNameValuePair
import org.apache.http.protocol.HTTP
import xml.{Elem, XML}
import scala.util.matching.Regex
import scala.util.parsing.combinator._
import net.liftweb.mapper.{DB, ConnectionManager, Schemifier, DefaultConnectionIdentifier, ConnectionIdentifier}
import net.liftweb.common.{Box, Empty, Failure, Full, Logger}
import net.liftweb.util._
import net.liftweb.http._
import bootstrap.liftweb.DBVendor
import collection.immutable.{Queue, HashMap}
import org.scala_tools.time.Imports._
import org.scala_tools.time.Implicits._
import actors.Actor
import concurrent.ops._

object Scraper extends Logging {
	if (!DB.jndiJdbcConnAvailable_?) DB.defineConnectionManager(DefaultConnectionIdentifier, DBVendor)
	Schemifier.schemify(true, Log.neverF _, OrderBlock)
	val markets = List("GBP"->"AGXLN")
	var scrapers = Map[String,Map[String,Scraper]]()
	var started = false

	def go() = {
		def addAndStartScraper(marketPair: Pair[String,String]) = {
			val scraper = new Scraper(marketPair._1, marketPair._2);
			val existingBlocks = OrderBlock.getOrderBlockRange(marketPair._1, marketPair._2, DateTime.now.minusMinutes(90).toDate, DateTime.now.toDate)
			scraper.marketData = existingBlocks ::: scraper.marketData
			scrapers += marketPair._1->Map(marketPair._2->scraper);
			scraper.start();
		}
		this.synchronized {
			if(!started) markets.foreach(addAndStartScraper)
			started = true
		}
	}
	go()
}

class Scraper(currency:String, security:String) extends Actor with Logging {
	var currentPrices = Map[String, Double]()
	val pricesUrl = "http://www.galmarley.com/prices/CHART_BAR_HLC/" + security.substring(0,3) + "/" + currency + "/5/Update"

	// var with immutable collection so we don't need to manage queue thread safety
	var marketData = List[OrderBlock]()
	val marketUrl = "http://www.bullionvault.com/view_market_depth.do?considerationCurrency=" + currency + "&securityId="+security+"&marketWidth=8&priceInterval=1"

	def act() {
		while (true) {
			scrapeSpotPrices(pricesUrl)
			scrapeMarketDepth(marketUrl)
//			println("added block. Size:"+marketData.size)
			Thread.sleep(20000)
		}
	}

	def readUrl(url: String): String = {
		val (received, body) = request(url)
		var doc = ""
		val input = new BufferedReader(new InputStreamReader(body))
		Stream.continually(input.readLine()).takeWhile(_ ne null) foreach {
			line => doc += line
		}
		body.close();
		doc
	}

	def scrapeSpotPrices(url: String): Boolean = {
		val (received, body) = request(url)
		if (received) {
			val xml = XML.load(body)
			var count = 0;
			var lowTotal = 0.0;
			var highTotal = 0.0;
			var closeTotal = 0.0;
			xml \\ "bar" foreach {
				(bar) =>
					val lowStr = (bar \ "low").text.trim();
					val highStr = (bar \ "high").text.trim();
					val closeStr = (bar \ "close").text.trim();
					if (lowStr != null && highStr != null && closeStr != null && lowStr != "" && highStr != "" && closeStr != "") {
						count += 1;
						lowTotal += lowStr.toDouble;
						highTotal += highStr.toDouble;
						closeTotal += closeStr.toDouble;
					}
			}
			currentPrices = Map("low" -> (lowTotal / count), "high" -> (highTotal / count), "close" -> (closeTotal / count))
		}
//		println("spot: "+currentPrices("low")+" "+currentPrices("high")+" "+currentPrices("close"))
		return received
	}

	def scrapeMarketDepth(url: String): Boolean = {
		val scrapeTime = new Date()
		val (received, body) = request(url)

		if (received) {
			val input = new BufferedReader(new InputStreamReader(body))
			var in = "A"
			var doc = ""
			while (in != null) {
				in = input.readLine()
				doc += in
			}
			body.close();
			var blocknum = 0
			doc.split("top.showOrder").foreach {
				block => if (blocknum > 1) {
					parseBlock(scrapeTime, block.substring(block.indexOf("(") + 2, block.indexOf(")") - 1))
				}
				blocknum += 1
			}
		}
		return received
	}

	def parseBlock(scrapeTime: Date, blockStr: String) {
		val map = parseHttpParams(blockStr)
		log.debug(map("actionIndicator") + map("securityId") + map("quantity") + map("considerationCurrency") + map("limit"))
		var block = OrderBlock.create
			.currency(map("considerationCurrency"))
			.blockDate(scrapeTime)
			.price(map("limit").toDouble.toInt)
			.security(map("securityId"))
			.sellOrBuy(map("actionIndicator"))
			.quantity(map("quantity").toDouble)
			.highPrice(currentPrices("high"))
			.lowPrice(currentPrices("low"))
			.closePrice(currentPrices("close"))
		spawn {
			block.save
		}
		// add new block and iterate to get rid of values older than the threshold (assumes blocks are enqueued in ascending date order)
//		println(scrapeTime+" "+map("quantity").toDouble+" "+map("limit").toDouble.toInt+" "+currentPrices("close"))
		marketData = block :: marketData.dropWhile(_.blockDate.get.before(DateTime.now.minusHours(2).toDate))
	}

	def parseHttpParams(url: String): HashMap[String, String] = {
		var params = new HashMap[String, String]();
		if (url.indexOf("?") >= 0) {
			url.substring(url.indexOf("?") + 1, url.length()).split("&").foreach {
				param =>
					params += param.substring(0, param.indexOf("=")) -> param.substring(param.indexOf("=") + 1)
			}
		}
		params
	}

	def outputHLC(xml: Elem) {
		println(xml \ "message" \ "bars" \ "bar".toString())
		xml \\ "bar" foreach {
			(bar) =>
				println((bar \ "low").text.trim())
		}
	}

	def request(urlString: String): (Boolean, InputStream) =
		try {
			val url = new URL(urlString)
			val conn = url.openConnection()
			conn.setRequestProperty("Cookie", "unitOfWeight=KG;")
			val body = conn.getInputStream()
			(true, body)
		}
		catch {
			case ex: Exception => (false, null)
		}
}

object DBVendor extends ConnectionManager {
	def newConnection(name: ConnectionIdentifier): Box[Connection] = {
		try {
			//			Class.forName("org.apache.derby.jdbc.EmbeddedDriver")
			//			val dm = DriverManager.getConnection("jdbc:derby:lift_example;create=true")
			//			Class.forName("org.postgresql.Driver")
			//			val dm = DriverManager.getConnection("jdbc:postgresql://localhost:5432/mutina","bvapp","bvapplskdjf")
			Class.forName("org.h2.Driver")
			val dm = DriverManager.getConnection("jdbc:h2:tcp://localhost:9092/~/db/bv", "bvapp", "bvapplskdjf")
			Full(dm)
		} catch {
			case e: Exception => e.printStackTrace; Empty
		}
	}

	def releaseConnection(conn: Connection) {
		conn.close
	}
}

