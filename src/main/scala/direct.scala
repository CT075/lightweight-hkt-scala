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
     trait Newtype1S:
       type S[A]
       // ...
   ```

   but this is *exactly* the functionality we are attempting to avoid with this
   encoding!

   One idea would be to attach the generic to the trait itself:

   ```scala
     trait Newtype1S[A]:
       type S
       type T
       // ...
   ```

   But this won't work, because that would create a new brand `T` for every
   instantiation of the generic parameter `A`.

   Instead, we need to delay the application of `A` by creating an inner
   class/trait:
 */

trait Newtype1:
  // The brand. Each instantiation of `Newtype1` will create a new brand.
  type T

  trait ApplyS[A]:
    // The fully-realized type, e.g. `List[A]`
    type S

    // Add a new variant to `Apply` corresponding to our brand
    private case class App(x: S) extends Apply[T, A]

    def inj(x: S): Apply[T, A] = App(x)
    def prj(t: Apply[T, A]): S = t match
      // This match is safe, because `Apply` is sealed so the only way to
      // produce a value of type `Apply[T,A]` for this particular brand is via
      // `inj` above.
      case App(x) => x

/* Now, we can create higher-kinded witnesses by extending `Newtype1`:
 */

object ListW extends Newtype1:
  type T

  class M[A] extends ApplyS[A]:
    type S = List[A]

object OptionW extends Newtype1:
  type T

  class M[A] extends ApplyS[A]:
    type S = Option[A]

object IdW extends Newtype1:
  type T

  class M[A] extends ApplyS[A]:
    type S = A

/*
def Newtype1[A](Base: Newtype1I[A]) = new Newtype1S[A] {
    type T
    type S = Base.T

    private case class App(x: S) extends Apply[T, A]
    def inj(x: S): Apply[T, A] = App(x)
    def prj(t: Apply[T, A]) = t match
      case App(x) => x
      case _ => throw new Exception
  }

def mkHKList[A]() = Newtype1[A](new Newtype1I[A] { type T = List[A] })
*/

/* Note that, although `Apply` is sealed, we can still extend it with the new
   case class `App` each time a new object of the `Newtype1` class is
   constructed. This is allowed because trait sealing only applies at a static
   file-level granularity.
 */
