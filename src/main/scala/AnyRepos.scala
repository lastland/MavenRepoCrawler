/**
 * Created by lastland on 15/6/8.
 */
import scala.xml.Elem

case class RepoInfo(groupId: String, artifactId: String)

case class RepoInfoVersion(groupId: String, artifactId: String, version: String) {
  override def hashCode =
    (List(groupId.replace(".", "/"), artifactId, version).mkString("/") + "/").hashCode

  override def toString = List(groupId, artifactId, version).mkString(" ")
}

abstract class AnyRepo(val link: String) {
  def repoInfo: RepoInfo
  def buildDef(repo: RepoInfoVersion): Elem
  def findVersions: Seq[RepoInfoVersion]
  def findLatestVersion: RepoInfoVersion
}

abstract class AnyRepos {
  def repos: Stream[AnyRepo]
}
