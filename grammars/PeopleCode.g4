grammar PeopleCode;

//******************************************************//
// Parser Rules 									    //
//******************************************************//

program	:	stmtList ;

stmtList:	(stmt ';')* stmt? ;	 // The last/only statement doesn't need a semicolon.

// Semicolons are optional after some PeopleCode stmt constructs.
stmt	:	appClassImport						# StmtAppClassImport
		|	funcImport							# StmtFuncImport
		|	funcDeclaration						# StmtFuncDeclaration
		|	varDeclaration						# StmtVarDeclaration
		|	ifStmt								# StmtIf
		|	forStmt								# StmtFor
		|	evaluateStmt						# StmtEvaluate
		|	tryCatchStmt						# StmtTryCatch
		|	'Exit'								# StmtExit
		|	'Break'								# StmtBreak
		|	'Error' expr						# StmtError
		|	'Warning' expr						# StmtWarning
		|	expr '=' expr						# StmtAssign	// Must be higher than fn call to properly resolve '=' ambiguity.
		|	expr								# StmtExpr		// For fn calls; the value of expr is not assigned to anything.
		;

expr	:	'(' expr ')'								# ExprParenthesized
		|	literal										# ExprLiteral
		|	id											# ExprId
		|	'@'<assoc=right> '(' expr ')'				# ExprDynamicReference
		|	'-'<assoc=right> expr						# ExprNegate
		|	'Not'<assoc=right> expr						# ExprNot
		|	createInvocation							# ExprCreate
		|	expr '.' id									# ExprStaticReference
		|	expr '[' expr ']'							# ExprArrayIndex
		| 	expr '(' exprList? ')'						# ExprFnCall
		|	expr ('+'|'-') expr							# ExprAddSub
		|	expr ('<'|'<='|'>'|'>=') expr				# ExprComparison
		|	expr ('='|'<>') expr						# ExprEquality	// splitting symbols out may affect parsing
		|	expr (
					'And'<assoc=right>
				|	'Or'<assoc=right>
			) expr										# ExprBoolean // order: Not, And, then Or
		|	expr '|' expr								# ExprConcatenate
		;

exprList:	expr (',' expr)* ;

varDeclaration	:	varScope varType varDeclarator (',' varDeclarator)* ;
varDeclarator	:	VAR_ID ('=' expr)? ;

varScope	:	'Global' | 'Component' | 'Local' ;
varType		:	'array' ('of' varType)? | 'Record' | 'string' | 'boolean' | 'Field'
			|	'integer' | 'number' | 'Rowset' | 'date' | 'Row' | appClassPath
			|	GENERIC_ID	// for objects (i.e., Address)
			;

appClassImport	:	'import' appClassPath ;
appClassPath	:	GENERIC_ID (':' GENERIC_ID)+ ;

funcImport	:	'Declare' 'Function' GENERIC_ID 'PeopleCode' recDefnPath event ;
recDefnPath	:	GENERIC_ID ('.' GENERIC_ID)* ;
event		:	'FieldFormula' ;

funcDeclaration :   'Function' GENERIC_ID '(' ')' ';' stmtList 'End-Function' ;

ifStmt	:	'If' expr 'Then' stmtList ('Else' stmtList)? 'End-If' ;

forStmt	:	'For' VAR_ID '=' expr 'To' expr stmtList 'End-For' ;

evaluateStmt	:	'Evaluate' expr ('When' '='? expr stmtList)+
						('When-Other' stmtList)? 'End-Evaluate' ;

tryCatchStmt	:	'try' stmtList 'catch' 'Exception' VAR_ID stmtList 'end-try' ;

// the object name could be the fully resolved app class path or just the class name.
createInvocation:	'create' (appClassPath | GENERIC_ID) '(' exprList? ')' ;

defnType	:	'Component' | 'Panel' | 'RecName' | 'Scroll' | 'HTML'
			|	'MenuName' | 'BarName' | 'ItemName' | 'CompIntfc' | 'Image'
			|	'Interlink' | 'StyleSheet' | 'FileLayout' | 'Page' | 'PanelGroup'
			|	'Message' | 'BusProcess' | 'BusEvent' | 'BusActivity' | 'Field'
			|	'Record' | 'Operation' | 'SQL' ;

literal	:	DecimalLiteral
		|	IntegerLiteral
		|	defnLiteral
		|	StringLiteral
		|	boolLiteral
		;
boolLiteral	:	'True' | 'true' | 'False' | 'false' ;
defnLiteral : defnType '.' GENERIC_ID ;

id	:	SYS_VAR_ID | VAR_ID | GENERIC_ID ;

//*******************************************************//
// Lexer Rules 									    	 //
//*******************************************************//

VAR_ID		:	'&' GENERIC_ID ;
SYS_VAR_ID	:	'%' GENERIC_ID ;
GENERIC_ID	:	[a-zA-Z] [0-9a-zA-Z_]* ;	

DecimalLiteral	:	IntegerLiteral '.' [0-9]+ ;
IntegerLiteral	:	'0' | '1'..'9' '0'..'9'* ;
StringLiteral	:	'"' ( ~'"' )* '"' ;

REM			:	WS [rR][eE][mM] WS .*? ';' -> skip;
COMMENT_1	:	'/*' .*? '*/' -> skip;
COMMENT_2	:	'<*' .*? '*>' -> skip;
WS			:	[ \t\r\n]+ -> skip ;
