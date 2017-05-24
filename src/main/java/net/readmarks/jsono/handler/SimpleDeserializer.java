package net.readmarks.jsono.handler;

import net.readmarks.jsono.EventHandler;
import net.readmarks.jsono.JsonParser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Converts series of JSON events into Java data structures.
 * JSON objects are represented as {@link HashMap}s, JSON arrays as {@link ArrayList}s.
 *
 * @see JsonParser
 */
public class SimpleDeserializer implements EventHandler {
  abstract class State implements Consumer<Object>, Supplier<Object> {
    final State parent;

    protected State(State parent) {
      this.parent = parent;
    }

    public State getParent() {
      if (parent == null) {
        throw new UnsupportedOperationException("Unbalanced onEnd() call.");
      }
      return parent;
    }
  }

  class MapValue extends State {
    final Map<String, Object> result = new HashMap<>();
    private String key;

    MapValue(State parent) {
      super(parent);
    }

    @Override
    public void accept(Object o) {
      if (key == null) {
        key = (String) o;
      } else {
        result.put(key, o);
        key = null;
      }
    }

    @Override
    public Object get() {
      return result;
    }
  }

  class ArrayValue extends State {
    final List<Object> result = new ArrayList<>();

    ArrayValue(State parent) {
      super(parent);
    }

    @Override
    public void accept(Object o) {
      result.add(o);
    }

    @Override
    public Object get() {
      return result;
    }
  }

  final private Consumer<Object> out;

  private State state = new State(null) {
    @Override
    public Object get() {
      throw new UnsupportedOperationException("Unbalanced onEnd() call.");
    }

    @Override
    public void accept(Object o) {
      out.accept(o);
    }
  };

  public SimpleDeserializer(Consumer<Object> out) {
    this.out = out;
  }

  @Override
  public void onValue(Object x) {
    state.accept(x);
  }

  @Override
  public void onArray() {
    state = new ArrayValue(state);
  }

  @Override
  public void onMap() {
    state = new MapValue(state);
  }

  @Override
  public void onEnd() {
    assert state != null;
    final State p = state.getParent();
    p.accept(state.get());
    state = p;
  }
}
