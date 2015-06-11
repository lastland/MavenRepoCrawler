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


class MavenRepo(override val link: String) extends AnyRepo(link) {
  private val domain = "http://search.maven.org/"
  private lazy val browser = new Browser

  override def toString = {
    "MavenRepo at " + link
  }

  override lazy val repoInfo: RepoInfo = {
    val repoPage = browser.get(link)
    val title: Seq[String] = (repoPage >> elements("p.im-subtitle") >> elements("a")).map(e =>
      e >> text("a")).toList
    RepoInfo(title(0), title(1))
  }

  override def buildDef(repo: RepoInfoVersion): Elem = {
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

  override def findVersions: Seq[RepoInfoVersion] = {
    val repoPage = browser.get(link)
    val versionTable = repoPage >?> element("table")
    versionTable match {
      case Some(table) =>
        val rows = table >> elements("tr")
        val links = for (row <- rows) yield (row >?> element("td"))
        for {
          olink <- links
          link <- olink
          l <- (link >?> text("a"))
        } yield RepoInfoVersion(repoInfo.groupId, repoInfo.artifactId, l)
      case None =>
        Seq()
    }
  }

  override def findLatestVersion: RepoInfoVersion = findVersions(0)
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

  private def reposOfCategory(categories: Seq[String]): Stream[AnyRepo] = {
    if (categories.isEmpty) {
      Stream.empty
    } else {
      val cat = categories.head
      reposOfPage(link + cat, 1) #::: reposOfCategory(categories.tail)
    }
  }

  def reposOfCategoryPages(pageNum: Int): Stream[AnyRepo] = {
    val page = browser.get(link + "/open-source?p=" + pageNum)
    val categories = (page >> element("div#maincontent") >> elements("a")).map(
      _.attr("href")).filter(_.startsWith("/open-source/")).distinct
    if (categories.isEmpty) {
      Stream.empty
    } else {
      reposOfCategory(categories) #::: reposOfCategoryPages(pageNum + 1)
    }
  }

  def reposOfCategories: Stream[AnyRepo] = {
    reposOfCategoryPages(1)
  }

  // This function does not guarantee that all repos returned are unique
  override def repos: Stream[AnyRepo] = reposOfPopular #::: reposOfTags #::: reposOfCategories

}
