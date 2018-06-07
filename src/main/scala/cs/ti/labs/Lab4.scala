package cs.ti.labs

import java.io.{FileOutputStream, IOException}


import scala.collection.parallel.ParMap
import scala.language.postfixOps
import scala.collection.JavaConverters._

object Codec {
  private val CODING_MODIFIER = BigInt(128)
  private val HEADER_END = "||"

  def toJava[K,V](map: Map[K,V]): java.util.Map[K,V] = map.asJava

  def prepareFileBytes(header: String, binaryString: String): Array[Byte] = {
    val lastCharLength = s"${binaryString.length % 8}"
    val chars = binaryString
      .grouped(8)
      .map(BigInt(_, 2) - CODING_MODIFIER)
      .map(_.toByteArray.apply(0))
      .toArray
    (header + HEADER_END).toBytes ++ lastCharLength.toBytes ++ chars
  }

  def splitHeaderAndContent(encoded: Array[Byte]): (Array[Byte], Int, Array[Byte]) = {
   val separatorIndex = getSeparatorIndex(encoded)
    val contentWithMissingNumber = encoded.drop(separatorIndex)
    val missingLenIndex = HEADER_END.length + 1
    val missingNum = contentWithMissingNumber.slice(HEADER_END.length, missingLenIndex).str.toInt
    val content = contentWithMissingNumber drop missingLenIndex
    val header = encoded take separatorIndex
    (content, missingNum, header)
  }

  private def getSeparatorIndex(encoded: Array[Byte]): Int = {
    val headerByte = HEADER_END.toBytes
    val separatorLen = HEADER_END.length
    for (i <- 0 until (encoded.length - separatorLen)) {
      val window = encoded.slice(i, i + separatorLen)
      if (window.deep == headerByte.deep) {
        return i
      }
    }
    throw new IllegalStateException("Separator not found")
  }

  def prepareHeader(code: java.util.Map[Char, Int], valueSize: Int): String =
    prepareHeader(code.asScala.toList, valueSize)

  private def prepareHeader(code: List[(Char, Int)], valueSize: Int = 2): String =
    code.map(v => s"${v._1}${("" + v._2).prependZeros(valueSize)}").mkString("")

  def getRawCoding(header: Array[Byte], groupSize: Int = 3): Map[String, Char] =  {
    header.grouped(groupSize)
      .map(k => k.splitAt(1))
      .toMap.map(e => (e._2.str.toInt.toBinaryString, e._1.str(0)))
  }

  def getContentBinaryString(coded: Array[Byte], missing: Int): String = {
    if (missing > 0)
      (coded.dropRight(1).map(_.normalize(8)) ++ coded.last.normalize(missing)).mkString("")
    else
      coded.map(_.normalize(8)).mkString("")
  }

  implicit class MyString(val str: String) {
    def prependZeros(len: Int): String =
      if (str.length > len )
        throw new IllegalStateException(s"String is longer than $len")
      else
        str.limit(len).reverse.padTo(len, "0").reverse.mkString("")

    def limit(len: Int): String = if (str.length > len) str.substring(0, len) else str

    def toBytes: Array[Byte] = str.toCharArray.map(_.toByte)
  }

  private implicit class MyByteArr(val byte: Array[Byte]) {
    def str: String = byte.map(b => {
      if (b.toInt < 0) (256 + b.toInt).toChar
      else b.toChar
    }).mkString("")
  }

  private implicit class MyByte(val byte: Byte) {
    def normalize(len: Int): String = (BigInt(byte) + CODING_MODIFIER toInt).toBinaryString.prependZeros(len)
  }
}

class Codec {

  import cs.ti.labs.Codec._
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

  private def encode(text: String, encoding: Map[Char, String], header: String) = {
    val binaryString = text.chars().parallel()
      .mapToObj(c => encoding(c.toChar))
      .toArray
      .mkString("")
    prepareFileBytes(header, binaryString)
  }

  def decode(encoded: Array[Byte]): String = {
    val (content, lastCharLen, header) = splitHeaderAndContent(encoded)
    val coding = getCodingFromHeader(header)
    parseContent(coding, lastCharLen, content)
  }

  private def parseContent(coding: Map[String, Char], missing: Int, coded: Array[Byte]) = {
    val maxSize = getCodeSize(coding)
    val padded = getContentBinaryString(coded, missing)
    padded.grouped(maxSize).toList.par.map(coding(_)).mkString("")
  }

  private def getCodingFromHeader(header: Array[Byte]) = {
    val rawCodding = getRawCoding(header)
    val maxLength = rawCodding.keys.map(_.length).max
    rawCodding.map {t => (t._1.prependZeros(maxLength), t._2)}
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

  def save(data: java.util.List[Byte], fileName: String, labNum: Int): Unit = {
    save(data.asScala.toArray, fileName)
  }

  def save(data: Array[Byte], fileName: String, labNum: Int = 4): Unit = {
    var out = None: Option[FileOutputStream]
    try {
      out = Some(new FileOutputStream(Utils.getLabDir(labNum) + fileName))
      out.get.write(data)
    } catch {
      case e: IOException => e.printStackTrace()
    } finally {
      if (out.isDefined) out.get.close()
    }
  }
}