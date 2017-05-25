package net.readmarks.jsono.handler;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonStreamContext;
import net.readmarks.jsono.EventHandler;
import net.readmarks.jsono.PrintJson;

import java.io.IOException;

/**
 * Converts JsonParser events into Jackson's JSON generator calls.
 *
 * @see PrintJson
 */
public class EventToGenerator implements EventHandler {

  private final JsonGenerator generator;

  public EventToGenerator(JsonGenerator generator) {
    this.generator = generator;
  }

  @Override
  public void onValue(Object x) {
    try {
      final JsonStreamContext ctx = generator.getOutputContext();
      if (x instanceof String
              && ctx.inObject()
              && ctx.getCurrentName() == null) {
        generator.writeFieldName((String) x);
      } else {
        generator.writeObject(x);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void onArray() {
    try {
      generator.writeStartArray();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void onMap() {
    try {
      generator.writeStartObject();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void onEnd() {
    try {
      final JsonStreamContext ctx = generator.getOutputContext();
      if (ctx.inArray()) {
        generator.writeEndArray();
      } else if (ctx.inObject()) {
        generator.writeEndObject();
      } else if (ctx.inRoot()) {
        generator.close();
      } else {
        throw new IllegalStateException("Unmatched onEnd call. context=" + ctx);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
