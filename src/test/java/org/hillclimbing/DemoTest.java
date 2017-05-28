package org.hillclimbing;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;

/**
 * Pretty strange test:
 * Original hill-climbing from CoreCLR has no tests at all (rly?).
 * I don't know obvious ways to test that local search (which hill climbing basically is) *actually* works.
 * Tests that on fixed input fixed output is produced are pointless: without fuzzers/coverage it's hard to
 * tell whether most of code-paths are tested or not.
 * <p>
 * So approach is following:
 * After implementing {@link Demo} and validating that algorithm works (via plotting demo results),
 * fix seed of random generators and verify that same long-running demo produces same results to be
 * sure that next refactoring will not break anything.
 */
public class DemoTest {

  @Test
  public void testSmooth() throws IOException {
    checkResults(new DemoRunner(new Random(239), false, false), "smooth.csv");
  }

  @Test
  public void testSmoothWithDisturbance() throws IOException {
    checkResults(new DemoRunner(new Random(314), true, false), "smooth-with-disturbance.csv");
  }

  private void checkResults(DemoRunner demoRunner, String resourceName) throws IOException {
    InputStream stream = DemoTest.class.getResourceAsStream("/" + resourceName);
    BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
    List<String> expectedResult = new ArrayList<>();
    String s;
    while ((s = reader.readLine()) != null) {
      expectedResult.add(s);
    }

    assertEquals(expectedResult, demoRunner.run());
  }
}
