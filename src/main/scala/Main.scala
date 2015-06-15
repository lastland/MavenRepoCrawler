/**
 * Created by lastland on 15/6/9.
 */

import org.h2.jdbc.JdbcSQLException

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.slick.driver.H2Driver.simple._
import Database.dynamicSession
import scala.slick.jdbc.StaticQuery._

object Main extends App {

  args.toList match {
    case "init" :: Nil =>
      ReposDatabase.DB.withDynSession {
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
      ReposDatabase.DB.withDynSession{updateNA("DROP ALL OBJECTS DELETE FILES").execute}
    case "run" :: Nil =>
      var mem: Set[AnyRepo] = Set()
      val repos = new MavenRepos
      for (r <- repos.repos) {
        if (!mem.contains(r)) {
          mem = mem + r
          val f = Future {
            val repo = r
            ReposDatabase.addRepo(repo)
          }
        }
      }
    case "further" :: Nil =>
      val repoList = ReposDatabase.repos.toList
      for (r <- repoList) {
        val repos = new MavenRepos
        for (ur <- repos.reposOfUsages(r)) {
          Future {
            ReposDatabase.addRepo(ur)
          }
        }
      }
  }
}
