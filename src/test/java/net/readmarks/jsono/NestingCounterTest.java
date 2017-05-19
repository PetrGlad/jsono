package net.readmarks.jsono;

import org.junit.Test;

import static org.junit.Assert.*;

public class NestingCounterTest {
  @Test(expected = IllegalArgumentException.class)
  public void testInitValidation() {
    new NestingCounter(-1);
  }

  @Test
  public void testCounting() {
    final NestingCounter c = new NestingCounter();
    assertEquals(0, c.getDepth());
    c.accept(JsonParser.Event.ARRAY);
    assertEquals(1, c.getDepth());
    c.accept(JsonParser.Event.MAP);
    assertEquals(2, c.getDepth());
    c.accept(JsonParser.Event.ARRAY_END);
    assertEquals(1, c.getDepth());
    c.accept(JsonParser.Event.MAP_END);
    assertEquals(0, c.getDepth());
  }

  @Test(expected = IllegalStateException.class)
  public void testUnderflow() {
    new NestingCounter(5).accept(JsonParser.Event.MAP_END);
  }

  @Test(expected = IllegalStateException.class)
  public void testOverflow() {
    final NestingCounter c = new NestingCounter(0);
    c.accept(JsonParser.Event.MAP);
  }

  @Test
  public void testOverflowDefault() {
    final NestingCounter c = new NestingCounter();
    for (int i = 0; i < 1000; i++) {
      c.accept(JsonParser.Event.ARRAY);
      assertEquals(i + 1, c.getDepth());
    }
    try {
      c.accept(JsonParser.Event.ARRAY);
      fail("Exception expected.");
    } catch (IllegalStateException e) {
      // Expected
    }
  }
}
