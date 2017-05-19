package net.readmarks.jsono;

import net.readmarks.utf8.Utf8Decoder;

import java.util.concurrent.atomic.AtomicLong;

public class Microbenchmark {

  public static void main(String[] args) {
    final long t1 = System.currentTimeMillis();
    final int N = 2000000;
    final String json = "[null,true,\"blUr \\u266b\" ,314e-2," +
            " {\"name\":[\"Cromwell\", \"jr.\", \"III\", \"Esq.\"]}," +
            " {\"name\":\"Elvis \\u266b\"}]";
    final byte[] sourceBytes = json.getBytes();
    final AtomicLong eventCount = new AtomicLong(0);
    for (int ll = 0; ll < N; ll++) {
      // Counter should normally be used in real applications so it's added here too.
      final JsonParser p = new JsonParser(new NestingCounter().andThen(event -> eventCount.incrementAndGet()));
      final Utf8Decoder utf8parser = new Utf8Decoder(p::parseNext);
      utf8parser.put(sourceBytes);
      utf8parser.end();
      p.end();
    }
    System.out.println("Parsed " + eventCount.get() + " events." +
            " Parse speed " + (1.0 * eventCount.get() / (System.currentTimeMillis() - t1)) + " events/mSec.");
  }
}
