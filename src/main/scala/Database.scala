/**
 * Created by lastland on 15/6/10.
 */

import scala.slick.driver.H2Driver.simple._
import Database.dynamicSession

object ReposDatabase {
  val dburl = "jdbc:h2:./test.tb"
  val driver = "org.h2.Driver"
  lazy val DB = Database.forURL(dburl, driver = driver)

  private def repos(from: Int, size: Int): Stream[RepoInfo] = DB.withDynSession {
    if (from > Tables.mavenRepos.length.run) {
      Stream.empty
    } else {
      val rs = for (r <- Tables.mavenRepos.drop(from).take(size)) yield r
      rs.run.map(r => RepoInfo(r.groupId, r.artifactId)).toStream #::: repos(from + size, size)
    }
  }

  def repos: Stream[RepoInfo] = DB.withDynSession {
    repos(0, 1000)
  }

  def addRepo(repo: AnyRepo) = DB.withDynSession {
    val thisRepo = Tables.mavenRepos.filter {
      r => r.groupId === repo.repoInfo.groupId &&
        r.artifactId === repo.repoInfo.artifactId
    } firstOption
    val id: Int = thisRepo match {
      case Some(r) =>
        r.id match {
          case Some(num) => num
          case None =>
            throw new RuntimeException("no id!")
        }
      case None =>
        (Tables.mavenRepos returning Tables.mavenRepos.map(_.id)) +=
          Tables.MavenRepo(None, repo.repoInfo.groupId, repo.repoInfo.artifactId)
    }
    val versions = for {
      v <- Tables.mavenRepoVersions
      if v.repoId === id
    } yield v
    for (v <- repo.findVersions) {
      println("trying " + v)
      val query = versions.filter(version => version.version === v.version).firstOption
      query match {
        case Some(version) =>
          println(v + " already exists")
        case None =>
          Tables.mavenRepoVersions += Tables.MavenRepoVersion(
            None, id, v.version, repo.buildDef(v))
      }
    }
  }
}
