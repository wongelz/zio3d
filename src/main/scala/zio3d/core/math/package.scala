package zio3d.core

package object math {

  def abs(x: Int): Int =
    java.lang.Math.abs(x)

  def sin(x: Float): Float =
    java.lang.Math.sin(x.toDouble).toFloat

  def cos(x: Float): Float =
    java.lang.Math.cos(x.toDouble).toFloat

  def tan(x: Float): Float =
    java.lang.Math.tan(x.toDouble).toFloat

  def toRadians(x: Float): Float =
    java.lang.Math.toRadians(x.toDouble).toFloat

  def sqrt(x: Float): Float =
    java.lang.Math.sqrt(x.toDouble).toFloat

  def min(a: Float, b: Float): Float =
    java.lang.Math.min(a, b)

  def max(a: Float, b: Float): Float =
    java.lang.Math.max(a, b)
}
