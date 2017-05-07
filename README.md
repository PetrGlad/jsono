# jsono

Experimental reactive JSON parser. 

This parser accepts a sequence of characters (unicode code points).
This means that for complete implementation you should also have an reactive UTF8 parser.
See for example https://github.com/PetrGlad/utf8-parser or character decoders available 
in JDK or Netty project.

It is an attempt to write more maintainable version of existing state-machine JSON parsers.
Currently implementation is incomplete and may generate lots of garbage.
The best approach would be to generate parser state machine from grammar but currently 
there seem no such tools that generate reactive parsers.
