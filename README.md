# jsono

A reactive JSON parser. It does not block thread when there's no input data.
This is especially useful for reading potentially infinite or very large JSON documents 
also in cases when incoming data is delivered with unpredictable or long delays.

This library has only JSON parser. If you want to generate JSON you can use any other 
existing JSON library (see class net.readmarks.jsono.PrintJson for example 
that uses Jackson).

The parser produces sequence of parsed JSON tokens that can be used for JSON analysis
(e.g. for XPath or jq-like queries) without deserializing anything or to deserialize 
Java objects (see class SimpleDeserializer for basic implementation).

This parser accepts a sequence of characters (unicode code points).
This means that for completely non-blocking implementation you should also have
a reactive UTF8 parser.
For example https://github.com/PetrGlad/utf8-parser or implement one using 
character decoders available in JDK or Netty project.

See test code for usage details.

#### Why

It is an attempt to write more maintainable version of existing state-machine JSON parsers.
The best approach would be to generate parser state machine from grammar but currently 
there seem no such tools that generate reactive parsers.

#### Reference

[json.org](http://json.org/) hosts JSON specification and reference implementations. 

#### Alternatives

[Actson](https://github.com/michel-kraemer/actson) which was an inspiration 
for making this library. 

If you can afford waiting for complete JSON document to be
available then pretty much any other JSON library might be sufficient:

* https://github.com/FasterXML/jackson
* https://github.com/google/gson
* https://github.com/stleary/JSON-java
* https://github.com/boonproject/boon

and others.
