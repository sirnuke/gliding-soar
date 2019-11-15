grammar GlidingSoar;

glidingSoar : element*;

element : declaration body;

declaration : type IDENTIFIER extends_?;

type: OBJECT | INPUT | OUTPUT | INTERFACE;

extends_ : COLON IDENTIFIER (COMMA IDENTIFIER)*;

body : OPEN_CURLY bodyElement* CLOSE_CURLY;

bodyElement : members | tags | matches;

members : MEMBERS OPEN_CURLY member* CLOSE_CURLY;
member : IDENTIFIER COLON IDENTIFIER classifier?;

classifier : OPTIONAL | REQUIRED | MULTIPLE;

tags : TAGS OPEN_CURLY tag* CLOSE_CURLY;
tag : IDENTIFIER (COLON IDENTIFIER)?;

matches : MATCHES OPEN_CURLY match*;
match : IDENTIFIER COLON matchBody;
matchBody : matchSubst;
matchSubst: SUBST RAW_TCL;

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
MULTIPLE : '+';

RAW_TCL : '<<' .*? '>>';

OPEN_CURLY : '{' ;
CLOSE_CURLY : '}';

TCL_LINE_COMMENT : '#'  ~[\r\n]* -> skip;
CPP_LINE_COMMENT : '//' ~[\r\n]* -> skip;
C_BLOCK_COMMENT  : '/*' .*? '*/' -> skip;

IDENTIFIER : IDENTIFIER_FIRST IDENTIFIER_REST*;

fragment IDENTIFIER_FIRST : [a-zA-Z\-_*];
fragment IDENTIFIER_REST : [a-zA-Z0-9\-_*];

WHITESPACE : [ \t\r\n] -> skip;


