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

import scala.reflect.macros.Context

class Helper[C <: Context](val c: C) extends QuasiquoteCompat {
  import c.universe._


  def mkAdjectives[T: c.WeakTypeTag] = {

    val nounType = weakTypeOf[T]

    if (!nounType.isCaseClass)
      c.abort(c.enclosingPosition, "target class must be a case class")

    // obtain primary constructor -- case classes should have one
    val ctor = nounType.primaryConstructorOption.getOrElse {
      c.abort(c.enclosingPosition, "target class must have a primary constructor")
    }

    var defs = Vector[Tree]()

    for {
      params <- ctor.paramss
      param <- params
    } (param.typeSignature) match {

      // enumerations
      case tpe if tpe.isEnumValue ⇒
        val name = tpe.ownerEnum.typeSymbol.name.toString.trim + "Adjective"
        defs = defs ++ typeAdjective(param, name, tpe)

      // booleans
      case tpe if tpe =:= typeOf[Boolean] ⇒
        defs = defs :+ booleanAdjective(param)

      // sealed hierarchies
      case tpe if tpe.isSealed ⇒
        val name = tpe.typeSymbol.name.toString.trim + "Adjective"
        defs = defs ++ typeAdjective(param, name, tpe)

    }


    // only generate noun builder if zero-args construction is possible
    if (nounType.primaryConstructor.isZeroArgs) {
      val nounName = newTermName(nounType.typeSymbol.name.toString.trim)
      defs = defs :+
        q"""
          implicit def nounBuilder(adj: Adj$$) = new {
            def $nounName = adj(new Noun$$())
          }
        """.asInstanceOf[Tree]
    }

    c.Expr(q"""{
        ${nounType.mkRename("Noun$")}
        type Adj$$ = io.github.hunam.adjectives.Adjectives.Adj[Noun$$]

        class Adjectives$$ extends AnyRef {
          ..$defs
        }
        new Adjectives$$() {}
    }""".asInstanceOf[Tree])

  }

  private def valueAdjective(member: Symbol, adjective: Symbol, rhs: Tree): Tree =
    q"def ${adjective.name}: Adj$$ = _.copy($member = $rhs)".asInstanceOf[Tree]

  private def booleanAdjective(member: Symbol) =
    valueAdjective(member, member, reify(true).tree)

  private def typeAdjective(member: Symbol, name: String, tpe: Type): List[Tree] = {
    val tpeName = tpe.asInstanceOf[TypeRef].sym.name
    val tpeAlias = c.fresh(tpeName + "$")
    val tpeTerm = newTypeName(tpeAlias)
    val param = newTermName(member.name.toString.trim)
    val convName = newTermName(name)

    List(
      member.typeSignature.mkRename(tpeAlias),
      q"implicit def $convName($param: $tpeTerm): Adj$$ = _.copy($param = $param)".asInstanceOf[Tree]
    )
  }

  // helper extension methods for types and method symbols

  private implicit class TypeExt(tpe: Type) {
    def isEnumValue = tpe <:< typeOf[Enumeration#Value]
    def ownerEnum = tpe.asInstanceOf[TypeRef].pre
    def isSealed = tpe.typeSymbol.isClass && tpe.typeSymbol.asClass.isSealed
    def isCaseClass = tpe.typeSymbol.isClass && tpe.typeSymbol.asClass.isCaseClass
    def primaryConstructorOption = tpe.declarations.collectFirst {
      case method: MethodSymbol if method.isPrimaryConstructor ⇒ method
    }
    def primaryConstructor = primaryConstructorOption.get
    def mkImport = c.parse(s"import ${tpe}")
    def mkRename(name: String) = {
      val escaped = name.replaceAllLiterally("$", "\\$")
      c.parse(s"import ${tpe.toString.replaceFirst("""\.([^\.]+)$""", s".{$$1 ⇒ $escaped}")}")
    }
  }

  private implicit class MethodSymbolExt(method: MethodSymbol) {
    def isZeroArgs = method.paramss.forall(_.forall(p ⇒ p.asTerm.isParamWithDefault || p.asTerm.isImplicit))
  }
}
