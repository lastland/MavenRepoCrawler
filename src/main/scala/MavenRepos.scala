/**
 * Created by lastland on 15/6/8.
 */

import scala.io.Source
import scala.xml.{XML, Elem}
import net.ruippeixotog.scalascraper.browser.Browser
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL.Parse._

case class MavenRepoInfo(groupId: String, artifactId: String, version: String) {
  override def hashCode =
    (List(groupId.replace(".", "/"), artifactId, version).mkString("/") + "/").hashCode

  override def toString = List(groupId, artifactId, version).mkString(" ")
}

class MavenRepo(override val link: String) extends AnyRepo(link) {
  private val domain = "http://search.maven.org/"
  private lazy val browser = new Browser

  override def toString = {
    "MavenRepo at " + link
  }

  override def buildDef: Elem = {
    val repo = findLatestVersion
    val repoInfo = "http://search.maven.org/solrsearch/select?q=parentId:\"" + repo.hashCode +
      """" AND type:1&rows=100000&core=filelisting&wt=xml"""
    val page = browser.get(repoInfo)
    val docs = page >> elements("doc")
    val poms = docs.flatMap(doc => doc >> elements("str")).map(_ >> text("str")).filter(
      _.endsWith(".pom")).filter(_.contains("/"))
    val source = Source.fromURL("https://repo1.maven.org/maven2/" + poms(0))
    val xml = source.mkString
    val pom = xml.substring(xml.indexOf("\n")+1, xml.length)
    XML.loadString(pom)
  }

  protected def findVersions: Seq[MavenRepoInfo] = {
    val repoPage = browser.get(link)
    val title: Seq[String] = (repoPage >> elements("p.im-subtitle") >> elements("a")).map(e =>
      e >> text("a")).toList
    val versionTable = repoPage >?> element("table")
    versionTable match {
      case Some(table) =>
        val rows = table >> elements("tr")
        val links = for (row <- rows) yield (row >?> element("td"))
        for {
          olink <- links
          link <- olink
          l <- (link >?> text("a"))
        } yield MavenRepoInfo(title(0), title(1), l)
      case None =>
        Seq()
    }
  }

  protected def findLatestVersion: MavenRepoInfo = findVersions(0)
}

class MavenRepos extends AnyRepos {
  private val link = "http://mvnrepository.com"

  private lazy val browser = new Browser

  private def reposOfPage(pageNum: Int): Stream[AnyRepo] = {
    if (pageNum > 20)
      Stream.empty
    else {
      val page = browser.get(link + s"/popular?p=$pageNum")
      val links = for {
        oa <- (page >> elements("div.im")).map(_ >?> element("a"))
        ra <- oa
      } yield link + ra.attr("href")
      links.map(new MavenRepo(_)).toStream #::: reposOfPage(pageNum + 1)
    }
  }

  override def repos: Stream[AnyRepo] = reposOfPage(1)

}
