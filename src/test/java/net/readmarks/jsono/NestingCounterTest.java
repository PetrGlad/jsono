package net.readmarks.jsono;

import net.readmarks.jsono.handler.NestingCounter;
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
    c.onArray();
    assertEquals(1, c.getDepth());
    c.onMap();
    assertEquals(2, c.getDepth());
    c.onEnd();
    assertEquals(1, c.getDepth());
    c.onEnd();
    assertEquals(0, c.getDepth());
  }

  @Test(expected = IllegalStateException.class)
  public void testUnderflow() {
    new NestingCounter(5).onEnd();
  }

  @Test(expected = IllegalStateException.class)
  public void testOverflow() {
    final NestingCounter c = new NestingCounter(0);
    c.onMap();
  }

  @Test
  public void testOverflowDefault() {
    final NestingCounter c = new NestingCounter();
    for (int i = 0; i < 1000; i++) {
      c.onArray();
      assertEquals(i + 1, c.getDepth());
    }
    try {
      c.onArray();
      fail("Exception expected.");
    } catch (IllegalStateException e) {
      // Expected
    }
  }
}
