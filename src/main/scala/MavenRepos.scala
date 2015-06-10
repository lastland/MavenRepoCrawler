/**
 * Created by lastland on 15/6/8.
 */

import org.jsoup.HttpStatusException

import scala.annotation.tailrec
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

  private def reposOfPage(pageLink: String, pageNum: Int): Stream[AnyRepo] = {
    try {
      val page = browser.get(pageLink + s"?p=$pageNum")
      val content = page >?> element("div#maincontent")
      content match {
        case Some(c) =>
          val links = for {
            oa <- (c >> elements("div.im")).map(_ >?> element("a"))
            ra <- oa
          } yield link + ra.attr("href")
          if (!links.isEmpty) {
            links.map(new MavenRepo(_)).toStream #::: reposOfPage(pageLink, pageNum + 1)
          } else {
            Stream.empty
          }
        case None =>
          Stream.empty
      }
    } catch {
      case statusException: HttpStatusException =>
        Stream.empty
    }
  }

  def reposOfPopular: Stream[AnyRepo] = {
    reposOfPage(link + "/popular", 1)
  }

  private def reposOfTags(tags: Seq[String]): Stream[AnyRepo] = {
    if (tags.isEmpty) {
      Stream.empty
    } else {
      val tag = tags.head
      reposOfPage(link + "/tags/" + tag, 1) #::: reposOfTags(tags.tail)
    }
  }

  def reposOfTags: Stream[AnyRepo] = {
    val page = browser.get(link + "/tags")
    val tags = (page >> element("div#maincontent") >> elements("a")).map(_ >> text("a"))
    reposOfTags(tags)
  }

  private def reposOfCategories(categories: Seq[String]): Stream[AnyRepo] = {
    if (categories.isEmpty) {
      Stream.empty
    } else {
      val cat = categories.head
      reposOfPage(link + "/open-source/" + cat, 1) #::: reposOfCategories(categories.tail)
    }
  }

  def reposOfCategories: Stream[AnyRepo] = {
    val page = browser.get(link + "/open-source")
    val categories = (page >> element("div#maincontent") >> elements("a")).map(
      _.attr("href")).filter(_.startsWith("/open-source"))
    reposOfCategories(categories)
  }

  // This function does not guarantee that all repos returned are unique
  override def repos: Stream[AnyRepo] = reposOfPopular #::: reposOfTags #::: reposOfCategories

}
