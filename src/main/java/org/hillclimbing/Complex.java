package org.hillclimbing;

final class Complex {

  final double real;
  final double imaginary;

  Complex(double real, double imaginary) {
    this.real = real;
    this.imaginary = imaginary;
  }

  Complex divideBy(double divisor) {
    return new Complex(real / divisor, imaginary / divisor);
  }

  double abs() {
    return Math.sqrt(real * real + imaginary * imaginary);
  }

  Complex divideBy(Complex divisor) {
    double denominator = divisor.real * divisor.real + divisor.imaginary * divisor.imaginary;
    return new Complex(
      (real * divisor.real + imaginary * divisor.imaginary) / denominator,
      (-real * divisor.imaginary + imaginary * divisor.real) / denominator);
  }

  Complex multiplyBy(Complex multiplier) {
    return new Complex(real * multiplier.real - imaginary * multiplier.imaginary,
      real * multiplier.imaginary + imaginary * multiplier.real);
  }

  Complex minus(Complex complex) {
    return new Complex(real - complex.real, imaginary - complex.imaginary);
  }

  static Complex zero() {
    return new Complex(0, 0);
  }

  static Complex real(double real) {
    return new Complex(real, 0);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Complex complex = (Complex) o;

    if (Double.compare(complex.real, real) != 0) return false;
    return Double.compare(complex.imaginary, imaginary) == 0;
  }

  @Override
  public int hashCode() {
    int result;
    long temp;
    temp = Double.doubleToLongBits(real);
    result = (int) (temp ^ (temp >>> 32));
    temp = Double.doubleToLongBits(imaginary);
    result = 31 * result + (int) (temp ^ (temp >>> 32));
    return result;
  }

  @Override
  public String toString() {
    return real + " + i * " + imaginary;
  }
}
