package uk.co.mailmemydoc

import java.net.URI
import java.nio.file.{Files, Paths}
import java.util.GregorianCalendar
import javax.xml.datatype.DatatypeFactory

import generated.docmail.CreateMailingResponse
import grizzled.slf4j.Logger
import org.kohsuke.args4j.CmdLineParser

import scala.collection.JavaConversions._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.control.Exception._
import scalaxb.Base64Binary

case class Address(title: Option[String],
                   firstName: Option[String],
                   surname: Option[String],
                   address1: Option[String],
                   address2: Option[String],
                   address3: Option[String],
                   address4: Option[String],
                   address5: Option[String],
                   address6: Option[String])

case class Credentials(u: String, p: String) {
  def username = Option(this.u)

  def password = Option(this.p)
}

class Args {
  @org.kohsuke.args4j.Option(name = "-h", aliases = Array("--help"), usage = "Print help") var doPrintUsage: Boolean = false
  @org.kohsuke.args4j.Option(name = "-test", aliases = Array("--test"), usage = "Test service URL") var test: Boolean = false
  @org.kohsuke.args4j.Option(name = "-u", aliases = Array("--usernmae"), usage = "Username") var username: String = java.lang.System.getenv("uk.co.docmail.username")
  @org.kohsuke.args4j.Option(name = "-p", aliases = Array("--password"), usage = "Password") var password: String = java.lang.System.getenv("uk.co.docmail.password")
  @org.kohsuke.args4j.Option(name = "-t", aliases = Array("--title"), usage = "Address Title", required = true) var title: String = null
  @org.kohsuke.args4j.Option(name = "-fn", aliases = Array("--first-name"), usage = "Address First Name", required = true) var firstname: String = null
  @org.kohsuke.args4j.Option(name = "-sn", aliases = Array("--surname"), usage = "Address Surname", required = true) var surname: String = null
  @org.kohsuke.args4j.Option(name = "-a1", aliases = Array("--address1"), usage = "Address 1", required = true) var address1: String = null
  @org.kohsuke.args4j.Option(name = "-a2", aliases = Array("--address2"), usage = "Address 2", required = true) var address2: String = null
  @org.kohsuke.args4j.Option(name = "-a3", aliases = Array("--address3"), usage = "Address 3", required = true) var address3: String = null
  @org.kohsuke.args4j.Option(name = "-a4", aliases = Array("--address4"), usage = "Address 4", required = false) var address4: String = null
  @org.kohsuke.args4j.Option(name = "-a5", aliases = Array("--address5"), usage = "Address 5", required = false) var address5: String = null
  @org.kohsuke.args4j.Option(name = "-a6", aliases = Array("--address6"), usage = "Address 6", required = false) var address6: String = null
  @org.kohsuke.args4j.Option(name = "-r", aliases = Array("--reference"), usage = "Reference", required = false) var reference: String = null
  @org.kohsuke.args4j.Option(name = "-f", aliases = Array("--file-path"), usage = "File Path", required = true) var filePath: String = null

  def address = {
    Address(
      title = Option(this.title),
      firstName = Option(this.firstname),
      surname = Option(this.surname),
      address1 = Option(this.address1),
      address2 = Option(this.address2),
      address3 = Option(this.address3),
      address4 = Option(this.address4),
      address5 = Option(this.address5),
      address6 = Option(this.address6)
    )
  }

  def credentials = {
    Credentials(this.username, this.password)
  }
}

class DocmailService(testUri: Boolean = false) {
  val logger = Logger[this.type]

  val testAddress = new java.net.URI("https://www.cfhdocmail.com/TestAPI2/DMWS.asmx")
  val liveAddress = new java.net.URI("https://www.cfhdocmail.com/LiveAPI2/DMWS.asmx")

  def json = Some("JSON")

  def customerApplication = Some("mmmd")

  def productType = Some("A4Letter")

  def documentType = productType

  def deliveryType = Some("Standard")

  def addressNameFormat = Some("Firstname Surname")

  def processMailing(credentials: Credentials, mailingGuid: String) = {
    service.processMailing(username = credentials.username,
      password = credentials.password,
      mailingGUID = mailingGuid,
      customerApplication = customerApplication,
      submit = true,
      partialProcess = false,
      maxPriceExVAT = 10d,
      poReference = None,
      paymentMethod = Option("Topup"),
      skipPreviewImageGeneration = true,
      emailSuccessList = credentials.username,
      emailErrorList = credentials.username,
      httpPostOnSuccess = None,
      httpPostOnError = None,
      json)
  }

  val cake = (new generated.docmail.DMWSSoap12Bindings with
    scalaxb.SoapClientsAsync with
    scalaxb.DispatchHttpClientsAsync {
    override def baseAddress: URI = if (testUri) testAddress else liveAddress
  })

  val service = cake.service

  def addTemplateFile(credentials: Credentials, mailingGuid: String, fileName: Option[String], fileData: Option[Base64Binary]) = {

    service.addTemplateFile(username = credentials.username,
      password = credentials.password,
      mailingGUID = mailingGuid,
      templateName = fileName,
      fileName = fileName,
      fileData = fileData,
      documentType = documentType,
      addressedDocument = false,
      addressFontCode = None,
      templateType = None,
      backgroundName = None,
      canBeginOnBack = true,
      nextTemplateCanBeginOnBack = false,
      protectedAreaPassword = None,
      encryptionPassword = None,
      bleedSupplied = false,
      copies = 1,
      instances = 1,
      instancePageNumbers = None,
      cycleInstancesOnCopies = false,
      returnFormat = json)
  }

  def getBalance(credentials: Credentials) = {
    service.getBalance(credentials.username, credentials.password, None, json)
  }

  def createMailing(credentials: Credentials, reference: Option[String], doubleSided: Boolean = true, isColor: Boolean = false) = {
    val mailingName = Some(String.format("%s %s", reference.getOrElse(""), new java.util.Date()))
    val mailingDescription = Some(String.format("%s %s", credentials.username.getOrElse(""), new java.util.Date()))
    val c = new GregorianCalendar()
    c.setTime(new java.util.Date())
    val nowDate: javax.xml.datatype.XMLGregorianCalendar = DatatypeFactory.newInstance().newXMLGregorianCalendar(c)

    service.createMailing(username = credentials.username,
      password = credentials.password,
      customerApplication = customerApplication,
      productType = productType,
      mailingName = mailingName,
      mailingDescription = mailingDescription,
      isMono = !isColor,
      isDuplex = doubleSided,
      deliveryType = deliveryType,
      courierDeliveryToSelf = false,
      despatchASAP = true,
      despatchDate = nowDate,
      addressNamePrefix = None,
      addressNameFormat = addressNameFormat,
      discountCode = None,
      minEnvelopeSize = None,
      returnFormat = json)
  }

  def addAddress(credentials: Credentials, address: Address, mailingGUID: String) = {

    service.addAddress(username = credentials.username,
      password = credentials.password,
      mailingGUID = mailingGUID,
      address1 = address.address1,
      address2 = address.address2,
      address3 = address.address3,
      address4 = address.address4,
      address5 = address.address5,
      address6 = address.address6,
      useForProof = true,
      title = address.title,
      firstName = address.firstName,
      surname = address.surname,
      fullname = None,
      jobTitle = None,
      companyName = None,
      email = None,
      telephone = None,
      directLine = None,
      mobile = None,
      facsimile = None,
      extraInfo = None,
      notes = None,
      customerAddressID = None,
      customerImportID = None,
      streamPages1 = 0,
      streamPages2 = 0,
      streamPages3 = 0,
      custom1 = None,
      custom2 = None,
      custom3 = None,
      custom4 = None,
      custom5 = None,
      custom6 = None,
      custom7 = None,
      custom8 = None,
      custom9 = None,
      custom10 = None,
      json)
  }

  def print(credentials: Credentials, reference: Option[String], address: Address, payloadFileName: Option[String], payloadFileData: Option[Base64Binary]) = {

    import scala.util.parsing.json._

    def extractMailingGuid(createMailingResponse: CreateMailingResponse): String = {
      createMailingResponse match {
        case CreateMailingResponse(Some(jsons)) =>
          JSON.parseFull(jsons) match {
            case Some(mmap: List[Map[Any, Any]]@unchecked) =>
              val map: Map[String, String] = mmap.flatMap(m => List(m("Key").toString -> m("Value").toString)).toMap
              val mailingGuid = map("MailingGUID")
              logger.info(s"MailingGUID: ${mailingGuid}, OrderRef: ${map("OrderRef")}")
              mailingGuid
            case _ =>
              throw new IllegalArgumentException(createMailingResponse.toString)
          }
        case _ =>
          throw new IllegalArgumentException(createMailingResponse.toString)
      }
    }

    for {
      mailing <- createMailing(credentials, reference)
      guid <- Future {extractMailingGuid(mailing)}
      address <- addAddress(credentials, address, guid)
      blank <- addTemplateFile(credentials, guid, DocMail.blankFileName, Some(scalaxb.Base64Binary(DocMail.blankFileBytes: _*)))
      payload <- addTemplateFile(credentials, guid, payloadFileName, payloadFileData)
      mailing <- processMailing(credentials, guid)
    } yield mailing
   }
}

object DocMail {
  val logger = Logger[this.type]

  def main(args: Array[String]): Unit = {

    val args1 = args.toList
    val args4j: Args = new Args
    val parser = new CmdLineParser(args4j)

    allCatch either parser.parseArgument(args1) match {
      case Right(x) => {
        if (!args4j.doPrintUsage) {

          val credentials = args4j.credentials
          val address = args4j.address

          val payloadFilePath = args4j.filePath
          val file = new java.io.File(payloadFilePath)
          if (!file.exists() || file.isDirectory) {
            logger.error(s"File does not exist or is a directory not a regular file: ${file.getCanonicalPath}")
            System.exit(1)
          }
          val payloadFileName = Some(file.getName)

          val payloadFileBytes = Files.readAllBytes(Paths.get(payloadFilePath))
          val payloadFileData: Option[scalaxb.Base64Binary] = Some(scalaxb.Base64Binary(payloadFileBytes: _*))

          val service = new DocmailService(args4j.test)

          service.print(credentials, Option(args4j.reference), address, payloadFileName, payloadFileData).onComplete { r =>
            logger.info(r)
            System.exit(0)
          }
        }
        else {
          parser.printUsage(System.out)
        }
      }
      case Left(e: Throwable) => {
        logger.warn(e.printStackTrace())
        parser.printSingleLineUsage(System.out)
      }
    }
  }

  val blankName = Some("blank")
  val blankFileName = Some("blank.pdf")

  // a blank A4 PDF
  val blankFileBytes: Array[Byte] = Array(37.toByte, 80.toByte, 68.toByte, 70.toByte, 45.toByte, 49.toByte, 46.toByte, 52.toByte, 10.toByte, 49.toByte, 32.toByte, 48.toByte, 32.toByte, 111.toByte, 98.toByte, 106.toByte, 60.toByte, 60.toByte, 47.toByte, 84.toByte, 121.toByte, 112.toByte, 101.toByte, 47.toByte, 67.toByte, 97.toByte, 116.toByte, 97.toByte, 108.toByte, 111.toByte, 103.toByte, 47.toByte, 80.toByte, 97.toByte, 103.toByte, 101.toByte, 115.toByte, 32.toByte, 50.toByte, 32.toByte, 48.toByte, 32.toByte, 82.toByte, 62.toByte, 62.toByte, 101.toByte, 110.toByte, 100.toByte, 111.toByte, 98.toByte, 106.toByte, 10.toByte, 50.toByte, 32.toByte, 48.toByte, 32.toByte, 111.toByte, 98.toByte, 106.toByte, 60.toByte, 60.toByte, 47.toByte, 84.toByte, 121.toByte, 112.toByte, 101.toByte, 47.toByte, 80.toByte, 97.toByte, 103.toByte, 101.toByte, 115.toByte, 47.toByte, 67.toByte, 111.toByte, 117.toByte, 110.toByte, 116.toByte, 32.toByte, 49.toByte, 47.toByte, 75.toByte, 105.toByte, 100.toByte, 115.toByte, 91.toByte, 51.toByte, 32.toByte, 48.toByte, 32.toByte, 82.toByte, 93.toByte, 62.toByte, 62.toByte, 101.toByte, 110.toByte, 100.toByte, 111.toByte, 98.toByte, 106.toByte, 10.toByte, 51.toByte, 32.toByte, 48.toByte, 32.toByte, 111.toByte, 98.toByte, 106.toByte, 60.toByte, 60.toByte, 47.toByte, 84.toByte, 121.toByte, 112.toByte, 101.toByte, 47.toByte, 80.toByte, 97.toByte, 103.toByte, 101.toByte, 47.toByte, 77.toByte, 101.toByte, 100.toByte, 105.toByte, 97.toByte, 66.toByte, 111.toByte, 120.toByte, 91.toByte, 48.toByte, 32.toByte, 48.toByte, 32.toByte, 54.toByte, 49.toByte, 50.toByte, 32.toByte, 55.toByte, 57.toByte, 50.toByte, 93.toByte, 47.toByte, 80.toByte, 97.toByte, 114.toByte, 101.toByte, 110.toByte, 116.toByte, 32.toByte, 50.toByte, 32.toByte, 48.toByte, 32.toByte, 82.toByte, 47.toByte, 82.toByte, 101.toByte, 115.toByte, 111.toByte, 117.toByte, 114.toByte, 99.toByte, 101.toByte, 115.toByte, 60.toByte, 60.toByte, 62.toByte, 62.toByte, 62.toByte, 62.toByte, 101.toByte, 110.toByte, 100.toByte, 111.toByte, 98.toByte, 106.toByte, 10.toByte, 120.toByte, 114.toByte, 101.toByte, 102.toByte, 10.toByte, 48.toByte, 32.toByte, 52.toByte, 10.toByte, 48.toByte, 48.toByte, 48.toByte, 48.toByte, 48.toByte, 48.toByte, 48.toByte, 48.toByte, 48.toByte, 48.toByte, 32.toByte, 54.toByte, 53.toByte, 53.toByte, 51.toByte, 53.toByte, 32.toByte, 102.toByte, 32.toByte, 10.toByte, 48.toByte, 48.toByte, 48.toByte, 48.toByte, 48.toByte, 48.toByte, 48.toByte, 48.toByte, 48.toByte, 57.toByte, 32.toByte, 48.toByte, 48.toByte, 48.toByte, 48.toByte, 48.toByte, 32.toByte, 110.toByte, 32.toByte, 10.toByte, 48.toByte, 48.toByte, 48.toByte, 48.toByte, 48.toByte, 48.toByte, 48.toByte, 48.toByte, 53.toByte, 50.toByte, 32.toByte, 48.toByte, 48.toByte, 48.toByte, 48.toByte, 48.toByte, 32.toByte, 110.toByte, 32.toByte, 10.toByte, 48.toByte, 48.toByte, 48.toByte, 48.toByte, 48.toByte, 48.toByte, 48.toByte, 49.toByte, 48.toByte, 49.toByte, 32.toByte, 48.toByte, 48.toByte, 48.toByte, 48.toByte, 48.toByte, 32.toByte, 110.toByte, 32.toByte, 10.toByte, 116.toByte, 114.toByte, 97.toByte, 105.toByte, 108.toByte, 101.toByte, 114.toByte, 60.toByte, 60.toByte, 47.toByte, 83.toByte, 105.toByte, 122.toByte, 101.toByte, 32.toByte, 52.toByte, 47.toByte, 82.toByte, 111.toByte, 111.toByte, 116.toByte, 32.toByte, 49.toByte, 32.toByte, 48.toByte, 32.toByte, 82.toByte, 62.toByte, 62.toByte, 10.toByte, 115.toByte, 116.toByte, 97.toByte, 114.toByte, 116.toByte, 120.toByte, 114.toByte, 101.toByte, 102.toByte, 10.toByte, 49.toByte, 55.toByte, 56.toByte, 10.toByte, 37.toByte, 37.toByte, 69.toByte, 79.toByte, 70.toByte, 10.toByte)
}
