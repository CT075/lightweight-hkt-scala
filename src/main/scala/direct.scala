package hkt.direct

/* ## The "direct" attack

   This approach uses methods devised by Dan James' in his blog post
   [Scala's Modular Roots](https://pellucidanalytics.tumblr.com/post/94532532890/scalas-modular-roots),
   mapping signatures to traits, functors to classes and modules to objects.

   The first order of business is to create an `app` type equivalent. In
   Yallop's implementation, `app` was defined as an extensible sum type,
   with each application of the `Newtype` functor creating a new variant
   `App: 'a s -> ('a, t) app`. In Scala, we can reproduce this pattern using
   a sealed trait:
 */

sealed trait Apply[F, A]

/* When attempting to translate the `Newtype` signature, we immediately
   encounter a problem. That is, it contains the generic type `'a s`, which
   we would write in Scala as

   ```scala
     trait NewtypeS:
       type S[A]
       // ...
   ```

   but this is *exactly* the functionality we are attempting to avoid with this
   encoding!

   One idea would be to attach the generic to the trait itself:

   ```scala
     trait NewtypeS[A]:
       type S
       type T
       // ...
   ```

   But this won't work, because that would create a new brand `T` for every
   instantiation of the generic parameter `A`.

   Instead, we need to delay the application of `A` by creating an inner
   class/trait:
 */

trait Newtype:
  // The brand. Each instantiation of `Newtype` will create a new brand.
  type T

  trait ApplyS[A]:
    // The fully-realized type, e.g. `List[A]`
    type S

    // Add a new variant to `Apply` corresponding to our brand
    private case class App(x: S) extends Apply[T, A]

    def inj(x: S): Apply[T, A] = App(x)
    def prj(t: Apply[T, A]): S =
      t match
        case App(x) => x
        case _ => throw new Exception

/* The safety of the incomplete match in `prj` is ensured by the generativity
   of trait instantiation. For each extension `W` of `Newtype`, a unique brand
   `W.T` and trait family `W.ApplyS[_]` is created, alongside a new case class
   `App` of `Apply[W.T, A]` for each instantiation of `ApplyS[A]`. If `W` is
   defined outside of this file, sealing will prevent any further extension of
   `Apply[W.T, A]`.

   As an aside, this use case of sealed traits seems to run counter to
   intuition. `Apply` is sealed, but we can still add new ad-hoc brands for
   each instantiation of `Newtype`? This works because the sealing restriction
   works at a syntactic, not semantic level -- sealing prevents new extensions
   from being extended outside of the file containing the declaration, but new
   variants can still be generated as needed (even potentially at runtime!).

   Now, we can create higher-kinded witnesses by extending `Newtype`:
 */

object ListW extends Newtype:
  class M[A] extends ApplyS[A]:
    type S = List[A]

object OptionW extends Newtype:
  class M[A] extends ApplyS[A]:
    type S = Option[A]

object IdW extends Newtype:
  class M[A] extends ApplyS[A]:
    type S = A

/* Finally, we can be polymorphic over the `F` parameter to `Apply` to achieve
   higher-kinded polymorphism using only the first-order fragment of Scala:
 */

class Person[F](var name: Apply[F, String], var age: Apply[F, Int])

trait MonadW

trait Monad extends Newtype:
  type T <: MonadW

  def bind[A, B](x: Apply[T, A], f: A => Apply[T, B]): Apply[T, B]

/*
object ListW_ extends Monad:
  def bind[A, B](x: Apply[T, A], f: A => Apply[T, B]): Apply[T, B] =
    throw new Exception
  */
