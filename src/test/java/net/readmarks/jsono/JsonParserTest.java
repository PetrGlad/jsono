package net.readmarks.jsono;

import net.readmarks.jsono.handler.StreamingHandler;
import org.junit.Test;

import java.util.stream.Stream;

import static net.readmarks.jsono.handler.Event.*;
import static org.junit.Assert.assertArrayEquals;

public class JsonParserTest {
  private static Object[] parse(String json) {
    final Stream.Builder<Object> result = Stream.builder();
    final JsonParser p = new JsonParser(new StreamingHandler(result::add));
    for (int i = 0; i < json.length(); i++) {
      p.parseNext(json.charAt(i));
    }
    p.end();
    return result.build().toArray();
  }

  @Test
  public void parseConstants() {
    assertArrayEquals(new Object[]{null}, parse("null"));
    assertArrayEquals(new Object[]{true}, parse("true"));
    assertArrayEquals(new Object[]{false}, parse("false"));
  }

  @Test
  public void skipWhitespace() {
    assertArrayEquals(new Object[]{false}, parse("   false\t"));
  }

  @Test
  public void parseArray() {
    assertArrayEquals(new Object[]{ARRAY, END}, parse("[]"));
    assertArrayEquals(new Object[]{ARRAY, END}, parse("[ ]"));
    assertArrayEquals(new Object[]{ARRAY, true, END}, parse("[true]"));
    assertArrayEquals(new Object[]{ARRAY, null, true, "blurB", END}, parse("[null ,true, \"blurB\"]"));
  }

  @Test
  public void parseMap() {
    assertArrayEquals(new Object[]{MAP, END}, parse("{}"));
    assertArrayEquals(
            new Object[]{
                    MAP,
                    KEY, "a", false,
                    END},
            parse("{\"a\": false}"));
    assertArrayEquals(
            new Object[]{
                    MAP,
                    KEY, "a", false,
                    KEY, "b", true,
                    END},
            parse("{\"a\": false, \"b\":true}"));
  }

  @Test
  public void parseNumbers() {
    assertArrayEquals(new Object[]{0L}, parse("0"));
    assertArrayEquals(new Object[]{4L}, parse("4"));
    assertArrayEquals(new Object[]{-12L}, parse("-12"));
    assertArrayEquals(new Object[]{-0.12}, parse("-0.12"));
    assertArrayEquals(new Object[]{1100.0}, parse("11e2"));
    assertArrayEquals(new Object[]{0.11}, parse("11E-2"));
    assertArrayEquals(new Object[]{1234321L}, parse("1234321"));
    assertArrayEquals(new Object[]{ARRAY, 1234L, END}, parse("[1234]"));
  }

  @Test(expected = JsonParser.ParseException.class)
  public void noCommas() {
    parse("[1 2 3]");
  }

  @Test(expected = JsonParser.ParseException.class)
  public void noCommas2() {
    parse("[\"\" \"\"]");
  }

  @Test
  public void parseNested() {
    assertArrayEquals(
            new Object[]{
                    MAP,
                    KEY, "k", ARRAY, END,
                    END},
            parse("{\"k\":[]}"));
    assertArrayEquals(
            new Object[]{
                    ARRAY,
                    MAP,
                    KEY, "k", ARRAY, END,
                    END,
                    END},
            parse("[{\"k\":[]}]"));
    // Number parser uses outer container or EOF as terminator.
    assertArrayEquals(
            new Object[]{
                    ARRAY,
                    321L,
                    END},
            parse("[321]"));
    assertArrayEquals(
            new Object[]{
                    MAP,
                    KEY, "a", 321L,
                    END},
            parse("{\"a\" : 321}"));
  }

  @Test(expected = JsonParser.ParseException.class)
  public void parseNestingError() {
    parse("[[{\"k\":[]}");
  }

  @Test
  public void testNumbers() {
    assertArrayEquals(new Object[]{0L}, parse("0"));
    assertArrayEquals(new Object[]{1.23e12}, parse("0.123e+13"));
  }

  @Test
  public void testStrings() {
    assertArrayEquals(new Object[]{""}, parse("\"\""));
    assertArrayEquals(new Object[]{" a"}, parse("\" a\""));
    assertArrayEquals(new Object[]{"\\%2"}, parse("\"\\\\%2\""));
    assertArrayEquals(new Object[]{"\" \\%22\""}, parse("\"\\u0022 \\\\%22\\\"\""));
    assertArrayEquals(new Object[]{"\b\f\n\r\t/\\"}, parse("\"\\b\\f\\n\\r\\t\\/\\\\\""));
    assertArrayEquals(new Object[]{"\u2615f"}, parse("\"\\u2615f\""));
    assertArrayEquals(new Object[]{"\u2615"}, parse("\"\\u2615\""));
    parse("\"\\n\"");
  }

  @Test(expected = JsonParser.ParseException.class)
  public void testControlSymbolsError() {
    parse("\"\n\"");
  }

  @Test(expected = JsonParser.ParseException.class)
  public void testIvalidCodepointEscape1() {
    parse("\"\\uw\"");
  }

  @Test(expected = JsonParser.ParseException.class)
  public void testIvalidCodepointEscape2() {
    parse("\"\\u111\"");
  }

  @Test(expected = JsonParser.ParseException.class)
  public void testIvalidCodepointEscape3() {
    parse("\"\\uaaaa");
  }
}
