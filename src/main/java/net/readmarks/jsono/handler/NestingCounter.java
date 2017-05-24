package net.readmarks.jsono.handler;


import net.readmarks.jsono.EventHandler;

/**
 * Counts structural depth of a JSON document.
 * Should be used to limit max depth of documents to prevent heap overflows from malicious/incorrectly
 * constructed documents.
 * Can be used to get current depth e.g. for JSON querying purposes.
 * Does not check whether begin/end tokens are matching (e.g. {[}] case). That is expected to be validated already
 * by upstream parser (event source);
 */
public class NestingCounter implements EventHandler {
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


  private void down() {
    if (depth >= limit) {
      throw new IllegalStateException("Nesting depth " + depth + " reached limit.");
    }
    depth++;
  }

  private void up() {
    if (depth <= 0) {
      throw new IllegalStateException("Nesting error.");
    }
    depth--;
  }

  @Override
  public void onValue(Object x) {
  }

  @Override
  public void onArray() {
    down();
  }

  @Override
  public void onMap() {
    down();
  }

  @Override
  public void onEnd() {
    up();
  }
}
