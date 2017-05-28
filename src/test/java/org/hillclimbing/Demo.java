package org.hillclimbing;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.List;
import java.util.Random;

public class Demo {

  public static void main(String[] args) throws Exception {
    // Can be pretty-printed by R/gnuplot/phantom/whatever
    run(new DemoRunner(new Random(314), true, true), args[0]);
    run(new DemoRunner(new Random(239), false, true), args[1]);
  }

  private static void run(DemoRunner demoRunner, String file) throws Exception {
    try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
      writer.write("Time,Throughput,Threads\n");
      List<String> samples = demoRunner.run();
      for (String sample : samples) {
        writer.write(sample + "\n");
      }
    }
  }
}
