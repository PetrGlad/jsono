package net.readmarks.jsono;

import java.util.concurrent.atomic.AtomicLong;

public class Microbenchmark {

    public static void main(String[] args) {
        final long t1 = System.currentTimeMillis();
        final int N = 10000000;
        final String json = "[null,true,\"blurB\"]"; // TODO (benchmark) Use larger documents
        AtomicLong eventCount = new AtomicLong(0);
        for (int ll = 0; ll < N; ll++) {
            final JsonParser p = new JsonParser(event -> eventCount.incrementAndGet());
            for (int i = 0; i < json.length(); i++) {
                p.parseNext(json.charAt(i));
            }
            p.end();
        }
        System.out.println("Parsed " + eventCount.get() + " events." +
                                   " Parse speed " + (1.0 * eventCount.get() / (System.currentTimeMillis() - t1)) + " events/mSec.");
    }
}
