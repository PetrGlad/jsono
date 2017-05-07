package net.readmarks.jsono;

import java.util.function.Consumer;

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

    public class ParseException extends RuntimeException {
        ParseException(String s) {
            super(s);
        }
    }

    abstract static class SElement {
        final SElement parent;

        SElement() {
            this(null);
        }

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
                return new SObject(this);
            } else if (ch == '[') {
                return new SArray(this);
            } else if (ch == '"') {
                return new SString(this);
            } else if (ch == '-' || Character.isDigit(ch)) {
                return new SNumber(this).parse(ch);
            } else if (ch == 't') {
                return new SConst(this, "true", Boolean.TRUE).parse(ch);
            } else if (ch == 'f') {
                return new SConst(this, "false", Boolean.FALSE).parse(ch);
            } else if (ch == 'n') {
                return new SConst(this, "null", null).parse(ch);
            } else {
                return parent.parse(ch);
            }
        }
    }

    class SConst extends SElement {
        final String valueString;
        final Object value;
        int pos = 0;

        SConst(SValue sValue, String valueString, Object value) {
            super(sValue);
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

    class SNumber extends SElement {
        final private StringBuilder value = new StringBuilder();

        SNumber(SValue sValue) {
            super(sValue);
        }

        @Override
        public SElement parse(char ch) {
            final SElement result = super.parse(ch);
            if (result != null) {
                return result;
            } else if (Character.isDigit(ch)) {
                value.append(ch);
                return this;
            } else {
                return end().parse(ch);
            }
        }

        @Override
        public SElement end() {
            eventSink.accept(Long.parseLong(value.toString()));
            return parent;
        }
    }

    class SString extends SElement {
        final private StringBuilder value = new StringBuilder();

        SString(SElement sValue) {
            super(sValue);
        }

        @Override
        public SElement parse(char ch) {
            final SElement result = super.parse(ch);
            if (result != null) {
                return result;
            } else if (ch == '"') {
                eventSink.accept(value.toString());
                return parent;
            } else if (ch == '\\') {
                throw new RuntimeException("Escape parsing is not implemented."); // FIXME Parse escapes
            } else {
                value.append(ch);
                return this;
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

        SObject(SValue sValue) {
            super(sValue);
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
        private boolean isFirst = true;

        SArray(SValue sValue) {
            super(sValue);
            eventSink.accept(Event.START_ARRAY);
        }

        @Override
        public SElement parse(char ch) {
            final SElement result = super.parse(ch);
            if (result != null) {
                return result;
            } else if (!isFirst && ch == ',') {
                return new SValue(this);
            } else if (ch == ']') {
                eventSink.accept(Event.END_ARRAY);
                return parent;
            } else {
                isFirst = false;
                return new SValue(this).parse(ch);
            }
        }
    }

    private final Consumer<Object> eventSink;
    private SElement state;

    public JsonParser(Consumer<Object> eventSink) {
        this.eventSink = eventSink;
    }

    /**
     * Call this once for every subsequent input's chars.
     *
     * @param ch next input chars.
     */
    public void parseNext(final char ch) {
        if (state == null)
            state = new SValue(null);
        state = state.parse(ch);
    }

    /**
     * Call this to indicate that there will be no more input.
     * This method allows to detect incomplete JSON documents.
     */
    public void end() {
        state = state.end();
        if (state instanceof SValue) {
            eventSink.accept(Event.END);
        } else {
            throw new ParseException("Premature end of JSON document. State is " + state.getClass().getSimpleName());
        }
    }
}
