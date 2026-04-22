package com.lnc.cc.parser;

import com.lnc.cc.ast.*;
import com.lnc.cc.types.*;
import com.lnc.common.frontend.CompileException;
import com.lnc.common.frontend.FullSourceParser;
import com.lnc.common.frontend.Token;
import com.lnc.common.frontend.TokenType;

import java.util.ArrayList;
import java.util.stream.Stream;

/**
 * The LncParser class is a specialized parser for interpreting and analyzing
 * the syntax of LNC programs. This parser extends
 * the FullSourceParser class and produces an Abstract Syntax Tree (AST)
 * representation of the parsed program.
 *
 * The parser processes tokens to recognize variable declarations, type
 * specifiers, expressions, and statements. It handles synchronization with
 * predefined tokens and manages the structure of parsed code blocks and
 * declarations.
 */
public class LncParser extends FullSourceParser<AST> {
    private static final TokenType[] SYNC_TOKENS = Stream.of(
            new TokenType[]{TokenType.IF, TokenType.WHILE, TokenType.FOR, TokenType.DO, TokenType.RETURN, TokenType.BREAK, TokenType.CONTINUE},
            new TokenType[]{
                    TokenType.CONST, TokenType.NEAR, TokenType.FAR
            },
            StorageQualifier.VALID_TOKENS,
            TypeSpecifier.VALID_TOKENS,
            new TokenType[]{TokenType.SEMICOLON, TokenType.L_CURLY_BRACE, TokenType.R_CURLY_BRACE}
    ).flatMap(Stream::of).toArray(TokenType[]::new);
    private final AST ast;

    public LncParser(Token[] tokens) {
        super(tokens);
        this.ast = new AST();
    }

    @Override
    public AST getResult() {
        return ast;
    }

    @Override
    public boolean parse() {
        boolean success = true;
        while(!isAtEnd()) {
            try{
                ast.addDeclaration(externalDeclaration());
            } catch (CompileException e) {
                e.log();
                sync();
                success = false;
                break;
            }
        }
        return success;
    }

    private Declaration externalDeclaration() {
        var statement = variableDeclaration(VarDeclRules.EXTERNAL_DECL);

        if(statement == null){
            return null;
        }

        if(statement.type != Statement.Type.DECLARATION){
            throw new CompileException("expected external declaration (variable or function)", peek());
        }

        var declaration = (Declaration) statement;

        if(declaration.declarationType == Declaration.Type.STRUCT){
            return declaration;
        }

        var variableDeclaration = (VariableDeclaration) statement;

        if(match(TokenType.L_PAREN)){
            var parameters = new ArrayList<VariableDeclaration>();

            while(!check(TokenType.R_PAREN)){
                var decl = variableDeclaration(VarDeclRules.PARAMETER_DECL, parameters.size());
                if(decl == null || decl.type != Statement.Type.DECLARATION || ((Declaration) decl).declarationType != Declaration.Type.VARIABLE){
                    throw new CompileException("expected parameter declaration", peek());
                }
                parameters.add((VariableDeclaration) decl);
                if(!match(TokenType.COMMA)){
                    break;
                }
            }

            consume("expected ')'", TokenType.R_PAREN);

            if(match(TokenType.SEMICOLON)){
                return new FunctionDeclaration(variableDeclaration.declarator, variableDeclaration.name, parameters.toArray(new VariableDeclaration[0]), null);
            }else if(match(TokenType.L_CURLY_BRACE)) {
                if(variableDeclaration.declarator.storageQualifier().isExtern())
                    throw new CompileException("extern function cannot have a body", variableDeclaration.name);
                return new FunctionDeclaration(variableDeclaration.declarator, variableDeclaration.name, parameters.toArray(new VariableDeclaration[0]), block());
            }else {
                throw new CompileException("expected function body or ';'", peek());
            }
        }else if(match(TokenType.SEMICOLON)){
            return variableDeclaration;
        }

        throw new CompileException("expected ';'", peek());
    }

    private Statement variableDeclaration(VarDeclRules varDeclRules) {
        return variableDeclaration(varDeclRules, -1);
    }

    /**
     * Parses a variable declaration.
     * @param rules the rules for parsing the variable declaration
     * @return the parsed variable declaration or null if no valid declaration was found
     */
    private Statement variableDeclaration(VarDeclRules rules, int parameterIndex) {
        Declarator declarator = declarator(rules);

        if(declarator == null){
            return statement();
        }

        if(!rules.isParameter && declarator.typeSpecifier().type == TypeSpecifier.Type.STRUCT && match(TokenType.SEMICOLON)){
            var structDecl = (StructType) declarator.typeSpecifier();
            if(!declarator.storageQualifier().isNone()){
                throw new CompileException("struct declarations cannot have type qualifiers", structDecl.getName());
            }
            return new StructDeclaration(structDecl.getName(), structDecl.getDefinition());
        }

        Token ident = consume("expected identifier", TokenType.IDENTIFIER);

        while(match(TokenType.L_SQUARE_BRACKET)){

            Token size = consume("expected array size", TokenType.INTEGER);
            consume("expected ']'", TokenType.R_SQUARE_BRACKET);
            declarator = Declarator.wrapArray(declarator, (Integer) size.literal);
        }

        VariableDeclaration decl;

        if(rules.allowInitializer && match(TokenType.EQUALS)){

            var equals = previous();

            var initializer = expression();

            decl = new VariableDeclaration(declarator, ident, new AssignmentExpression(new IdentifierExpression(ident), equals, initializer, true), rules.isParameter, parameterIndex);
        }else{
            decl = new VariableDeclaration(declarator, ident, null, rules.isParameter, parameterIndex);
        }

        if(rules.expectSemicolon){
            consume("expected ';'", TokenType.SEMICOLON);
        }

        return decl;
    }

    private Declarator declarator(VarDeclRules rules){
        StorageQualifier storageQualifier = StorageQualifier.parse(this, rules);
        TypeSpecifier typeSpecifier = typeSpecifier(storageQualifier);

        if(typeSpecifier == null){
            return null;
        }

        return new Declarator(storageQualifier, typeSpecifier);
    }

    private TypeSpecifier typeSpecifier(StorageQualifier storageQualifier) {
        TypeSpecifier ts = null;
        if(check(TypeSpecifier.VALID_TOKENS)){
            ts = TypeSpecifier.parsePrimaryType(this, storageQualifier.storageLocation());
        }else if(check(TokenType.STRUCT)){
            ts = structSpecifier(storageQualifier.storageLocation());
        }
        if(ts == null){
            return null;
        }

        while(check(TokenType.STAR, TokenType.NEAR, TokenType.FAR, TokenType.CONST)){
            ts = wrapPointer(ts);
        }
        return ts;
    }

    private TypeSpecifier wrapPointer(TypeSpecifier ts) {
        boolean hasNear = false, hasFar = false, hasConst = false;
        StorageLocation sl = StorageLocation.NEAR;
        while(check(TokenType.STAR, TokenType.NEAR, TokenType.FAR, TokenType.CONST)){
            switch(advance().type){
                case STAR:
                    return PointerType.wrap(ts, hasConst, sl);
                case NEAR:
                    if(hasNear){
                        throw new CompileException("duplicate 'near'", previous());
                    }
                    if(hasFar){
                        throw new CompileException("conflicting near and far specifiers for pointer type", previous());
                    }
                    hasNear = true;
                    break;
                case FAR:
                    if(hasFar){
                        throw new CompileException("duplicate 'far'", previous());
                    }
                    if (hasNear){
                        throw new CompileException("conflicting near and far specifiers for pointer type", previous());
                    }
                    hasFar = true;
                    sl = StorageLocation.FAR;
                    break;
                case CONST:
                    if(hasConst){
                        throw new CompileException("duplicate 'const'", previous());
                    }
                    hasConst = true;
                    break;
            }
        }

        if(previous().type != TokenType.STAR){
            throw new CompileException("invalid token", previous()); // pointers must always end in a '*' token
        }

        return ts;
    }

    private TypeSpecifier structSpecifier(StorageLocation storageLocation) {
        consume("expected 'struct'", TokenType.STRUCT);
        Token name = consume("expected struct name", TokenType.IDENTIFIER);

        if(match(TokenType.L_CURLY_BRACE)){
            var members = new ArrayList<VariableDeclaration>();
            while(!check(TokenType.R_CURLY_BRACE)){
                var member = variableDeclaration(VarDeclRules.STRUCT_MEMBER_DECL);
                if(member == null || member.type != Statement.Type.DECLARATION || ((Declaration) member).declarationType != Declaration.Type.VARIABLE){
                    throw new CompileException("expected struct member declaration", peek());
                }
                members.add((VariableDeclaration) member);
            }
            consume("expected '}'", TokenType.R_CURLY_BRACE);

            return new StructType(name, new StructDefinitionType(name, members), storageLocation);
        }else{
            return new StructType(name, storageLocation);
        }
    }


    private BlockStatement block(){
        var statements = new ArrayList<Statement>();
        while(!check(TokenType.R_CURLY_BRACE) && !isAtEnd()){
            var decl = declaration();
            if(decl != null){
                statements.add(decl);
            }
        }
        consume("expected '}'", TokenType.R_CURLY_BRACE);
        return new BlockStatement(statements.toArray(new Statement[0]));
    }

    private Statement declaration() {
        return variableDeclaration(VarDeclRules.INTERNAL_DECL);
    }

    private Statement statement() {

        if(match(TokenType.L_CURLY_BRACE)){
            return block();
        }else if(match(TokenType.RETURN)){
            Token token = previous();
            if(check(TokenType.SEMICOLON)){
                advance();
                return new ReturnStatement(token, null);
            }
            var expr = expression();

            consume("expected ';'", TokenType.SEMICOLON);

            return new ReturnStatement(token, expr);
        }else if(match(TokenType.IF)){
            consume("expected '('", TokenType.L_PAREN);
            var condition = expression();
            consume("expected ')'", TokenType.R_PAREN);
            var thenBranch = statement();
            Statement elseBranch = null;
            if(match(TokenType.ELSE)){
                elseBranch = statement();
            }
            return new IfStatement(condition, thenBranch, elseBranch);
        }else if(match(TokenType.WHILE)){
            consume("expected '('", TokenType.L_PAREN);
            var condition = expression();
            consume("expected ')'", TokenType.R_PAREN);
            var body = statement();
            return new WhileStatement(condition, body);
        }else if(match(TokenType.FOR)) {
            consume("expected '('", TokenType.L_PAREN);
            Statement initializer = null;
            if (!match(TokenType.SEMICOLON)) {
                initializer = variableDeclaration(VarDeclRules.INTERNAL_DECL);
            }
            Expression condition = null;
            if (!match(TokenType.SEMICOLON)) {
                condition = expression();
                consume("expected ';'", TokenType.SEMICOLON);
            }
            Expression increment = null;
            if (!match(TokenType.R_PAREN)) {
                increment = expression();
                consume("expected ')'", TokenType.R_PAREN);
            }
            var body = statement();
            return new ForStatement(initializer, condition, increment, body);
        }else if(match(TokenType.DO)){
            var body = statement();
            consume("expected 'while'", TokenType.WHILE);
            consume("expected '('", TokenType.L_PAREN);
            var condition = expression();
            consume("expected ')'", TokenType.R_PAREN);
            consume("expected ';'", TokenType.SEMICOLON);
            return new DoWhileStatement(condition, body);
        }else if(match(TokenType.CONTINUE)){
            Token tkn = previous();
            consume("expected ';'", TokenType.SEMICOLON);
            return new ContinueStatement(tkn);
        }else if(match(TokenType.BREAK)){
            Token tkn = previous();
            consume("expected ';'", TokenType.SEMICOLON);
            return new BreakStatement(tkn);
        }else{

            if(match(TokenType.SEMICOLON)){
                return null;
            }

            var expression = expression();
            consume("expected ';'", TokenType.SEMICOLON);
            return new ExpressionStatement(expression);
        }
    }

    private Expression expression() {
        return assignment();
    }

    private Expression assignment() {
        var left = logicalOr();
        if(match(TokenType.EQUALS)){
            var operator = previous();
            var right = assignment();
            return new AssignmentExpression(left, operator, right, false);
        }
        if(match(TokenType.BITWISE_SHIFT_LEFT_EQUALS, TokenType.BITWISE_SHIFT_LEFT_EQUALS, TokenType.BITWISE_SHIFT_RIGHT_EQUALS, TokenType.BITWISE_AND_EQUALS, TokenType.BITWISE_OR_EQUALS, TokenType.BITWISE_XOR_EQUALS, TokenType.PLUS_EQUALS, TokenType.MINUS_EQUALS)){
            var operator = previous();
            var right = assignment();
            return new AssignmentExpression(left, operator, new BinaryExpression(left, right, operator), false);
        }
        return left;
    }

    private Expression logicalOr() {
        var left = logicalAnd();
        while(match(TokenType.LOGICAL_OR)){
            var operator = previous();
            var right = logicalAnd();
            left = new BinaryExpression(left, right, operator);
        }
        return left;
    }

    private Expression logicalAnd() {
        var left = bitwiseOr();
        while(match(TokenType.LOGICAL_AND)){
            var operator = previous();
            var right = bitwiseOr();
            left = new BinaryExpression(left, right, operator);
        }
        return left;
    }

    private Expression bitwiseOr() {
        var left = bitwiseXor();
        while(match(TokenType.BITWISE_OR)){
            var operator = previous();
            var right = bitwiseXor();
            left = new BinaryExpression(left, right, operator);
        }
        return left;
    }

    private Expression bitwiseXor() {
        var left = bitwiseAnd();
        while(match(TokenType.BITWISE_XOR)){
            var operator = previous();
            var right = bitwiseAnd();
            left = new BinaryExpression(left, right, operator);
        }
        return left;
    }

    private Expression bitwiseAnd() {
        var left = equality();
        while(match(TokenType.AMPERSAND)){
            var operator = previous();
            var right = equality();
            left = new BinaryExpression(left, right, operator);
        }
        return left;
    }

    private Expression equality() {
        var left = comparison();
        while(match(TokenType.DOUBLE_EQUALS, TokenType.NOT_EQUALS)){
            Token op = previous();
            var right = comparison();
            left = new BinaryExpression(left, right, op);
        }
        return left;
    }

    private Expression comparison() {
        var left = shift();
        while(match(TokenType.GREATER_THAN, TokenType.LESS_THAN, TokenType.GREATER_THAN_OR_EQUAL, TokenType.LESS_THAN_OR_EQUAL)){
            var op = previous();
            var right = shift();
            left = new BinaryExpression(left, right, op);
        }
        return left;
    }

    private Expression shift() {
        var left = addition();
        while(match(TokenType.BITWISE_LEFT, TokenType.BITWISE_RIGHT)){
            var op = previous();
            var right = addition();
            left = new BinaryExpression(left, right, op);
        }
        return left;
    }

    private Expression addition() {
        var left = multiplication();
        while(match(TokenType.PLUS, TokenType.MINUS)){
            Token op = previous();
            var right = multiplication();
            left = new BinaryExpression(left, right, op);
        }
        return left;
    }

    private Expression multiplication() {
        var left = leftUnary();
        while(match(TokenType.STAR, TokenType.SLASH)){
            var op = previous();
            var right = leftUnary();
            left = new BinaryExpression(left, right, op);
        }
        return left;
    }

    private Expression leftUnary() {
        if(isCastExpressionAhead()){
            return castExpression();
        }

        if(match(TokenType.SIZEOF)){
            Token sizeofToken = previous();
            consume("expected '('", TokenType.L_PAREN);
            var decl = declarator(VarDeclRules.SIZEOF_DECL);
            if(decl != null){
                consume("expected ')'", TokenType.R_PAREN);
                return new SizeofExpression(sizeofToken, decl.typeSpecifier());
            }else{
                Expression operand = expression();
                consume("expected ')'", TokenType.R_PAREN);
                return new SizeofExpression(sizeofToken, operand);
            }
        }

        if(match(TokenType.MINUS, TokenType.LOGICAL_NOT, TokenType.BITWISE_NOT, TokenType.STAR, TokenType.AMPERSAND, TokenType.DOUBLE_PLUS, TokenType.DOUBLE_MINUS)){
            var op = previous();
            var right = leftUnary();
            return new UnaryExpression(right, op, UnaryExpression.UnaryPosition.PRE);
        }
        return rightUnary();
    }

    private Expression castExpression() {
        var castToken = consume("expected '('", TokenType.L_PAREN);
        var targetType = castTypeSpecifier();
        consume("expected ')'", TokenType.R_PAREN);

        // Cast precedence matches other left-unary operators.
        var operand = leftUnary();
        return new CastExpression(castToken, targetType, operand);
    }

    private TypeSpecifier castTypeSpecifier() {
        var typeSpecifier = typeSpecifier(StorageQualifier.NONE);

        if(typeSpecifier == null){
            throw new CompileException("expected type specifier in cast expression", peek());
        }

        if(typeSpecifier.type == TypeSpecifier.Type.STRUCT && ((StructType) typeSpecifier).providesDefinition()){
            throw new CompileException("struct definition is not allowed in cast expression", ((StructType) typeSpecifier).getName());
        }

        return typeSpecifier;
    }

    private boolean isCastExpressionAhead() {
        if(!check(TokenType.L_PAREN)){
            return false;
        }

        var savedIndex = index;
        try {
            advance(); // consume '('

            var typeSpecifier = typeSpecifier(StorageQualifier.NONE);
            if(typeSpecifier == null){
                return false;
            }

            if(typeSpecifier.type == TypeSpecifier.Type.STRUCT && ((StructType) typeSpecifier).providesDefinition()){
                return false;
            }

            return check(TokenType.R_PAREN);
        } catch (CompileException ignored) {
            return false;
        } finally {
            index = savedIndex;
        }
    }

    private Expression rightUnary() {
        var left = memberAccessAndSubscript();
        if(match(TokenType.DOUBLE_PLUS, TokenType.DOUBLE_MINUS)){
            var op = previous();
            return new UnaryExpression(left, op, UnaryExpression.UnaryPosition.POST);
        }
        return left;
    }

    private Expression memberAccessAndSubscript() {
        var left = call();
        while (match(TokenType.ARROW, TokenType.DOT, TokenType.L_SQUARE_BRACKET)) {
            var op = previous();
            if (op.type == TokenType.L_SQUARE_BRACKET) {
                var right = expression();
                consume("expected ']'", TokenType.R_SQUARE_BRACKET);
                left = new SubscriptExpression(left, right);
            } else {
                var right = consume("expected member access", TokenType.IDENTIFIER);
                left = new MemberAccessExpression(left, right, op);
            }
        }
        return left;
    }

    private Expression call() {
        var left = primary();
        while(match(TokenType.L_PAREN)){
            var arguments = new ArrayList<Expression>();
            while(!check(TokenType.R_PAREN)){
                arguments.add(expression());
                if(!match(TokenType.COMMA)){
                    break;
                }
            }
            consume("expected ')'", TokenType.R_PAREN);
            left = new CallExpression(left, arguments.toArray(new Expression[0]));
        }
        return left;
    }

    private Expression primary() {
        if(match(TokenType.L_PAREN)){
            var expression = expression();
            consume("expected ')'", TokenType.R_PAREN);
            return expression;
        }else if(match(TokenType.IDENTIFIER)){
            return new IdentifierExpression(previous());
        }else if(match(TokenType.INTEGER)){
            return new NumericalExpression(previous());
        }else if(match(TokenType.STRING)){
            var string = previous();

            if(string.lexeme.startsWith("'")){
                if(((String) string.literal).length() > 1)
                    throw new CompileException("invalid character literal", string);

                return new NumericalExpression(string, ((String) string.literal).charAt(0));
            }

            return new StringExpression(string);
        }else{
            throw new CompileException("expected expression", peek());
        }
    }




    private void sync() {
        while(!isAtEnd()) {
            if (check(SYNC_TOKENS)) {
                return;
            }
            advance();
        }
    }
}
