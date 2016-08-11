import java.util.{Calendar, GregorianCalendar}
import javax.xml.datatype.DatatypeFactory

import org.scalatestplus.play._


object IntegrationSpec {
  def dateToXml(date: java.util.Date) = {
    val c = new GregorianCalendar();
    c.setTime(date);
    DatatypeFactory.newInstance().newXMLGregorianCalendar(c);
  }

  def dateAddYear(date: java.util.Date, years: Int) = {
    val c = new GregorianCalendar();
    c.setTime(date);
    c.add(Calendar.YEAR, years); // to get previous year add -1
    c.getTime();
  }

}

/**
  * add your integration spec here.
  * An integration test will fire up a whole play application in a real (or headless) browser
  */
class IntegrationSpec extends PlaySpec with OneServerPerTest with OneBrowserPerTest with HtmlUnitFactory {

  "Application" should {

    "run ws" in {


    }
  }
}
