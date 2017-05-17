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
        assertArrayEquals(new Object[]{START_ARRAY, END_ARRAY, END}, parse("[]"));
        assertArrayEquals(new Object[]{START_ARRAY, END_ARRAY, END}, parse("[ ]"));
        assertArrayEquals(new Object[]{START_ARRAY, true, END_ARRAY, END}, parse("[true]"));
        assertArrayEquals(new Object[]{START_ARRAY, null, true, "blurB", END_ARRAY, END}, parse("[null ,true, \"blurB\"]"));
    }

    @Test
    public void parseMap() {
        assertArrayEquals(new Object[]{START_MAP, END_MAP, END}, parse("{}"));
        assertArrayEquals(
                new Object[]{
                        START_MAP,
                        MAP_KEY, "a", Event.MAP_VALUE, false,
                        END_MAP,
                        END},
                parse("{\"a\": false}"));
    }

    @Test
    public void parseNumbers() {
        assertArrayEquals(new Object[]{0L, END}, parse("0"));
        assertArrayEquals(new Object[]{-12L, END}, parse("-12"));
        assertArrayEquals(new Object[]{1234321L, END}, parse("1234321"));
        assertArrayEquals(new Object[]{START_ARRAY, 1234L, END_ARRAY, END}, parse("[1234]"));
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
                        START_MAP,
                        MAP_KEY, "k", MAP_VALUE, START_ARRAY, END_ARRAY,
                        END_MAP,
                        END},
                parse("{\"k\":[]}"));
        assertArrayEquals(
                new Object[]{
                        START_ARRAY,
                        START_MAP,
                        MAP_KEY, "k", MAP_VALUE, START_ARRAY, END_ARRAY,
                        END_MAP,
                        END_ARRAY,
                        END},
                parse("[{\"k\":[]}]"));
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
    }
}
