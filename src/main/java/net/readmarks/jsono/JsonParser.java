package net.readmarks.jsono;

import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * Incremental non blocking JSON parser. Emits parsed events synchronously when enough text is provided.
 * Instantiates scalar types (null, boolean, number) and strings.
 * Containers (arrays and maps) are emitted  as sequence of events.
 * <p>
 * Array is represented as ARRAY, value1, value2, ..., ARRAY_END
 * <p>
 * Map/object is represented as MAP, key1, value1, key2, value2, ... , MAP_END
 * <p>
 * Event END is emitted at the end of well formed input.
 * <p>
 * The parser accepts top-level sequence of documents/values (possibly whitespace delimited).
 * That is this input text "{} {}" will produce MAP, MAP_END, MAP, MAP_END, END
 * <p>
 * Example usage is
 * <pre>
 *   final String json = "[null,true,\"blUr \\u266b\" ,314e-2]";
 *   final JsonParser p = new JsonParser(System.out::println); // Handle events in this callback
 *   for (int i = 0; i < json.length(); i++) {
 *     p.parseNext(json.charAt(i));
 *   }
 *   p.end(); // Call this when there's no more input data to ensure that input is well-formed.
 * </pre>
 * <p>
 * Implementation questions
 * <p>
 * TODO Can we somehow typecheck emitted events?
 * Downstream consumers accepting Object are confusing.
 * I'd want to include Event and primitive values into the type.
 * A solution could be to have wrappers for scalar types that implement same interface.
 * Would that cause problem with garbage?
 *
 * TODO Parse states: Is it worthwhile to rewrite this class using explicit stack instead of chain of objects
 * that refer their parents? Or use pool for these objects? Would this reduce amount of garbage?
 *
 * TODO Implement an existing API (see reactive-streams) in handlers
 *
 * @see NestingCounter
 */
public class JsonParser {

  public enum Event {
    ARRAY,
    ARRAY_END,
    MAP,
    MAP_END,
    END
  }

  public static class ParseException extends RuntimeException {
    ParseException(String s) {
      super(s);
    }
  }

  abstract static class SElement {
    final SElement parent;

    SElement(SElement parent) {
      this.parent = parent;
    }

    public SElement parse(char ch) {
      if (Character.isWhitespace(ch)) {
        return this;
      } else {
        return null;
      }
    }

    public SElement end() {
      throw new ParseException("Unexpected end of input in " + getClass().getSimpleName());
    }
  }

  class SDoc extends SElement {
    SDoc() {
      super(null);
    }

    @Override
    public SElement parse(char ch) {
      return new SValue(this).parse(ch);
    }

    @Override
    public SElement end() {
      eventSink.accept(Event.END);
      return this;
    }
  }

  class SValue extends SElement {
    SValue(SElement parent) {
      super(parent);
    }

    @Override
    public SElement parse(char ch) {
      final SElement result = super.parse(ch);
      if (result != null) {
        return result;
      } else if (ch == '{') {
        return new SObject(parent);
      } else if (ch == '[') {
        return new SArray(parent);
      } else if (ch == '"') {
        return new SString(parent);
      } else if (ch == '-' || Character.isDigit(ch)) {
        return new SNumber(parent).parse(ch);
      } else if (ch == 't') {
        return new SConst(parent, "true", Boolean.TRUE).parse(ch);
      } else if (ch == 'f') {
        return new SConst(parent, "false", Boolean.FALSE).parse(ch);
      } else if (ch == 'n') {
        return new SConst(parent, "null", null).parse(ch);
      } else {
        return parent.parse(ch);
      }
    }

    @Override
    public SElement end() {
      return parent.end();
    }
  }

  class SConst extends SElement {
    final String valueString;
    final Object value;
    int pos = 0;

    SConst(SElement sParent, String valueString, Object value) {
      super(sParent);
      this.valueString = valueString;
      this.value = value;
    }

    @Override
    public SElement parse(char ch) {
      if (ch == valueString.charAt(pos)) {
        if (pos == valueString.length() - 1) {
          eventSink.accept(value);
          return parent;
        } else {
          pos++;
          return this;
        }
      } else {
        throw new ParseException("Unexpected char " + ch);
      }
    }
  }

  private static Pattern NUMBER_FORMAT = Pattern.compile("-?(0|[1-9][0-9]+)(\\.[0-9]+)?([eE][-+]?[0-9]+)?");

  class SNumber extends SElement {
    final private StringBuilder stringValue = new StringBuilder();
    private boolean isDouble = false;

    SNumber(SElement sParent) {
      super(sParent);
    }

    @Override
    public SElement parse(char ch) {
      if (stringValue.length() == 0) {
        final SElement result = super.parse(ch);
        if (result != null) {
          return result;
        }
      }
      if (Character.isDigit(ch) || ch == '-' || ch == '+') {
        stringValue.append(ch);
        return this;
      } else if (ch == '.' || ch == 'e' || ch == 'E') {
        isDouble = true;
        stringValue.append(ch);
        return this;
      } else {
        complete();
        return parent.parse(ch);
      }
    }

    @Override
    public SElement end() {
      complete();
      return parent.end();
    }

    private void complete() {
      if (!NUMBER_FORMAT.matcher(stringValue).matches()) {
        throw new ParseException(
                "'" + stringValue + "' does not match number format" +
                        " '" + NUMBER_FORMAT.pattern() + "'");
      }
      if (isDouble) {
        eventSink.accept(Double.parseDouble(stringValue.toString()));
      } else {
        eventSink.accept(Long.parseLong(stringValue.toString()));
      }
    }
  }

  class SString extends SElement {
    final StringBuilder value = new StringBuilder();

    SString(SElement sParent) {
      super(sParent);
    }

    @Override
    public SElement parse(char ch) {
      if (ch == '"') {
        eventSink.accept(value.toString());
        return parent;
      } else if (ch == '\\') {
        return new SStringEscape(this);
      } else if (ch <= 0x1f) {
        throw new ParseException("Unexpected character within string " + Integer.toHexString((int) ch) + "."
                + " Control characters in range U+0000 to U+001F must be escaped.");
      } else {
        value.append(ch);
        return this;
      }
    }
  }

  class SStringEscape extends SElement {
    private StringBuilder codeString;

    public SStringEscape(SString sParent) {
      super(sParent);
    }

    private char singleEscapeChar(char nextChar) {
      switch (nextChar) {
        case '"':
          return '"';
        case '\\':
          return '\\';
        case '/':
          return '/';
        case 'b':
          return '\b';
        case 'f':
          return '\f';
        case 'n':
          return '\n';
        case 'r':
          return '\r';
        case 't':
          return '\t';
        case 'u':
          return 'u';
        default:
          throw new IllegalStateException("Unexpected escape '" + nextChar + "'");
      }
    }

    @Override
    public SElement parse(char ch) {
      if (codeString == null) {
        char eCh = singleEscapeChar(ch);
        if (eCh == 'u') {
          codeString = new StringBuilder(5);
          return this;
        } else {
          ((SString) parent).value.append(eCh);
          return parent;
        }
      } else if (codeString.length() < 3) {
        codeString.append(ch);
        return this;
      } else {
        codeString.append(ch);
        ((SString) parent).value.appendCodePoint(parseCodePoint());
        return parent;
      }
    }

    private int parseCodePoint() {
      try {
        return Integer.parseInt(codeString.toString(), 16);
      } catch (NumberFormatException e) {
        throw new ParseException("Invalid escape code point format '" + codeString.toString() + "'." +
                " Expecting exactly 4 hexadecimal digits.");
      }
    }
  }

  private enum SObjectState {
    START,
    KEY,
    COLON,
    VALUE
  }

  class SObject extends SElement {
    private SObjectState sObjectState = SObjectState.START;

    SObject(SElement sParent) {
      super(sParent);
      eventSink.accept(Event.MAP);
    }

    @Override
    public SElement parse(char ch) {
      final SElement result = super.parse(ch);
      if (result != null) {
        return result;
      } else if ((sObjectState == SObjectState.START || sObjectState == SObjectState.VALUE) && ch == '}') {
        eventSink.accept(Event.MAP_END);
        return parent;
      } else if ((sObjectState == SObjectState.START || sObjectState == SObjectState.KEY) && ch == '"') {
        sObjectState = SObjectState.COLON;
        return new SString(this);
      } else if (sObjectState == SObjectState.COLON && ch == ':') {
        sObjectState = SObjectState.VALUE;
        return new SValue(this);
      } else if (sObjectState == SObjectState.VALUE && ch == ',') {
        sObjectState = SObjectState.KEY;
        return this;
      } else {
        throw new ParseException("Unexpected character '" + ch + "'");
      }
    }
  }

  class SArray extends SElement {
    private boolean expectComma = false;

    SArray(SElement sParent) {
      super(sParent);
      eventSink.accept(Event.ARRAY);
    }

    @Override
    public SElement parse(char ch) {
      final SElement result = super.parse(ch);
      if (result != null) {
        return result;
      } else if (ch == ']') {
        eventSink.accept(Event.ARRAY_END);
        return parent;
      } else if (expectComma) {
        if (ch != ',') {
          throw new ParseException("Expecting comma or array end, got '" + ch + "'");
        }
        expectComma = false;
        return this;
      } else {
        expectComma = true;
        return new SValue(this).parse(ch);
      }
    }
  }

  private final Consumer<Object> eventSink;
  private SElement state = new SDoc();

  public JsonParser(Consumer<Object> eventSink) {
    this.eventSink = eventSink;
  }

  /**
   * Call this once for every subsequent input's chars.
   *
   * @param ch next input chars.
   */
  public void parseNext(final char ch) {
    state = state.parse(ch);
  }

  /**
   * Call this to indicate that there will be no more input.
   * This method allows to detect incomplete JSON documents.
   */
  public void end() {
    state = state.end();
  }
}
