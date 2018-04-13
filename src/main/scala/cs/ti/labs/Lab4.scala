package cs.ti.labs

import java.io.{FileOutputStream, IOException}

import cs.ti.labs.Lab4.MyString


object Lab4 {
  val HEADER_END = '|'
  def main(args: Array[String]): Unit = {
    //    val fileString = Utils.getFileString(Utils.WIKI_TXT, 1)
    val fileString = "abcd"
    getOccurence(fileString)
  }

  def getOccurence(text: String) = {
    val total = text.length.toDouble
    val occurences = text.toCharArray.groupBy(f => f).mapValues(_.length)
    create(occurences, text)
  }

  def create(occurences: Map[Char, Int], text: String) = {
    val valueToChar = occurences
      .toList.sortBy(_._2)
      .reverse.zipWithIndex
      .map(v => (v._1._1, v._2 + 1))
    val map = valueToChar.toMap.mapValues(_.toShort)
    val header = prepareHeader(valueToChar)

    encode(map, text, header)
  }

  private def prepareHeader(valueToChar: List[(Char, Int)]) = {
    valueToChar.map(v => s"${v._1}${("" + v._2).prependZeros(2)}").mkString("") + HEADER_END
  }

  def encode(encoding: Map[Char, Short], text: String, header: String) = {
    val maxLength = getCodeSize(encoding)

    val binaryString = text.chars()
      .map(c => encoding(c.toChar))
      .toArray
      .map(_.toBinaryString)
      .map(_.prependZeros(maxLength))
      .mkString("")
    println(binaryString)
    val lastCharLength = s"${binaryString.length % 8}"
    val chars = binaryString
      .grouped(8)
      .map(BigInt(_, 2).toByteArray.apply(0))
      .toArray
    val endString = chars.mkString("")
    println(endString)
    val resChars = header.toBytes ++ lastCharLength.toBytes ++ chars
    //    val str = new String(resChars, "ASCII")
    decode(resChars)
//        writeToFile(resChars, "result.bin")
  }

  def decode(encoded: Array[Byte]) = {
    val (content, lastCharLen, header) = splitHeaderAndContent(encoded)
    val coding = getCodingFromHeader(header)
    parseContent(coding, lastCharLen, content)
//    println(coding)
//    println(content)
    //    content.foreach(println(_))
    //    println(content)
  }

  def parseContent(coding: Map[String, String], missing: Int, coded: Array[Byte]) = {
    val maxSize = getCodeSize(coding)
    val binars = coded.map(_.toBinaryString)
    val padded = binars.dropRight(1).map(_.prependZeros(8)) ++ binars.last.prependZeros(missing)
    val result = padded.mkString("").grouped(maxSize).map(coding(_)).mkString("")
    println(result)
  }

  private def splitHeaderAndContent(encoded: Array[Byte]) = {
    val headerByte = HEADER_END.toByte
    val contentWithMissingNumber = encoded.dropWhile(_ != headerByte)
    val missingNum = contentWithMissingNumber.slice(1, 2).str.toInt
    val content = contentWithMissingNumber.drop(2)
    val header = encoded.dropRight(contentWithMissingNumber.length)
    (content, missingNum, header)
  }

  private def getCodingFromHeader(header: Array[Byte]) = {
    val reversedCoding = header.grouped(3)
      .map(k => k.splitAt(1))
      .toMap.map(e => (e._2.str.toInt, e._1.str))
    val size = getCodeSize(reversedCoding)
    reversedCoding.map(e => (e._1.toBinaryString.prependZeros(size), e._2))
  }

  private def getCodeSize(reversedCoding: Map[_, _]) = {
    reversedCoding.size.toBinaryString.length
  }

  def writeToFile(data: Array[Byte], fileName: String) = {
    var out = None: Option[FileOutputStream]
    try {
      out = Some(new FileOutputStream(Utils.getLabDir(4) + fileName))
      out.get.write(data)
    } catch {
      case e: IOException => e.printStackTrace()
    } finally {
      println("entered finally ...")
      if (out.isDefined) out.get.close()
    }
  }

  implicit class MyString(val str: String) {
    def prependZeros(len: Int) =
      str.limit(len).reverse.padTo(len, "0").reverse.mkString("")

    def limit(len: Int) = if (str.length > len) str.substring(0, len) else str

    def toBytes = str.toCharArray.map(_.toByte)
  }

  implicit class MyByte(val byte: Array[Byte]) {
    def str = new String(byte, "UTF-8")
  }

}
