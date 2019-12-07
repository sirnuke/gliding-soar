grammar GlidingSoar;

glidingSoar : element*;

element : type IDENTIFIER extends_? body;

type: OBJECT | INPUT | OUTPUT | INTERFACE;

extends_ : COLON IDENTIFIER (COMMA IDENTIFIER)*;

body : OPEN_CURLY bodyElement* CLOSE_CURLY;

bodyElement : parameter | member | match;

parameter : PARAMETER IDENTIFIER COLON IDENTIFIER MULTIPLE? OPTIONAL?;

member : (I_SUPPORT | O_SUPPORT) TAG? IDENTIFIER COLON IDENTIFIER MULTIPLE?;

match : (SUBST|PROC) IDENTIFIER arguments? EQUALS RAW_TCL;

arguments : OPEN_CURLY IDENTIFIER+ RAW_TCL? CLOSE_CURLY;

COMMA : ',';
COLON : ':';
EQUALS : '=';

OBJECT    : 'object';
INPUT     : 'input';
OUTPUT    : 'output';
INTERFACE : 'interface';

PARAMETER : 'param';

I_SUPPORT : 'i';
O_SUPPORT : 'o';

TAG : 'tag';

PROC : 'proc';
SUBST : 'subst';

OPTIONAL : '?';
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


