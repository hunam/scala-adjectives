scala-adjectives
================

Infers adjectives from your case-class nouns

This macro library infers adjectives from the Noun type parameter. These adjectives can
then be used to describe Noun instances as a fluid alternative for constructors.

The adjectives are returned as members of a structural type, an instance of which
is created for the supplied noun type. The compiler deals with this correctly, but
IntelliJ will complain (yet build and execute your applications nonetheless).

_scala-adjectives_ is implemented using macro paradise 2.10, for quasiquote support.
Your client code can be vanilla Scala 2.10.


Instructions
------------

It is most convenient to add adjectives for a case class as a field
called 'adjectives' on its companion object. E.g.:

```scala
import com.github.hunam.adjectives.Adjectives._

sealed abstract class Color
case object red extends Color
case object blue extends Color

object Length extends Enumeration {
  val short, long = Value
}

case class Book(
  coverColor: Color = blue,
  length: Length.Value = Length.short,
  interesting: Boolean = false)

object Book {
  val adjectives = mkAdjectives[Book]
}

// ...

import Book.adjectives._

val book: Book = a (long, interesting) Book
```


Limitations apply
-----------------

- only case classes supported for now
- case class properties must be enumerations, booleans, or sealed types. This may be relaxed in the future.
- does not handle ambiguity or duplicate adjectives within scope
- the trailing postfix 'Noun' method is only supported for case classes with default or implicit parameters.
You will otherwise be forced to add another apply with the Noun instance, e.g.:

```scala
  a (long, interesting) (Book(coverColor = red))
```
