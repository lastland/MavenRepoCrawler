/**
 * Created by lastland on 15/6/8.
 */
import scala.xml.Elem

abstract class AnyRepo(val link: String) {
  def buildDef: Elem
}

abstract class AnyRepos {
  def repos: Stream[AnyRepo]
}
