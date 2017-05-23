package net.readmarks.jsono;

import net.readmarks.utf8.Utf8Decoder;

public class SimpleDeserializerExample {
  public static void main(String[] args) {
    final JsonParser jsonParser = new JsonParser(
            new NestingCounter()
                    .andThen(new SimpleDeserializer(struct ->
                            System.out.println("Parsed struct: " + struct))));
    final Utf8Decoder utf8decoder = new Utf8Decoder(jsonParser::parseNext);

    utf8decoder.put(("[null,true,\"blUr \\u266b\" ,314e-2," +
                " {\"full-name\":[\"Cromwell\", \"jr.\", \"III\", \"Esq.\"]}," +
                " {\"name\":\"Elvis \\u266b\"}]").getBytes());
    utf8decoder.end();
    jsonParser.end();
  }
}
