/**
 * Created by lastland on 15/6/10.
 */
import scala.slick.driver.H2Driver.simple._

object Tables {

  case class MavenRepo(id: Option[Int], groupId: String, artifactId: String)

  class MavenRepoTable(tag: Tag) extends Table[MavenRepo](tag, "Repo") {
    def id = column[Int]("ID", O.PrimaryKey, O.AutoInc)

    def groupId = column[String]("groupId")

    def artifactId = column[String]("artifactId")

    def * = (id.?, groupId, artifactId) <> (MavenRepo.tupled, MavenRepo.unapply)
  }

  val mavenRepos = TableQuery[MavenRepoTable]

  case class MavenRepoVersion(id: Option[Int], repoId: Int, version: String, buildDef: String)

  class MavenRepoVersionTable(tag: Tag) extends Table[MavenRepoVersion](tag, "RepoVersion") {
    def id = column[Int]("ID", O.PrimaryKey, O.AutoInc)

    def repoId = column[Int]("RepoID")

    def version = column[String]("version")

    def buildDef = column[String]("buildDef")

    def * = (id.?, repoId, version, buildDef) <> (MavenRepoVersion.tupled, MavenRepoVersion.unapply)

    def repo = foreignKey("Repo_FK", repoId, mavenRepos)(_.id)
  }

  def mavenRepoVersions = TableQuery[MavenRepoVersionTable]
}