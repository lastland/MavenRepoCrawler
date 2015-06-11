/**
 * Created by lastland on 15/6/9.
 */

import org.h2.jdbc.JdbcSQLException

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import java.util.concurrent.ConcurrentLinkedQueue
import scala.slick.driver.H2Driver.simple._
import Database.dynamicSession
import scala.slick.jdbc.StaticQuery._

object Main extends App {

  args.toList match {
    case "init" :: Nil =>
      MyDatabase.DB.withDynSession {
        import Tables._
        try {
          mavenRepos.ddl.create
        } catch {
          case e: JdbcSQLException =>
            println("mavenRepos already exist, skipped")
        }
        try {
          mavenRepoVersions.ddl.create
        } catch {
          case e: JdbcSQLException =>
            println("mavenRepoVersions already exist, skipped")
        }
      }
    case "reset" :: Nil =>
      MyDatabase.DB.withDynSession{updateNA("DROP ALL OBJECTS DELETE FILES").execute}
    case "run" :: Nil =>
      var mem: Set[AnyRepo] = Set()
      val repos = new MavenRepos
      for (r <- repos.repos) {
        if (!mem.contains(r)) {
          mem = mem + r
          val f = Future {
            MyDatabase.DB.withDynSession {
              val repo = r
              import Tables._
              val thisRepo = mavenRepos.filter {
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
                  (mavenRepos returning mavenRepos.map(_.id)) +=
                    MavenRepo(None, repo.repoInfo.groupId, repo.repoInfo.artifactId)
              }
              val versions = for {
                v <- mavenRepoVersions
                if v.repoId === id
              } yield v
              for (v <- repo.findVersions) {
                println("trying " + v)
                val query = versions.filter(version => version.version === v.version).firstOption
                query match {
                  case Some(version) =>
                    println(v + " already exists")
                  case None =>
                    mavenRepoVersions += MavenRepoVersion(
                      None, id, v.version, repo.buildDef(v).toString())
                }
              }
            }
          }
        }
      }
  }
}
