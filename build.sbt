
name := "SpreadSite"

version := "1.0"

scalaVersion := "2.8.1"

seq(webSettings: _*)

resolvers ++= Seq(
	"Scala-Tools Maven2 Repository" at "http://scala-tools.org/repo-releases",
	"Java.net Maven2 Repository" at "http://download.java.net/maven/2/"
)

libraryDependencies ++= Seq(
	"com.h2database" % "h2" % "1.3.162",
	"postgresql" % "postgresql" % "8.4-702.jdbc4",
	"net.liftweb" % "lift-webkit_2.8.1" % "2.2",
	"net.liftweb" % "lift-mapper_2.8.1" % "2.2",
	"javax.servlet" % "servlet-api" % "2.5",
	"org.mortbay.jetty" % "jetty" % "6.1.22" % "container",
	"org.slf4j" % "slf4j-api" % "1.6.4",
	"ch.qos.logback" % "logback-classic" % "1.0.6",
	"org.scalatest" % "scalatest" % "1.3",
	"org.apache.commons" % "commons-lang3" % "3.1",
	"org.apache.httpcomponents" % "httpclient" % "4.2-alpha1",
	"org.scala-tools.time" % "time_2.8.1" % "0.5"
)
