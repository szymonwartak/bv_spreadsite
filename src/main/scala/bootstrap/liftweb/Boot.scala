package bootstrap.liftweb

import net.liftweb.common.{Box,Empty,Failure,Full,Logger}
import net.liftweb.util._
import net.liftweb.http._
import net.liftweb.sitemap._
import net.liftweb.sitemap.Loc._
import Helpers._
import net.liftweb.mapper.{DB, ConnectionManager, Schemifier, DefaultConnectionIdentifier, ConnectionIdentifier}
import java.sql.{Connection, DriverManager}
import net.mutina.bv.model._
 
/**
  * A class that's instantiated early and run.  It allows the application
  * to modify lift's environment
  */
class Boot {
  def boot {
                        
    LiftRules.addToPackages("net.mutina.bv")     
    LiftRules.early.append { _.setCharacterEncoding("UTF-8") }

    // Build SiteMap
    LiftRules.setSiteMap(SiteMap(MenuInfo.menu :_*))
    LiftRules.ajaxPostTimeout = 15000
    
    // this has to go after the menu defs which fail otherwise (though this should work!)
    if (!DB.jndiJdbcConnAvailable_?) DB.defineConnectionManager(DefaultConnectionIdentifier, DBVendor)
    Schemifier.schemify(true, Log.infoF _, 
    		User, OrderBlock)
  }
}

object MenuInfo {
  import Loc._
  import net.liftweb.sitemap.**

  // Define a simple test clause that we can use for multiple menu items
  val IfLoggedIn = If(() => User.currentUser.isDefined, "You must be logged in")

  def menu: List[Menu] = 
    List[Menu](Menu.i("Home") / "index",
               Menu.i("Market Spread") / "marketSpread")// >> IfLoggedIn)::: 
               //User.sitemap.slice(0,2)
}

object DBVendor extends ConnectionManager {
  def newConnection(name: ConnectionIdentifier): Box[Connection] = {
    try {
//      Class.forName("org.apache.derby.jdbc.EmbeddedDriver")
//      val dm = DriverManager.getConnection("jdbc:derby:lift_example;create=true")
      Class.forName("org.h2.Driver")
      val dm = DriverManager.getConnection("jdbc:h2:tcp://localhost:9092/~/db/bv;LOCK_TIMEOUT=10000","bvapp","bvapplskdjf")
      Full(dm)
    } catch {
      case e : Exception => e.printStackTrace; Empty
    }
  }
  def releaseConnection(conn: Connection) {conn.close}
}

