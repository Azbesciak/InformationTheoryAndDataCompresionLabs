package cs.ti.labs

import cs.ti.labs.Utils.getFileString

import scala.math.log10


object Lab3 {
  def main(args: Array[String]): Unit = {
    val fileString = getFileString("norm_wiki_en.txt", 3)
    for (len <- 1 to 5) {
      val fragments = (0 to fileString.length - len)
        .map(start => fileString.substring(start, start + len))
      val total = fragments.length.doubleValue()
      val ocurrences = fragments.groupBy(f => f).mapValues(_.length / total)
      val sorted = ocurrences.toList.sortBy(_._2).reverse
      println(s"sum ${sorted.map(_._2).sum}")
      println(getEntropy(sorted))
    }
  }

  def getEntropy(occurences: List[(String, Double)]): Double =
    -occurences.map(_._2).map(v => v * log2(v)).sum

  private def log2(value: Double) = log10(value)/log10(2)
}
