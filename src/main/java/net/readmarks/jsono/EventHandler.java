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
   * Signals that next {@link #onValue(Object)} call will provide map key.
   *
   * Tentative, might be removed in future versions.
   * It is possible to ignore this event and explicitly handle key/value interleaving.
   */
  void onMapKey();

  /**
   * End of most recent nested data structure (map or array).
   */
  void onEnd();
}
