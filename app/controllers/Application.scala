package controllers

import java.nio.file.{Files, Paths}
import javax.inject.{Inject, Singleton}

import generated.docmail.ProcessMailingResponse
import grizzled.slf4j.Logger
import play.api.mvc._
import uk.co.mailmemydoc.{Address, Credentials, DocmailService}

import scala.concurrent.ExecutionContext

@Singleton
class Application @Inject()()(implicit exec: ExecutionContext) extends Controller {
  val logger = Logger[this.type]

  val docmailTest = new DocmailService(testUri = true)
  val docmailLive = new DocmailService(testUri = false)


  def index = Action {
    Ok(views.html.index("File Upload In Play"))
  }

  def uploadFile = Action.async(parse.multipartFormData) { request =>

    val username = request.body.dataParts.get("email").get.head
    val password = request.body.dataParts.get("password").get.head
    val title = request.body.dataParts.get("title").get.headOption
    val fname = request.body.dataParts.get("fname").get.headOption
    val lname = request.body.dataParts.get("lname").get.headOption
    val address1 = request.body.dataParts.get("address1").get.headOption
    val address2 = request.body.dataParts.get("address2").get.headOption
    val address3 = request.body.dataParts.get("address3").get.headOption
    val address4 = request.body.dataParts.get("address4").get.headOption
    val address5 = request.body.dataParts.get("address5").get.headOption
    val address6 = request.body.dataParts.get("address6").get.headOption
    val reference = request.body.dataParts.get("reference").get.headOption
    val testOpt: Option[Seq[String]] = request.body.dataParts.get("test")
    logger.info(s"testOpt $testOpt")

    val test = testOpt.isDefined

    val credentials = Credentials(username, password)
    val address = Address(title, fname, lname, address1, address2, address3, address4, address5, address6)

    val pdf = request.body.file("fileUpload").get

    val payloadFileName: Option[String] = Option(pdf.filename)
    val contentType = pdf.contentType.get
    val payloadFileBytes = Files.readAllBytes(Paths.get(pdf.ref.file.getAbsolutePath))
    val payloadFileData: Option[scalaxb.Base64Binary] = Some(scalaxb.Base64Binary(payloadFileBytes: _*))

    if( test ){
      logger.info(s"test $username, $reference, $payloadFileName")
      docmailTest.print(Credentials(username, password), reference, address, payloadFileName, payloadFileData).map {
        (result: ProcessMailingResponse) => Ok("test: "+result.toString)
      }
    } else {
      logger.info(s"live $username, $reference, $payloadFileName")
      docmailLive.print(Credentials(username, password), reference, address, payloadFileName, payloadFileData).map {
        (result: ProcessMailingResponse) => Ok("live: "+result.toString)
      }
    }

  }
}