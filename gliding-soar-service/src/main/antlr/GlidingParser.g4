parser grammar GlidingParser;

options { tokenVocab = GlidingLexer; }

glidingSoar : element*;

element : deceleration body;

deceleration : typeDecleration IDENTIFIER typeExtends?;

body : OPEN_CURLY members? tags? matches? CLOSE_CURLY;

members : MEMBERS OPEN_CURLY member* CLOSE_CURLY;
member : IDENTIFIER COLON IDENTIFIER classifier?;

classifier : OPTIONAL | REQUIRED | MULTIIPLE;

tags : TAGS OPEN_CURLY tag* CLOSE_CURLY;
tag : IDENTIFIER (COLON IDENTIFIER)?;

matches : MATCHES OPEN_CURLY match*;
match : IDENTIFIER COLON IDENTIFIER RAW_TCL;

typeDecleration: OBJECT | INPUT | OUTPUT | INTERFACE;

typeExtends : COLON IDENTIFIER (COMMA IDENTIFIER)*;
