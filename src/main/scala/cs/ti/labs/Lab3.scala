package cs.ti.labs

import cs.ti.labs.Utils.getFileString

import scala.collection.parallel.ParMap
import scala.math.log10
import scala.util.Try

import scala.collection.JavaConverters._

object EntropyCounter {
  def getEntropy(values: java.util.Map[String, BigDecimal]): BigDecimal = getEntropy(values.asScala.toMap)
  def getEntropy(values: Map[String, BigDecimal]):BigDecimal = getEntropy(values.values.toList)
  private def getEntropy(occurrences: List[BigDecimal]):BigDecimal = occurrences.map(v => getEntropy(v, v)).sum
  private def getEntropy(v1: BigDecimal, v2: BigDecimal) = -v1 * log2(v2.doubleValue())
  private def log2(value: Double) = BigDecimal(log10(value)) / BigDecimal(log10(2))
}

object Splitter {
  def toJava(list: List[List[String]]): java.util.List[java.util.List[String]] = {
    list.map {_.asJava}.asJava
  }

  def splitWithChars(fileString: String, depth: Int): List[List[String]] =
    (1 to depth).map { len =>
      (0 to fileString.length - len)
        .map(start => fileString.substring(start, start + len))
        .toList
    }.toList

  def splitWithWords(fileString: String, depth: Int): List[List[String]] = {
    val words = fileString.split("\\s").filter(!_.isEmpty)
    (1 to depth).map { len =>
      (0 to words.length - len)
        .map(start => words.slice(start, start + len).mkString(" "))
        .toList
    }.toList
  }
}

class EntropyCounter(val provider: (String, Int) => List[List[String]], val grouper: (String) => String, val depth: Int) {
  def count(fileString: String): Unit = {
    val occWithNGramLen = getJoinedProbability(fileString)
    for (len <- 0 until depth) {
      val (wordsOccurrences: Map[String, BigDecimal], conditionalEntropy: BigDecimal) =
        countConditionalEntropy(occWithNGramLen, len)
      printResults(len, wordsOccurrences, conditionalEntropy)
    }
  }

  private def countConditionalEntropy(occWithNGramLen: List[ParMap[String, BigDecimal]], len: Int) = {
    val wordsOccurrences = occWithNGramLen(len)
    val previous = getMapOfWords(occWithNGramLen, len)
    val conditional = conditionalProbability(wordsOccurrences, previous)
    val conditionalEntropy = conditional.values.flatMap {
      t => t.map(kv => EntropyCounter.getEntropy(wordsOccurrences(kv._1), kv._2))
    }.sum
    (wordsOccurrences, conditionalEntropy)
  }

  private def getMapOfWords(occWithNGramLen: List[ParMap[String, BigDecimal]], len: Int) =
    if (len < 1)
      Map[String, BigDecimal]().withDefaultValue(BigDecimal(1)).par
    else
      occWithNGramLen(len - 1)

  private def printResults(len: Int, wordsOccurrences: Map[String, BigDecimal], conditionalEntropy: BigDecimal): Unit = {
    println("------------------------------------------")
    println(s"Depth: $len")
    println(s"merged entropy: ${EntropyCounter.getEntropy(wordsOccurrences)}")
    println(s"conditional entropy: $conditionalEntropy")
  }

  private def conditionalProbability(wordsOccurrences: ParMap[String, BigDecimal],
                                     previous: ParMap[String, BigDecimal]) = {
    val conditionalProbability = wordsOccurrences.groupBy(v => grouper(v._1))
      .map(t => {
        val rootProbability = Try(previous(t._1)).getOrElse(BigDecimal(1))
        val conditionalProbabilities = t._2.mapValues(_ / rootProbability)
        val sumOfConditionalProbabilities = conditionalProbabilities.values.sum
        (t._1, conditionalProbabilities)
      })
    conditionalProbability
  }

  private def getJoinedProbability(fileString: String) =
    provider(fileString, depth).map(fragments => {
      val total = BigDecimal(fragments.length)
      fragments.par.groupBy(f => f).mapValues(_.length / total)
    })

}


object Lab3 {
  val LETTERS_DEPTH = 5
  val WORDS_DEPTH = 4
  val LAB_NUM = 3

  def main(args: Array[String]): Unit = {
    Utils.getFilesInDirectory(LAB_NUM).forEach(f => {
      println(f)
      println("-----------")
      val fileString = getFileString(f, LAB_NUM)
      println("LETTERS")
      new EntropyCounter(Splitter.splitWithChars, _.dropRight(1), LETTERS_DEPTH).count(fileString)
      println("WORDS")
      new EntropyCounter(Splitter.splitWithWords, removeLastWord, WORDS_DEPTH).count(fileString)
      println("------------")
    })
  }

  private def removeLastWord(sentence: String) =
    sentence.split("\\s").dropRight(1).mkString(" ")
}
