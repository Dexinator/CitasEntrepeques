package utils

import scala.io.Source
import scala.util.Random._

sealed trait Diceware {
  type Dicethrow = List[Int]
  type Passphrase = String
  type Word = String
}

sealed trait PassphraseGenerator extends Diceware {

  def lookupDicethrow: Dicethrow => Option[Word]

  def generate(numberOfWords: Int, separator: String = "-") : Passphrase =
   generateDicewareThrows(numberOfWords)
      .map(lookupDicethrow)
      .flatten
      .mkString(separator)

  private def generateDicewareThrows(numOfThrows: Int): List[Dicethrow] =
    1.to(numOfThrows).toList.map(_ => throw5dices())

  /*the diceware lookup table is indexed with a 5 digit base 6 system */
  def throw5dices() : Dicethrow = 1.to(5).map(_ => nextInt(6) + 1).toList
}

case class Wordlist(src: Source) extends Diceware {
  private def numberStringToIntegers(s: String) = s.toList.map(_.toString.toInt)
  private val words : Map[Dicethrow, Word] = src
    .getLines()
    .filterNot(_.startsWith("#")) // not part of default implementation but allows to add comments with # in dicefiles
    .foldLeft(List.empty[(Dicethrow, Word)]){ case (acc, line) =>
      val Array(key, word) = line.split("\t")
      (numberStringToIntegers(key), word) :: acc
    }.toMap

  def lookupDicethrow(dicethrow: Dicethrow) : Option[Word] = words get dicethrow
}
object Wordlist {
  def fromResource(resource: String) = {
    val is = getClass.getResourceAsStream(resource)
    Wordlist(Source.fromInputStream(is)("utf-8"))
  }
}

class DicewareGenerator(wordlist: Wordlist) extends PassphraseGenerator {
  override def lookupDicethrow: Dicethrow => Option[Passphrase] = wordlist.lookupDicethrow
}
object DicewareGenerator {
  def fromResource(resource : String) = new DicewareGenerator(Wordlist.fromResource(resource))
}

object PasswordGenerator {
  lazy val english = DicewareGenerator.fromResource("/wordlists/english_wordlist.diceware")
  lazy val spanish = DicewareGenerator.fromResource("/wordlists/diceware.wordlist.asc")
  
  lazy val default = spanish
}