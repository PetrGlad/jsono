package net.readmarks.jsono;

import net.readmarks.jsono.JsonParser.Event;

import java.util.function.Consumer;

/**
 * Counts structural depth of a JSON document.
 * Should be used to limit max depth of documents to prevent heap overflows from malicious/incorrectly
 * constructed documents.
 * Can be used to get current depth e.g. for JSON querying purposes.
 * Does not check whether begin/end tokens are matching (e.g. {[}] case). That is expected to be validated already
 * by upstream parser (event source);
 */
public class NestingCounter implements Consumer<Object> {
  private final int limit;
  private int depth;

  public NestingCounter() {
    this(1000);
  }

  public NestingCounter(int limit) {
    if (limit < 0) {
      throw new IllegalArgumentException("Limit should not be negative. limit=" + limit + ".");
    }
    this.limit = limit;
  }

  public int getDepth() {
    return depth;
  }

  @Override
  public void accept(Object jsonEvent) {
    if (jsonEvent instanceof Event) {
      switch ((Event) jsonEvent) {
        case ARRAY:
        case MAP:
          if (depth >= limit) {
            throw new IllegalStateException("Nesting depth " + depth + " reached limit.");
          }
          depth++;
          break;
        case ARRAY_END:
        case MAP_END:
          if (depth <= 0) {
            throw new IllegalStateException("Nesting error.");
          }
          depth--;
          break;
        default:
      }
    }
  }
}
