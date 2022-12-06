package hkt

/* # Lightweight higher-kinded polymorphism in DOT

   This is an adaptation of "Lightweight higher-kinded polymorphism" (Jeremy
   Yallop and Leo White, FLOPS 2014) to Scala. While Scala itself has
   first-class higher-kinded types and polymorphism, introducing higher-kinded
   types into Scala's underlying type theory (the DOT calculus, or just DOT)
   remains an open problem.

   Yallop's encoding, however, can express higher-kinded patterns in OCaml,
   a first-order system similar to DOT. It was noted by Odersky in his 2013
   talk "Scala with Style" that Scala's path-dependent types form a
   correspondence with ML modules (in particular, the classical SML module
   system extended with first-class modules), so it should be possible to
   reproduce Yallop's encoding in Scala 3 without using Scala's higher-order
   types

   ## OCaml's `higher`

   Yallop's core idea is to perform type-level defunctionalization, with
   first-order type witnesses standing in for higher-kinded types. Their main
   contribution is the interface and implementation of the following OCaml
   functor (a module-level function):

   ```ocaml
     type ('a, 'f) app

     module type Newtype1 = sig
       type 'a s
       type t
       val inj : 'a s -> ('a, t) app
       val prj : ('a, t) app -> 'a s
     end

     module Newtype1(M : sig type 'a t end): Newtype1 with type 'a s = 'a M.t
   ```

   OCaml syntactically rejects higher-kinded type expressions, forcing all
   type constructors to be concrete names rather than variables. To work around
   this, Yallop introduces the yet-unspecified type `app`, which takes two
   parameters:


   - `'a`, representing the argument to the constructor
   - `'f`, a *brand* corresponding to a type operator, which is a proper,
     uninhabited type.

   It may be more illustrative to show an instantiation of this mysterious
   signature:

   ```ocaml
     module List : sig
       type t
       val inj : 'a list -> ('a, t) app
       val prj : ('a, t) app -> 'a list
     end = Newtype1(struct type 'a t = 'a list end)
   ```

   In this case, `List.t` is the brand for the type constructor `list`, so the
   type `('a, List.t) app` is the type expression `'a list`. We can convert
   between the two using the `inj` and `prj` functions.

   Yallop presented two different implementations of the `Newtype1` functor,
   one using an unsafe cast (that was meta-theoretically safe due to module
   encapsulation) and another using extensible sums, which had not yet landed
   in the main OCaml compiler at the time.

   The core difficulty in representing this encoding in "pure" OCaml is in
   the definition of the type `app`. Classically, defunctionalisation is a
   whole-program transformation, in which a single dispatch function handles
   every higher-order function usage. But this is obviously not very modular,
   as it would require knowing the identity of every higher-kinded type
   constructor in advance. This means the `app` type must be extensible
   post-hoc, letting the user create new brands easily. We also need to have
   some means of converting from `('a, f_brand) app` to the underlying proper
   type `'a f`.

   Yallop mentions in passing that Haskell's type families, which provide
   extensible type-level functions, are precisely the functionality needed to
   resolve this. Unfortunately, Scala does not have type families. Even so, we
   can encode a limited form of extensible type functions of kind `T -> T` via
   traits and abstract type members as follows:
*/

trait Func:
  type ReturnVar

/* where a new clause mapping the input `Foo` to the output `Bar` is declared
   by implementing the trait `Func` for `Foo` with `type ReturnVar = Bar`. This
   is actually enough to fully implement `higher`, using only features
   currently present in DOT.

   # `higher` in first-order Scala

   We present two versions of Yallop's representation:

   - A "direct" translation using functional constructs.

   - A more object-oriented encoding.
 */

/*
trait Witness[A]:
  type This

class Apply[A](val F: Witness[A], x: F.This):
  def prj(): F.This = x

trait HKT:
  def mkBrand[A](): Witness[A]

object ListH extends HKT:
  def mkBrand[A]() = new Witness[A] { type This = List[A] }

class PersonM(val W: HKT):
  val IntW = W.mkBrand[Int]()
  val StringW = W.mkBrand[String]()

  case class Person(name: StringW.This, age: IntW.This)

object IdH extends HKT:
  def mkBrand[A]() = new Witness[A] { type This = A }

object OptH extends HKT:
  def mkBrand[A]() = new Witness[A] { type This = Option[A] }
  */

trait Apply[F, A]:
  type This
  def prj(): This

trait Monad[F, A] extends Apply[F, A]:
  def bind[B](f: A => Apply[F, B]): Apply[F, B]

object ListW:
  case object Brand
  type T = Brand.type

  trait Applied[A]:
    type This = List[A]

  class Inj[A](val x: List[A]) extends Apply[T, A], Applied[A]:
    type This = List[A]
    override def prj(): This = x

object OptionW:
  case object Brand
  type T = Brand.type

  trait Applied[A]:
    type This = Option[A]

  class Inj[A](val x: Option[A]) extends Apply[T, A], Monad[T, A], Applied[A]:
    type This = Option[A]
    override def prj(): This = x
    override def bind[B](f: A => Apply[T, B]): Apply[T, B] =
      x match {
        case Some(x) => f(x)
        case None => new Inj[B](None)
      }

/*
trait Monad[brand]:
  def pure[A](x: A): Apply[brand, A]
  def bind[A, B](x: Apply[brand, A], f: A => Apply[brand, B]): Apply[brand, B]

object OptionMonad extends Monad[OptionW.T]:
  override def pure[A](x: A) = new OptionW.Inj[A](Some(x))
  override def bind[A, B]
    (x: Apply[OptionW.T, A], f: A => Apply[OptionW.T, B]): Apply[OptionW.T, B]
  =
    x.prj() match {
      case Some(x) => f(x)
      case None => new OptionW.Inj[B](None)
    }
  */
/*
case object IdW extends HKT

case object ListW extends HKT

class IdH[A](x: A) extends Apply[IdW.type, A]:
  type This = A
  override def prj(): This = x

class ListH[A](x: List[A]) extends Apply[ListW.type, A]:
  type This = List[A]
  override def prj(): This = x

// Example:
case class Person(F: HKT)(name: Apply[F.type, String], age: Apply[F.type, Int])

trait Monad[brand <: HKT]:
  def pure[A](x: A): Apply[brand, A]
  def bind[A, B]
    (x: Apply[brand, A], f: A => Apply[brand, B]):
    Apply[brand, B]

case object OptionW extends HKT
*/
