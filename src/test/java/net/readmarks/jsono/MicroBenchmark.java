package net.readmarks.jsono;

import com.fasterxml.jackson.core.JsonGenerator;
import net.readmarks.jsono.handler.StreamingHandler;
import net.readmarks.utf8.Utf8Decoder;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

public class MicroBenchmark {

  public static void main(String[] args) throws IOException {
    mixedStructTest();
    deserializeStringsTest();
  }

  private static void mixedStructTest() {
    final String json = "[null,true,\"blUr \\u266b\" ,314e-2," +
            " {\"name\":[\"Thomas Thumbson\", \"jr.\", \"III\", \"Esq.\"]}," +
            " {\"name\":\"Elvis \\u266b\"}]";
    final byte[] sourceBytes = json.getBytes();
    final AtomicLong eventCount = new AtomicLong(0);
    final int N = 2000000;
    final long t1 = System.currentTimeMillis();
    for (int ll = 0; ll < N; ll++) {
      // Counter should normally be used in real applications so it's added here too.
      final JsonParser p = makeJsonParser(eventCount);
      final Utf8Decoder utf8parser = new Utf8Decoder(p::parseNext);
      utf8parser.put(sourceBytes);
      utf8parser.end();
      p.end();
    }
    System.out.println("Parsed " + eventCount.get() + " events." +
            " Parse speed " + (1.0 * eventCount.get() / (System.currentTimeMillis() - t1)) + " events/mSec.");
  }

  private static void deserializeStringsTest() throws IOException {
    final int stringMb = 2;
    final int n = 1024 * 1024 * stringMb;
    final StringBuilder sb = new StringBuilder(n);
    {
      final Random rand = new Random(0xDEADBEEF);
      for (int i = 0; i < n; i++) {
        sb.append((char) rand.nextInt(Character.MAX_VALUE));
      }
    }

    final StringWriter jsonStringWriter = new StringWriter();
    {
      JsonGenerator generator = PrintJson.defaultGenerator(jsonStringWriter);
      generator.writeString(sb.toString());
      generator.close();
    }

    final String json = jsonStringWriter.getBuffer().toString();
    final byte[] sourceBytes = json.getBytes();
    final AtomicLong eventCount = new AtomicLong(0);
    final int N = 50;
    final long t1 = System.currentTimeMillis();
    for (int ll = 0; ll < N; ll++) {
      // Counter should normally be used in real applications so it's added here too.
      final JsonParser p = makeJsonParser(eventCount);
      final Utf8Decoder utf8parser = new Utf8Decoder(p::parseNext);
      utf8parser.put(sourceBytes);
      utf8parser.end();
      p.end();
    }
    System.out.println("Parsed " + eventCount.get() + " events." +
            " Parse speed " + (stringMb * N * 1000.0  / (System.currentTimeMillis() - t1)) + " Mb/Sec.");
  }

  private static JsonParser makeJsonParser(AtomicLong eventCount) {
    return JsonParser.makeDefault(
            new StreamingHandler(event -> eventCount.incrementAndGet()));
  }
}
