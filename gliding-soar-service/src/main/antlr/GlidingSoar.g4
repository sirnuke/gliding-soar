grammar GlidingSoar;

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
match : IDENTIFIER COLON matchBody;
matchBody : matchSubst;
matchSubst: SUBST RAW_TCL;

typeDecleration: OBJECT | INPUT | OUTPUT | INTERFACE;

typeExtends : COLON IDENTIFIER (COMMA IDENTIFIER)*;

COMMA : ',';
COLON : ':';

OBJECT    : 'object';
INPUT     : 'input';
OUTPUT    : 'output';
INTERFACE : 'interface';

MEMBERS : 'members';
TAGS : 'tags';
MATCHES : 'matches';

SUBST : 'Subst';

OPTIONAL : '?';
REQUIRED : '!';
MULTIIPLE : '+';

RAW_TCL : '<<' .*? '>>';

OPEN_CURLY : '{' ;
CLOSE_CURLY : '}';

IDENTIFIER : [a-zA-Z9-9\-_*]+;

WHITESPACE : [ \t\r\n] -> skip;


