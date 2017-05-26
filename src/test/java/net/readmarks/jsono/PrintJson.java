package net.readmarks.jsono;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import net.readmarks.jsono.handler.EventToGenerator;
import net.readmarks.jsono.handler.HandlerUtil;
import net.readmarks.jsono.handler.NestingCounter;
import net.readmarks.utf8.Utf8Decoder;

import java.io.*;

/**
 * Example of JsonParser usage with Jackson JSON generator.
 */
public class PrintJson {

  public static void main(String[] args) throws IOException {
    if (args.length < 1) {
      System.err.println("Analogous to `jq .` reads input document and formats it.");
      System.err.println("Expecting a file name as argument.");
      System.exit(1);
    }
    try (final InputStream in = new FileInputStream(args[0])) {
      // Using Jackson to generate JSON
      final JsonGenerator generator = defaultGenerator(new OutputStreamWriter(System.out));
      generator.useDefaultPrettyPrinter();

      // Parsing input as UTF-8
      final JsonParser parser = new JsonParser(
              HandlerUtil.then(
                      new NestingCounter(),
                      new EventToGenerator(generator)));
      final Utf8Decoder utf8parser = new Utf8Decoder(parser::parseNext);
      int b = in.read();
      while (b != -1) {
        // TODO (API) Allow decoding sub-range of provided array in UTF-decoder.
        utf8parser.put(new byte[]{(byte) b});
        b = in.read();
      }
      utf8parser.end();
      parser.end();
      generator.close();
      System.out.println();
    }
  }

  public static JsonGenerator defaultGenerator(Writer writer) {
    try {
      return new JsonFactory().createGenerator(writer);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
