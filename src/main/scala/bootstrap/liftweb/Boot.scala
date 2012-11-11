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
  }
}

object MenuInfo {
  import Loc._
  import net.liftweb.sitemap.**

  // Define a simple test clause that we can use for multiple menu items
  val IfLoggedIn = If(() => User.currentUser.isDefined, "You must be logged in")

  def menu: List[Menu] = 
    List[Menu](	Menu.i("SW") / "index",
               	Menu.i("Market Spread") / "marketSpread",
								Menu.i("Music") / "music",
								Menu.i("CV") / "cv")
}
