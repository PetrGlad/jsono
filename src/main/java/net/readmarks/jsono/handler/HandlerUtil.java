package net.readmarks.jsono.handler;

import net.readmarks.jsono.EventHandler;

import java.util.function.Consumer;

public class HandlerUtil {
  /**
   * Same as {@link java.util.function.Consumer#andThen(Consumer)}
   * but for pair of {@link net.readmarks.jsono.EventHandler}s.
   */
  public static EventHandler then(final EventHandler a, final EventHandler b) {
    return new EventHandler() {
      @Override
      public void onValue(Object x) {
        a.onValue(x);
        b.onValue(x);
      }

      @Override
      public void onArray() {
        a.onArray();
        b.onArray();
      }

      @Override
      public void onMap() {
        a.onMap();
        b.onMap();
      }

      @Override
      public void onEnd() {
        a.onEnd();
        b.onEnd();
      }
    };
  }
}
