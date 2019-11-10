lexer grammar GlidingLexer;

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


