package net.readmarks.jsono;

import net.readmarks.jsono.JsonParser.Event;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static net.readmarks.jsono.JsonParser.Event.*;
import static org.junit.Assert.*;

public class JsonParserTest {
    static Object[] parse(String json) {
        final List<Object> result = new ArrayList<>();
        final JsonParser p = new JsonParser(result::add);
        for (int i = 0; i < json.length(); i++) {
            p.parseNext(json.charAt(i));
        }
        p.end();
        return result.toArray();
    }

    @Test
    public void parseConstants() {
        assertArrayEquals(new Object[]{null, END}, parse("null"));
        assertArrayEquals(new Object[]{true, END}, parse("true"));
        assertArrayEquals(new Object[]{false, END}, parse("false"));
    }

    @Test
    public void skipWhitespace() {
        assertArrayEquals(new Object[]{false, END}, parse("   false\t"));
    }

    @Test
    public void parseArray() {
        assertArrayEquals(new Object[]{ARRAY, ARRAY_END, END}, parse("[]"));
        assertArrayEquals(new Object[]{ARRAY, ARRAY_END, END}, parse("[ ]"));
        assertArrayEquals(new Object[]{ARRAY, true, ARRAY_END, END}, parse("[true]"));
        assertArrayEquals(new Object[]{ARRAY, null, true, "blurB", ARRAY_END, END}, parse("[null ,true, \"blurB\"]"));
    }

    @Test
    public void parseMap() {
        assertArrayEquals(new Object[]{MAP, MAP_END, END}, parse("{}"));
        assertArrayEquals(
                new Object[]{
                        MAP,
                        MAP_KEY, "a", Event.MAP_VALUE, false,
                        MAP_END,
                        END},
                parse("{\"a\": false}"));
    }

    @Test
    public void parseNumbers() {
        assertArrayEquals(new Object[]{0L, END}, parse("0"));
        assertArrayEquals(new Object[]{-12L, END}, parse("-12"));
        assertArrayEquals(new Object[]{1234321L, END}, parse("1234321"));
        assertArrayEquals(new Object[]{ARRAY, 1234L, ARRAY_END, END}, parse("[1234]"));
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
                        MAP_KEY, "k", MAP_VALUE, ARRAY, ARRAY_END,
                        MAP_END,
                        END},
                parse("{\"k\":[]}"));
        assertArrayEquals(
                new Object[]{
                        ARRAY,
                        MAP,
                        MAP_KEY, "k", MAP_VALUE, ARRAY, ARRAY_END,
                        MAP_END,
                        ARRAY_END,
                        END},
                parse("[{\"k\":[]}]"));
        // Number parser uses outer container or EOF as terminator.
        assertArrayEquals(
                new Object[]{
                        ARRAY,
                        321L,
                        ARRAY_END,
                        END},
                parse("[321]"));
        assertArrayEquals(
                new Object[]{
                        MAP,
                        MAP_KEY,
                        "a",
                        MAP_VALUE,
                        321L,
                        MAP_END,
                        END},
                parse("{\"a\" : 321}"));
    }

    @Test(expected = JsonParser.ParseException.class)
    public void parseNestingError() {
        parse("[[{\"k\":[]}");
    }

    @Test
    public void testNumbers() {
        assertArrayEquals(new Object[]{0L, END}, parse("0"));
        assertArrayEquals(new Object[]{1.23e12, END}, parse("0.123e+13"));
    }

    @Test
    public void testStrings() {
        assertArrayEquals(new Object[]{" a", END}, parse("\" a\""));
        assertArrayEquals(new Object[]{"\\%2", END}, parse("\"\\\\%2\""));
        assertArrayEquals(new Object[]{"\" \\%22\"", END}, parse("\"\\u0022 \\\\%22\\\"\""));
        assertArrayEquals(new Object[]{"\b\f\n\r\t/\\", END}, parse("\"\\b\\f\\n\\r\\t\\/\\\\\""));
        assertArrayEquals(new Object[]{"\u2615f", END}, parse("\"\\u2615f\""));
        assertArrayEquals(new Object[]{"\u2615", END}, parse("\"\\u2615\""));
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
