grammar GlidingSoar;

glidingSoar : element*;

element : deceleration body;

deceleration : declerationType IDENTIFIER extends?;

body : OPEN_CURLY members? tags? matches? CLOSE_CURLY;

members : MEMBERS OPEN_CURLY member* CLOSE_CURLY;
member : IDENTIFIER COLON IDENTIFIER classifier?;

classifier : OPTIONAL | REQUIRED | MULTIIPLE;

tags : TAGS OPEN_CURLY tag* CLOSE_CURLY;
tag : IDENTIFIER (COLON IDENTIFIER)?;

matches : MATCHES OPEN_CURLY match*;
match : IDENTIFIER COLON IDENTIFIER OPEN_CURLY MATCH_STRING CLOSE_CURLY;

declerationType : OBJECT | INPUT | OUTPUT | INTERFACE;

extends : COLON IDENTIFIER (COMMA IDENTIFIER)*;

COMMA : ',';
COLON : ':';

OBJECT    : 'object';
INPUT     : 'input';
OUTPUT    : 'output';
INTERFACE : 'interface';

MEMBERS : 'members';
TAGS : 'tags';
MATCHES : 'matches';

OPTIONAL : '?';
REQUIRED : '!';
MULTIIPLE : '+';

MATCH_STRING : '{' MATCH_STRING_CONTENT? '}';

fragment MATCH_STRING_CONTENT : MATCH_STRING_CHAR+;
fragment MATCH_STRING_CHAR : ~[\\"];

OPEN_CURLY : '{' ;
CLOSE_CURLY : '}';

IDENTIFIER : [a-zA-Z9-9\-_*]+;

WHITESPACE : [ \t\r\n] -> skip;
