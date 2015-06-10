/**
 * Created by lastland on 15/6/9.
 */

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import java.util.concurrent.ConcurrentLinkedQueue

object Main extends App {

  var expectedDependence = "akka"
  var mem: Set[AnyRepo] = Set()
  val result = new ConcurrentLinkedQueue[AnyRepo]()

  val repos = new MavenRepos
  for (repo <- repos.repos) {
    if (!mem.contains(repo)) {
      mem = mem + repo
      val f = Future {
        println("trying " + repo)
        val buildDef = repo.buildDef
        val t = (buildDef \ "dependencies" \ "dependency") exists { d =>
          (d \ "artifactId").map(_.text) exists { a =>
            a.contains(expectedDependence)
          }
        }
        if (t) Some(repo) else None
      }
      f onSuccess {
        case Some(repo) =>
          println("-" * 80)
          println("FIND ONE! " + repo)
          println("-" * 80)
          result.add(repo)
        case None => ()
      }
    }
  }
}
