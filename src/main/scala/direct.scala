package hkt.direct

/* ## The "direct" attack

   This approach uses methods devised by Dan James' in his blog post
   [Scala's Modular Roots](https://pellucidanalytics.tumblr.com/post/94532532890/scalas-modular-roots),
   mapping signatures to traits, functors to classes and modules to objects.

   The first order of business is to create an `app` type equivalent. In
   Yallop's implementation, `app` was defined as an extensible sum type,
   with each application of the `Newtype1` functor creating a new variant
   `App: 'a s -> ('a, t) app`. In Scala, we can reproduce this pattern using
   a sealed trait:
 */

sealed trait Apply[F, A]

/* When attempting to translate the `Newtype1` signature, we immediately
   encounter a problem. That is, it contains the generic type `'a s`, which
   we would write in Scala as

   ```scala
     trait Newtype1:
     type S[A]
     // ...
   ```

   but this is *exactly* the functionality we are attempting to avoid with this
   encoding! Instead, we need to introduce a generic parameter to the trait
   itself:
 */

trait Newtype1S[A]:
  type T
  type S
  def inj(x: S): Apply[T, A]
  def prj(t: Apply[T, A]): S

/* As Scala lacks an analogue to ML's anonymous signatures, we also need an
   explicit trait to represent the input type:
 */

trait Newtype1I[A]:
  type T
  def _hack(v: Nothing): T

/* From here, a translation of `Newtype1` appears to follow straightforwardly:

   ```scala
     class Newtype1[A](val Base: Newtype1I[A]) extends Newtype1S[A]:
       case object T
       type S = Base.T

       private case class App(x: S) extends Apply[T, A]

       def inj(x: S): Apply[T, A] = App(x)
       def prj(t: Apply[T, A]) = t match
         case App(x) => x
   ```

   While this compiles, it doesn't quite work -- the class argument, `Base`,
   is ascribed to the *abstract* signature `Newtype1I[A]`, which will prevent
   the type `S` from ever unifying with a concrete type (say, `List[A]`).
*/

abstract class Newtype1[A]:
  val Base: Newtype1I[A]

  case object T
  type S = Base.T

  private case class App(x: S) extends Apply[T.type, A]

  def inj(x: S): Apply[T.type, A] = App(x)
  def prj(t: Apply[T.type, A]) = t match
    case App(x) => x

/* Note that, although `Apply` is sealed, we can still extend it with the new
   case class `App` each time a new object of the `Newtype1` class is
   constructed. This is allowed because trait sealing only applies at a static
   file-level granularity.
 */
