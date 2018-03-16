package cs.ts.labs

import cs.ti.labs.Utils
import cs.ti.labs.Utils.SEED_WORD

object Constants {
    const val mostCommonWords = 6000
    const val maxWords = 100
    const val depth = 1
}

fun main(args: Array<String>) {
    val allChars = Utils.readFileCharacters(Utils.WIKI_TXT)
    val words = allChars.joinToString("").split(Regex("\\s"))
    val allDifWordsCount = words.size
    val wordsWithOccurrence = words.groupBy { it }.mapValues { it.value.size }
    val wordsLimit = Math.min(Constants.mostCommonWords, wordsWithOccurrence.size)
    val listOfAllWordsWithOccurrence = wordsWithOccurrence
            .toList()
            .sortedByDescending { it.second }
            .subList(0, wordsLimit)
    val mostCommonWordsNumInText = listOfAllWordsWithOccurrence.map { it.second }.sum()
    listOfAllWordsWithOccurrence.subList(0, 40).forEach {println("${it.first} - ${it.second}")}
    println("percentage of most common words in whole text : ${mostCommonWordsNumInText.toDouble()/allDifWordsCount*100}%")

    val orders = Utils.getObjectsOrderMap(words, Constants.depth)
    val resultString = Utils.prepareMarkovString(orders, Constants.maxWords, Constants.depth, " ") {
        mutableListOf(orders[SEED_WORD])
    }
    println(resultString)
}