/* # Lightweight higher-kinded polymorphism in Scala

   This is an implementation of "Lightweight higher-kinded polymorphism" by
   Jeremy Yallop and Leo White from FLOPS 2014.

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

   One dissatisfying thing about Yallop's solution is its requirement of an
   unsafe cast within the implementation of the `Newtype1` functor. While it is
   meta-theoretically safe due to encapsulation and generative properties of
   OCaml's module system, it would be preferable to have an entirely native
   encoding.

   The core difficulty in representing this encoding in pure OCaml is in the
   definition of the type `app`. Classically, defunctionalisation is a
   whole-program transformation, in which a single dispatch function handles
   every higher-order function usage. But this is obviously not very modular,
   as it would require knowing the identity of every higher-kinded type
   constructor in advance. Ideally, the `app` type would be extensible, adding
   a new variant for each newly-discovered higher-kinded instance.

   Enter traits and associated types.
*/

trait Apply1[F, A]:
  type This
  def prj(): This

object Apply1:
  type Aux[F, A, This0] = Apply1[F, A] { type This = This0 }

case class IdW()

def makeIdHK[A](x: A): Apply1.Aux[IdW, A, A] = new Apply1[IdW, A]:
  type This = A
  override def prj(): This = x

case class ListW()

def makeListHK[A](x: List[A]): Apply1.Aux[ListW, A, List[A]] = new Apply1[ListW, A]:
  type This = List[A]
  override def prj(): This = x
