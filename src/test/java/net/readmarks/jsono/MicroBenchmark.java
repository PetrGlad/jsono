package net.readmarks.jsono;

import com.fasterxml.jackson.core.JsonGenerator;
import net.readmarks.jsono.handler.StreamingHandler;
import net.readmarks.utf8.Utf8Decoder;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
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

//    String json = loadFile("./src/test/resources/sample.json");

    repeatedlyParse(json, 2000000);
  }

  private static String loadFile(String filePath) {
    try {
      return new String(Files.readAllBytes(Paths.get(filePath)));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
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
    repeatedlyParse(jsonStringWriter.getBuffer().toString(),
            50);
  }

  private static void repeatedlyParse(String json, int iterations) {
    final byte[] sourceBytes = json.getBytes();
    final AtomicLong eventCount = new AtomicLong(0);
    final long t1 = System.currentTimeMillis();
    for (int ll = 0; ll < iterations; ll++) {
      // Counter should normally be used in real applications so it's added here too.
      final JsonParser p = JsonParser.makeDefault(
              new StreamingHandler(event -> eventCount.incrementAndGet()));
      final Utf8Decoder utf8parser = new Utf8Decoder(p::parseNext);
      utf8parser.put(sourceBytes);
      utf8parser.end();
      p.end();
    }
    long elapsed = System.currentTimeMillis() - t1;
    System.out.println("\n" + iterations + " iterations, " + (1e3 * elapsed / iterations) + " uSec/document.");
    System.out.println("Parsed " + eventCount.get() + " events."
            + "\n" + (1.0 * eventCount.get() / elapsed) + " events/mSec"
            + "\n" + (iterations * sourceBytes.length / elapsed / 1024.0)+ " kB/mSec.");
  }
}
