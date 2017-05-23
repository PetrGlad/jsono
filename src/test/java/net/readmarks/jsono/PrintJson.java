package net.readmarks.jsono;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import net.readmarks.jsono.JsonParser.Event;
import net.readmarks.utf8.Utf8Decoder;

import java.io.*;

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
      setPrettyPrint(generator);

      // Parsing input as UTF-8
      final JsonParser parser = new JsonParser(
              new NestingCounter()
                      .andThen(event -> eventToGenerator(event, generator)));
      final Utf8Decoder utf8parser = new Utf8Decoder(parser::parseNext);
      int b = in.read();
      while (b != -1) {
        // TODO (API) Allow reading sub-range of provided array in UTF-decoder
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

  public static JsonGenerator setPrettyPrint(JsonGenerator generator) {
    return generator.setPrettyPrinter(new DefaultPrettyPrinter());
  }

  public static void eventToGenerator(Object event, JsonGenerator generator) {
    try {
      if (event instanceof Event) {
        switch ((Event) event) {
          case ARRAY:
            generator.writeStartArray();
            break;
          case ARRAY_END:
            generator.writeEndArray();
            break;
          case MAP:
            generator.writeStartObject();
            break;
          case MAP_END:
            generator.writeEndObject();
            break;
          case END:
        }
      } else if (event instanceof String
              && generator.getOutputContext().inObject()
              && generator.getOutputContext().getCurrentName() == null) {
        generator.writeFieldName((String) event);
      } else {
        generator.writeObject(event);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
