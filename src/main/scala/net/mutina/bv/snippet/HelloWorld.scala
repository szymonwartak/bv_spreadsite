package net.mutina.bv.snippet

import scala.xml.{NodeSeq,Text}
import net.liftweb.common._
import net.mutina.bv.model.User
import java.util.Date

class HelloWorld {
  def greeting (xhtml : NodeSeq) : NodeSeq = User.currentUser match {
  	case Full(user) => Text("Hello "+User.currentUser.get.firstName)
  	case _ => Text("The time is now after... "+(new Date())) 
  } 
  
}

