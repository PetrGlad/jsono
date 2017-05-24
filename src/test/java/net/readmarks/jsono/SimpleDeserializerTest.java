package net.readmarks.jsono;

import net.readmarks.jsono.handler.SimpleDeserializer;
import net.readmarks.utf8.Utf8Decoder;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SimpleDeserializerTest {
  @Test
  public void deserializeTest() {
    final JsonParser jsonParser = JsonParser.makeDefault(
            new SimpleDeserializer(struct ->
            {
              // System.out.println("Result: " + struct);
              if (!getExpectedResult().equals(struct)) {
                throw new AssertionError("Deserialized struct does not match expected one.");
              }
            }));
    final Utf8Decoder utf8decoder = new Utf8Decoder(jsonParser::parseNext);
    utf8decoder.put(("[null,true,\"blUr \\u266b\" ,314e-2," +
            " {\"full-name\":[\"Thomas Thumbson\", \"jr.\", \"III\", \"Esq.\"]}," +
            " {\"name\":\"Elvis \\u266b\"}]").getBytes());
    utf8decoder.end();
    jsonParser.end();
  }

  private static List<Object> getExpectedResult() {
    final List<Object> result = new ArrayList<>();
    result.add(null);
    result.add(true);
    result.add("blUr ♫");
    result.add(3.14);
    {
      final List<String> names = new ArrayList<>();
      names.add("Thomas Thumbson");
      names.add("jr.");
      names.add("III");
      names.add("Esq.");
      final Map<String, Object> map1 = new HashMap<>();
      map1.put("full-name", names);
      result.add(map1);
    }
    {
      final Map<String, Object> map2 = new HashMap<>();
      map2.put("name", "Elvis ♫");
      result.add(map2);
    }
    return result;
  }

  /**
   * MicroBenchmark
   */
  public static void main(String[] args) {
    final long t1 = System.currentTimeMillis();
    final int N = 2000000;
    for (int ll = 0; ll < N; ll++) {
      new SimpleDeserializerTest().deserializeTest();
    }
    System.out.println("Completed: " + (1.0 * N / (System.currentTimeMillis() - t1)) + " structs/mSec.");
  }
}
