package hkt

/* ## The "direct" attack

   The first order of business is to create an `app` type equivalent. Instead
   of creating a type directly, we use a trait:
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
  def inj(s: S): Apply[T, A]
  def prj(t: Apply[T, A]): S

/* We also need an auxiliary trait representing the input to the `Newtype1`
   functor:
 */

trait Newtype1I[A]:
  type T

class Newtype1[A](m: Newtype1I[A]) extends Newtype1S[A]:
  case object T
  case object S extends Apply[T, A]
