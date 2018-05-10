package cs.ti.labs

import java.io.{FileOutputStream, IOException}

import scala.collection.parallel.ParMap

class Codec {
  private val HEADER_END = '|'
  val CODING_MODIFIER = BigInt(127)

  def encode(text: String): Array[Byte] = {
    val occurrences = getOccurrence(text)
    val code = createCode(occurrences)
    val header = prepareHeader(code)
    val codeMap = mapCodeToBinary(code)
    val bytes = encode(text, codeMap, header)
    bytes
  }

  private def mapCodeToBinary(code: List[(Char, Int)]) = {
    val codeLength = getCodeSize(code)
    val codeMap = code.toMap.mapValues(_.toBinaryString.prependZeros(codeLength))
    codeMap
  }

  private def getOccurrence(text: String) =
    text.toCharArray.par.groupBy(f => f).mapValues(_.length)

  private def createCode(occurrences: ParMap[Char, Int]) =
    occurrences
      .toList.sortBy(_._2)
      .reverse.zipWithIndex
      .map(v => (v._1._1, v._2 + 1))

  private def prepareHeader(code: List[(Char, Int)]) =
    code.map(v => s"${v._1}${("" + v._2).prependZeros(2)}").mkString("") + HEADER_END

  private def encode(text: String, encoding: Map[Char, String], header: String) = {
    val binaryString = text.chars().parallel()
      .mapToObj(c => encoding(c.toChar))
      .toArray
      .mkString("")
    val lastCharLength = s"${binaryString.length % 8}"

    val chars = binaryString
      .grouped(8)
      .map(BigInt(_, 2) - CODING_MODIFIER)
      .map(_.toByteArray.apply(0))
      .toArray
    header.toBytes ++ lastCharLength.toBytes ++ chars
  }

  def decode(encoded: Array[Byte]): String = {
    val (content, lastCharLen, header) = splitHeaderAndContent(encoded)
    val coding = getCodingFromHeader(header)
    parseContent(coding, lastCharLen, content)
  }

  private def parseContent(coding: Map[String, String], missing: Int, coded: Array[Byte]) = {
    val maxSize = getCodeSize(coding)
    val padded = coded.dropRight(1).map(_.normalize(8)) ++ coded.last.normalize(missing)
    padded.mkString("").grouped(maxSize).toList.par.map(coding(_)).mkString("")
  }

  private implicit class MyString(val str: String) {
    def prependZeros(len: Int): String =
      str.limit(len).reverse.padTo(len, "0").reverse.mkString("")

    def limit(len: Int): String = if (str.length > len) str.substring(0, len) else str

    def toBytes: Array[Byte] = str.toCharArray.map(_.toByte)
  }

  private implicit class MyByteArr(val byte: Array[Byte]) {
    def str = new String(byte, "UTF-8")
  }

  private implicit class MyByte(val byte: Byte) {
    def normalize(len: Int): String = (BigInt(byte) + CODING_MODIFIER toInt).toBinaryString.prependZeros(len)
  }

  private def splitHeaderAndContent(encoded: Array[Byte]) = {
    val headerByte = HEADER_END.toByte
    val contentWithMissingNumber = encoded.dropWhile(_ != headerByte)
    val missingNum = contentWithMissingNumber.slice(1, 2).str.toInt
    val content = contentWithMissingNumber drop 2
    val header = encoded dropRight contentWithMissingNumber.length
    (content, missingNum, header)
  }

  private def getCodingFromHeader(header: Array[Byte]) = {
    val reversedCoding = header.grouped(3)
      .map(k => k.splitAt(1))
      .toMap.map(e => (e._2.str.toInt, e._1.str))
    val size = getCodeSize(reversedCoding)
    reversedCoding.map(e => (e._1.toBinaryString.prependZeros(size), e._2))
  }

  private def getCodeSize(reversedCoding: Iterable[_]) = {
    reversedCoding.size.toBinaryString.length
  }
}

object Lab4 {
  val TEMP_FILE_NAME = "alamakota.txt"

  def main(args: Array[String]): Unit = {
    val fileString = Utils.getFileString(Utils.WIKI_TXT, 1)
    val codec = new Codec
    val encoded = codec.encode(fileString)
    save(encoded, TEMP_FILE_NAME)
    val encodedFile = Utils.getFileBytes(TEMP_FILE_NAME, 4)
    val decoded = codec.decode(encodedFile)
    if (!(fileString equals decoded)) {
      throw new IllegalStateException("Original and after decoding are not equal")
    } else {
      println(s"SUCCESS! compression: ${encoded.length / fileString.length.doubleValue * 100} %")
    }
  }

  def save(data: Array[Byte], fileName: String): Unit = {
    var out = None: Option[FileOutputStream]
    try {
      out = Some(new FileOutputStream(Utils.getLabDir(4) + fileName))
      out.get.write(data)
    } catch {
      case e: IOException => e.printStackTrace()
    } finally {
      if (out.isDefined) out.get.close()
    }
  }
}