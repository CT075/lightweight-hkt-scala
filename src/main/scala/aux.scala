trait Apply1[F, A]:
  type This
  def prj(): This

object Apply1:
  type Aux[F, A, This0] = Apply1[F, A] { type This = This0 }

trait Apply2[F, A, B]:
  type This
  def prj(): This

case class IdW()

def makeIdHK[A](x: A): Apply1.Aux[IdW, A, A] = new Apply1[IdW, A] {
  type This = A
  override def prj(): This = x
}

case class ListW()

def makeListHK[A](x: List[A]): Apply1.Aux[ListW, A, List[A]] = new Apply1[ListW, A] {
  type This = List[A]
  override def prj(): This = x
}
