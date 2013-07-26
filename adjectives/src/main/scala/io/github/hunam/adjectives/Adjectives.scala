/*
 * Copyright (c) 2013, Nadav Wiener
 * All rights reserved.
 *
 *   Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 *   o   Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *
 *   o   Redistributions in binary form must reproduce the above copyright notice,
 *     this list of conditions and the following disclaimer in the documentation
 *     and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */

package io.github.hunam
package adjectives

import scala.language.experimental.macros
import scala.language.postfixOps
import scala.language.existentials
import scala.reflect.macros.Context

/**
 * Infers adjectives from the Noun type parameter. These adjectives can
 * then be used to describe Noun instances as a fluid alternative for constructors.
 *
 * =Instructions=
 *
 * It is most convenient to add adjectives for a case class as the 'adjectives' field of its companion object. E.g.:
 *
 * {{{
 * import com.github.hunam.adjectives.Adjectives._
 *
 * sealed abstract class Color
 * case object red extends Color
 * case object blue extends Color
 *
 * object Length extends Enumeration {
 *   val short, long = Value
 * }
 *
 * case class Book(
 *   coverColor: Color = blue,
 *   length: Length.Value = Length.short,
 *   interesting: Boolean = false)
 *
 * object Book {
 *   val adjectives = mkAdjectives[Book]
 * }
 *
 * // ...
 *
 * import Book.adjectives._
 *
 * val book: Book = a (long, interesting) Book
 * }}}
 *
 *
 * =Limitations apply=
 *
 * <li>only case classes supported for now
 * <li>case class properties must be enumerations, booleans, or sealed types. This may be relaxed in the future.
 * <li>does not handle ambiguity or duplicate adjectives within scope
 * <li>the trailing postfix 'Noun' method is only supported for case classes with default or implicit parameters.
 * You will otherwise be forced to add another apply with the Noun instance, e.g.:
 *
 * {{{
 *        a (long, interesting) (Book(coverColor = red))
 * }}}
 */
object Adjectives {

  type Adj[Noun] = Noun ⇒ Noun

  def an[Noun](description: Adj[Noun]*): Adj[Noun] = a[Noun](description: _*)

  def a[Noun](description: Adj[Noun]*): Adj[Noun] = noun0 ⇒
    description.foldLeft(noun0) {
      (noun, adj) ⇒ adj(noun)
    }


  /**
   * @tparam Noun the case class to generate adjectives from
   * @return a structural type instance whose members provide adjectives
   */
  def mkAdjectives[Noun] = macro mkAdjectives_impl[Noun]

  def mkAdjectives_impl[Noun: c.WeakTypeTag](c: Context) = {
    val helper = new Helper[c.type](c)
    helper.mkAdjectives[Noun].asInstanceOf[c.Expr[Any]]
  }
}
