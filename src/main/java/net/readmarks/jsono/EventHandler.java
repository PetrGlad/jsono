package net.readmarks.jsono;

/**
 * Receives events from JSON parser.
 *
 * @see JsonParser
 */
public interface EventHandler {
  /**
   * @param x Next primitive value (including Strings).
   */
  void onValue(Object x);

  /**
   * Start of an array. Subsequent calls to {@link #onValue(Object)} provide sequence of array values.
   */
  void onArray();

  /**
   * Start of map (object). Subsequent calls to {@link #onValue(Object)}
   * provide interleaved sequence of keys and values: key1, value1, key2, value2...
   */
  void onMap();

  /**
   * End of most recent data structure.
   * Every call to {@link #onArray()} or {@link #onMap()} is eventually followed by it's {@link #onEnd()} call.
   */
  void onEnd();
}
