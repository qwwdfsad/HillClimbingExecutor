package org.hillclimbing;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ComplexTest {

  @Test
  public void testAbs() {
    assertEquals(4, Complex.real(4).abs(), 1e-8);
    assertEquals(5, new Complex(3, 4).abs(), 1e-8);
    assertEquals(1.41421, new Complex(1, 1).abs(), 1e-5);
  }

  @Test
  public void testDivideBy() {
    assertEquals(Complex.real(2), Complex.real(4).divideBy(2));
    assertEquals(Complex.real(2), Complex.real(4).divideBy(Complex.real(2)));
    assertEquals(new Complex(1.5, -1), new Complex(5, 1).divideBy(new Complex(2, 2)));
  }

  @Test
  public void testMultiplyBy() {
    assertEquals(new Complex(2, 2), new Complex(1, 1).multiplyBy(Complex.real(2)));
    assertEquals(new Complex(-5, 10), new Complex(1, 2).multiplyBy(new Complex(3, 4)));
  }

  @Test
  public void testMinus() {
    assertEquals(Complex.real(2), Complex.real(5).minus(Complex.real(3)));
    assertEquals(new Complex(2, 3), new Complex(7, 7).minus(new Complex(5, 4)));
  }
}