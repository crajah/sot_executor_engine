package parallelai.sot.engine.generic.row

import shapeless.HList

object Syntax {

  implicit class nested[B](b : B) {
    def ->:[A](a : A) = Nested(a, b)
  }

  implicit class rename[A](a: A) {
    def **[B](b: B) = Rename(a, b)
  }

  val Col = shapeless.Witness

  def projector[A](a: A) = HList.apply(a)
  def projector[A, B](a: A, b: B) = HList.apply(a, b)
  def projector[A, B, C](a: A, b: B, c: C) = HList.apply(a, b, c)
  def projector[A, B, C, D](a: A, b: B, c: C, d: D) = HList.apply(a, b, c, d)
  def projector[A, B, C, D, E](a: A, b: B, c: C, d: D, e: E) = HList.apply(a, b, c, d, e)
  def projector[A, B, C, D, E, F](a: A, b: B, c: C, d: D, e: E, f: F) = HList.apply(a, b, c, d, e, f)
  def projector[A, B, C, D, E, F, G](a: A, b: B, c: C, d: D, e: E, f: F, g: G) = HList.apply(a, b, c, d, e, f, g)
  def projector[A, B, C, D, E, F, G, H](a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H) = HList.apply(a, b, c, d, e, f, g, h)
  def projector[A, B, C, D, E, F, G, H, I](a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I) = HList.apply(a, b, c, d, e, f, g, h, i)
  def projector[A, B, C, D, E, F, G, H, I, J](a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I, j: J) = HList.apply(a, b, c, d, e, f, g, h, i, j)
  def projector[A, B, C, D, E, F, G, H, I, J, K](a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I, j: J, k: K) = HList.apply(a, b, c, d, e, f, g, h, i, j, k)
  def projector[A, B, C, D, E, F, G, H, I, J, K, L](a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I, j: J, k: K, l: L) = HList.apply(a, b, c, d, e, f, g, h, i, j, k, l)
  def projector[A, B, C, D, E, F, G, H, I, J, K, L, M](a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I, j: J, k: K, l: L, m: M) = HList.apply(a, b, c, d, e, f, g, h, i, j, k, l, m)
  def projector[A, B, C, D, E, F, G, H, I, J, K, L, M, N](a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I, j: J, k: K, l: L, m: M, n: N) = HList.apply(a, b, c, d, e, f, g, h, i, j, k, l, m, n)
  def projector[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O](a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I, j: J, k: K, l: L, m: M, n: N, o: O) = HList.apply(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o)
  def projector[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P](a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I, j: J, k: K, l: L, m: M, n: N, o: O, p: P) = HList.apply(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p)
  def projector[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q](a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I, j: J, k: K, l: L, m: M, n: N, o: O, p: P, q: Q) = HList.apply(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q)
  def projector[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R](a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I, j: J, k: K, l: L, m: M, n: N, o: O, p: P, q: Q, r: R) = HList.apply(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r)
  def projector[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S](a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I, j: J, k: K, l: L, m: M, n: N, o: O, p: P, q: Q, r: R, s: S) = HList.apply(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s)
  def projector[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T](a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I, j: J, k: K, l: L, m: M, n: N, o: O, p: P, q: Q, r: R, s: S, t: T) = HList.apply(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t)
  def projector[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U](a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I, j: J, k: K, l: L, m: M, n: N, o: O, p: P, q: Q, r: R, s: S, t: T, u: U) = HList.apply(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t, u)
  def projector[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, V](a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I, j: J, k: K, l: L, m: M, n: N, o: O, p: P, q: Q, r: R, s: S, t: T, u: U, v: V) = HList.apply(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t, u, v)


}
