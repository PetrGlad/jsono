package net.readmarks.jsono;

import java.util.function.Consumer;
import java.util.regex.Pattern;

public class JsonParser {

    public enum Event {
        END,
        START_ARRAY,
        END_ARRAY,
        START_MAP,
        END_MAP,
        MAP_KEY,
        MAP_VALUE
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
        final private StringBuilder value = new StringBuilder();

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
            } else {
                value.append(ch);
                return this;
            }
        }

        void appendCodePoint(int codePoint) {
            value.appendCodePoint(codePoint);
        }

        void appendChar(char ch) {
            value.append(ch);
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
                    ((SString) parent).appendChar(eCh);
                    return parent;
                }
            } else if (codeString.length() < 3) {
                codeString.append(ch);
                return this;
            } else {
                codeString.append(ch);
                ((SString) parent).appendCodePoint(
                        Integer.parseInt(codeString.toString(), 16));
                return parent;
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
            eventSink.accept(Event.START_MAP);
        }

        @Override
        public SElement parse(char ch) {
            final SElement result = super.parse(ch);
            if (result != null) {
                return result;
            } else if ((sObjectState == SObjectState.START || sObjectState == SObjectState.VALUE) && ch == '}') {
                eventSink.accept(Event.END_MAP);
                return parent;
            } else if ((sObjectState == SObjectState.START || sObjectState == SObjectState.KEY) && ch == '"') {
                sObjectState = SObjectState.COLON;
                eventSink.accept(Event.MAP_KEY);
                return new SString(this);
            } else if (sObjectState == SObjectState.COLON && ch == ':') {
                sObjectState = SObjectState.VALUE;
                eventSink.accept(Event.MAP_VALUE);
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
            eventSink.accept(Event.START_ARRAY);
        }

        @Override
        public SElement parse(char ch) {
            final SElement result = super.parse(ch);
            if (result != null) {
                return result;
            } else if (ch == ']') {
                eventSink.accept(Event.END_ARRAY);
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
