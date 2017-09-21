grammar XQuery;

options {
	language = Java;
}

ap
	: doc '/' rp	#ApChildren
	| doc '//' rp	#ApDescendants
	;
doc
	: 'doc' '(' '"' fileName '"' ')'
	;

fileName
    : NAME
    ;

rp
	: '(' rp ')'	#RpBracket
	| rp '/' rp		#RpSlash
	| rp '//' rp	#RpDoubleSlash
	| rp '[' f ']'	#RpFilter
	| rp ',' rp		#RpConcatenation
	| '*'		#RpChildren
	| '.'		#RpCurrent
	| '..'		#RpParent
	| 'text()'	#RpText
	| '@' NAME	#RpAttribute
	| NAME	#RpTagName
	;
f
	: rp	#FilterRp
	| rp '=' rp		#FilterEq
	| rp 'eq' rp	#FilterEq
	| rp '==' rp	#FilterIs
	| rp 'is' rp	#FilterIs
	| '(' f ')'		#FilterBracket
	| f 'and' f		#FilterAnd
	| f 'or' f		#FilterOr
	| 'not' f		#FilterNot
	;

xq
    : '$' var       #XqVar
    | STR_CONST     #XqStrConst
    | ap            #XqAp
    | '(' xq ')'    #XqBracket
    | xq '//' rp    #XqDoubleSlashRp
    | xq '/' rp     #XqSlashRp
    | xq ',' xq     #XqComma
    | '<' NAME '>' '{' xq '}' '<' '/' NAME '>'            #XqResult
    | forClause letClause? whereClause? returnClause      #XqClause
    | letClause xq  #Xqlet
    | 'join' '(' xq ',' xq ',' varList ',' varList  ')'  #XqJoin
    ;

forClause
    : 'for' '$' var 'in' xq (',' '$' var 'in' xq)*
    ;

letClause
    : 'let' '$' var ':=' xq (',' '$' var ':=' xq)*
    ;

whereClause
    : 'where' cond
    ;

returnClause
    : 'return' xq
    ;

cond
    : xq '=' xq                 #CondEq
    | xq 'eq' xq                #CondEq
    | xq '==' xq                #CondIs
    | xq 'is' xq                #CondIs
    | 'empty' '(' xq ')'        #CondEmpty
    | 'some' '$' var 'in' xq (',' '$' var 'in' xq)* 'satisfies' cond    #CondSome
    | '(' cond ')'              #CondParenthetsis
    | cond 'and' cond           #CondAnd
    | cond 'or' cond            #CondOr
    | 'not' cond                #CondNot
    ;

var
    : NAME
    ;

varList
    : '[' NAME? (',' NAME)* ']'
    ;

NAME	: [-_.a-zA-Z0-9]+;
STR_CONST : '"' ['"a-zA-Z0-9! ?,.;:]+ '"';
WS	: [ \t\r\n]+ -> skip ;