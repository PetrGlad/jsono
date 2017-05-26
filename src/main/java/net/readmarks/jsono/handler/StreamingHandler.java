package net.readmarks.jsono.handler;

import net.readmarks.jsono.EventHandler;

import java.util.function.Consumer;

/**
 * Turns calls to EventHandler into sequence of JSON event objects.
 * Primitive values are passed as is, nested structures are delimited by {@link Event} values.
 */
public class StreamingHandler implements EventHandler {
  private final Consumer<Object> sink;

  public StreamingHandler(Consumer<Object> sink) {
    assert sink != null;
    this.sink = sink;
  }

  @Override
  public void onValue(Object x) {
    sink.accept(x);
  }

  @Override
  public void onArray() {
   sink.accept(Event.ARRAY);
  }

  @Override
  public void onMap() {
    sink.accept(Event.MAP);
  }

  @Override
  public void onMapKey() {
    sink.accept(Event.KEY);
  }

  @Override
  public void onEnd() {
    sink.accept(Event.END);
  }
}
