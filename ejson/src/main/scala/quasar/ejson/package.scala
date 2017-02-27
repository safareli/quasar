/*
 * Copyright 2014–2017 SlamData Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package quasar

import quasar.Predef._
import quasar.fp.ski._
import matryoshka.data.Fix
import matryoshka._, implicits._
import monocle.Prism
import scalaz._
import jawn._

package object ejson {
  def nul[A] =
    Prism.partial[Common[A], Unit] { case Null() => () } (κ(Null()))
  def bool[A] =
    Prism.partial[Common[A], Boolean] { case Bool(b) => b } (Bool(_))
  def dec[A] =
    Prism.partial[Common[A], BigDecimal] { case Dec(bd) => bd } (Dec(_))
  def str[A] = Prism.partial[Common[A], String] { case Str(s) => s } (Str(_))
  def arr[A] =
    Prism.partial[Common[A], List[A]] { case Arr(a) => a } (Arr(_))

  def obj[A] =
    Prism.partial[Obj[A], ListMap[String, A]] { case Obj(o) => o } (Obj(_))

  def byte[A] =
    Prism.partial[Extension[A], scala.Byte] { case Byte(b) => b } (Byte(_))
  def char[A] =
    Prism.partial[Extension[A], scala.Char] { case Char(c) => c } (Char(_))
  def int[A] =
    Prism.partial[Extension[A], BigInt] { case Int(i) => i } (Int(_))
  def map[A] =
    Prism.partial[Extension[A], List[(A, A)]] { case Map(m) => m } (Map(_))
  def meta[A] =
    Prism.partial[Extension[A], (A, A)] {
      case Meta(v, m) => (v, m)
    } ((Meta(_: A, _: A)).tupled)

  /** For _strict_ JSON, you want something like `Obj[Mu[Json]]`.
    */
  type Json[A]  = Coproduct[Obj, Common, A]
  type EJson[A] = Coproduct[Extension, Common, A]

  val ExtEJson = implicitly[Extension :<: EJson]
  val CommonEJson = implicitly[Common :<: EJson]

  val fixParser    = jsonParser[Fix[Json]]
  val fixParserSeq = fixParser async AsyncParser.ValueStream

  def jawnParser[A](implicit z: Facade[A]): SupportParser[A] =
    new SupportParser[A] { implicit val facade: Facade[A] = z }

  def jawnParserSeq[A](implicit z: Facade[A]): AsyncParser[A] =
    jawnParser[A] async AsyncParser.ValueStream

  def jsonParser[T](implicit T: Corecursive.Aux[T, Json]): SupportParser[T] =
    jawnParser(jsonFacade[T, Json])

  def readJson[A: Facade](in: JsonInput): Try[A]            = JsonInput.readOneFrom(jawnParser[A], in)
  def readJsonSeq[A: Facade](in: JsonInput): Try[Vector[A]] = JsonInput.readSeqFrom(jawnParser[A], in)
  def readJsonFix(in: JsonInput): Try[Fix[Json]]            = JsonInput.readOneFrom(fixParser, in)
  def readJsonFixSeq(in: JsonInput): Try[Vector[Fix[Json]]] = JsonInput.readSeqFrom(fixParser, in)

  object EJson {
    def fromJson[A](f: String => A): Json[A] => EJson[A] =
      json => Coproduct(json.run.leftMap(Extension.fromObj(f)))

    def fromCommon[T](implicit T: Corecursive.Aux[T, EJson]): Common[T] => T =
      CommonEJson.inj(_).embed

    def fromExt[T](implicit T: Corecursive.Aux[T, EJson]): Extension[T] => T =
      ExtEJson.inj(_).embed

    def isNull[T](ej: T)(implicit T: Recursive.Aux[T, EJson]): Boolean =
      CommonEJson.prj(ej.project).fold(false) {
        case ejson.Null() => true
        case _ => false
    }
  }
}
