package uk.co.mailmemydoc

import org.scalatest.WordSpec

class HelloWorldSpec extends WordSpec {
  "A hello" should {
    "do hell" in {
      assert( Hello("world").world == "world")
    }
  }
}
