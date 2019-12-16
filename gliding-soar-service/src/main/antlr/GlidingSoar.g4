grammar GlidingSoar;

glidingSoar : element*;

element : type resolvedIdentifier extends_? body;

type: OBJECT | INPUT | OUTPUT | INTERFACE;

resolvedIdentifier : (IDENTIFIER NAMESPACE)* IDENTIFIER;

extends_ : COLON resolvedIdentifier (COMMA resolvedIdentifier)*;

body : OPEN_CURLY bodyElement* CLOSE_CURLY;

bodyElement : member | match;

member : (I_SUPPORT | O_SUPPORT )? PARAMETER? TAG? IDENTIFIER COLON resolvedIdentifier MULTIPLE? OPTIONAL?;

match : (SUBST|PROC) IDENTIFIER arguments? EQUALS RAW_TCL;

arguments : OPEN_CURLY IDENTIFIER+ RAW_TCL? CLOSE_CURLY;

COMMA : ',';
COLON : ':';
EQUALS : '=';
NAMESPACE : '::';

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


