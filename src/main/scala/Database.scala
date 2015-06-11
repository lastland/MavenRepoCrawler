/**
 * Created by lastland on 15/6/10.
 */

import scala.slick.driver.H2Driver.simple._

object MyDatabase {
  val dburl = "jdbc:h2:./test.tb"
  val driver = "org.h2.Driver"
  lazy val DB = Database.forURL(dburl, driver = driver)
}
