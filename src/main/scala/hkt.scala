package hkt

/* # Lightweight higher-kinded polymorphism in Scala

   This is an adaptation of "Lightweight higher-kinded polymorphism" (Jeremy
   Yallop and Leo White, FLOPS 2014) to Scala.

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
   is actually enough to fully implement `higher`, using only features present
   in first-order DOT.

   # `higher` in first-order Scala

   We present two versions of Yallop's representation:

   - A "direct" translation using functional constructs.

   - A more object-oriented encoding.
 */

import hkt.direct._;

trait Apply1[F, A]:
  type This
  def prj(): This

case class IdW()

class IdH[A] private (x: A) extends Apply1[IdW, A]:
  type This = A
  override def prj(): This = x

/*
case class ListW()

def makeListHK[A](x: List[A]): Apply1.Aux[ListW, A, List[A]] = new Apply1[ListW, A]:
  type This = List[A]
  override def prj(): This = x
*/
