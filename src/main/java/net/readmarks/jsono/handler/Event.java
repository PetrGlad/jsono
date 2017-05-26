package net.readmarks.jsono.handler;

import net.readmarks.jsono.EventHandler;

/**
 * @see StreamingHandler
 */
public enum Event {
  /**
   * @see EventHandler#onArray()
   */
  ARRAY,
  /**
   * @see EventHandler#onMap()
   */
  MAP,
  /**
   * @see EventHandler#onMapKey()
   */
  KEY,
  /**
   * @see EventHandler#onEnd()
   */
  END
}
