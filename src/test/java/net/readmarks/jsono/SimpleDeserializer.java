package net.readmarks.jsono;

import net.readmarks.jsono.JsonParser.Event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

public class SimpleDeserializer implements Consumer<Object> {
  interface State extends Function<Object, State> {
  }

  class AnyValue implements State {
    final State parent;

    public AnyValue(State parent) {
      this.parent = parent;
    }

    @Override
    public State apply(Object o) {
      if (o == Event.ARRAY) {
        return new ArrayValue(this);
      } else if (o == Event.MAP) {
        return new MapValue(this);
      } else {
        return parent.apply(o);
      }
    }
  }

  class MapValue implements State {
    final State parent;
    final Map<String, Object> result = new HashMap<>();
    private Event mapState; // TODO Remove Event.MAP_KEY and Event.MAP_VALUE (should handle key/val interleaving anyway here)
    private String key;

    public MapValue(State parent) {
      this.parent = parent;
    }

    @Override
    public State apply(Object o) {
      assert o != Event.END;
      if (o == Event.MAP_KEY || o == Event.MAP_VALUE) {
        mapState = (Event) o;
      } else if (o == Event.MAP_END) {
        return parent.apply(result);
      } else if (mapState == Event.MAP_KEY) {
        key = (String) o;
      } else if (mapState == Event.MAP_VALUE) {
        if (o instanceof Event) {
          return new AnyValue(this).apply(o);
        } else {
          result.put(key, o);
        }
      }
      return this;
    }
  }

  class ArrayValue implements State {
    final State parent;
    final List<Object> result = new ArrayList<>();

    public ArrayValue(State parent) {
      this.parent = parent;
    }

    @Override
    public State apply(Object o) {
      if (o == Event.ARRAY_END) {
        return parent.apply(result);
      } else {
        if (o instanceof Event) {
          return new AnyValue(this).apply(o);
        } else {
          result.add(o);
        }
      }
      return this;
    }
  }

  final private Consumer<Object> out;

  private State state = new State() {
    @Override
    public State apply(Object o) {
      if (o == Event.END) {
        return null;
      } else if (o instanceof Event) {
        return new AnyValue(this)
                .apply(o);
      } else {
        out.accept(o);
        return this;
      }
    }
  };

  public SimpleDeserializer(Consumer<Object> out) {
    this.out = out;
  }

  @Override
  public void accept(Object o) {
    if (state == null) {
      throw new IllegalStateException("No more events expected (got " + o + ").");
    }
    state = state.apply(o);
  }
}

