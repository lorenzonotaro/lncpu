package com.lnc.cc.parser;

import com.lnc.cc.ast.*;
import com.lnc.cc.types.Declarator;
import com.lnc.cc.types.TypeQualifier;
import com.lnc.cc.types.TypeSpecifier;
import com.lnc.common.frontend.CompileException;
import com.lnc.common.frontend.FullSourceParser;
import com.lnc.common.frontend.Token;
import com.lnc.common.frontend.TokenType;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class LncParser extends FullSourceParser<Declaration[]> {

    private final List<Declaration> result;

    private static final TokenType[] SYNC_TOKENS = Stream.of(
            TypeQualifier.VALID_TOKENS,
            TypeSpecifier.VALID_TOKENS,
            new TokenType[]{TokenType.SEMICOLON, TokenType.L_CURLY_BRACE, TokenType.R_CURLY_BRACE}
    ).flatMap(Stream::of).toArray(TokenType[]::new);

    public LncParser(Token[] tokens) {
        super(tokens);
        this.result = new ArrayList<>();
    }

    @Override
    public Declaration[] getResult() {
        return result.toArray(new Declaration[0]);
    }

    @Override
    public boolean parse() {
        boolean success = true;
        while(!isAtEnd()) {
            try{
                result.add(externalDeclaration());
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
        var statement = variableDeclaration(true, false);

        if(statement.type != Statement.Type.DECLARATION){
            throw new CompileException("expected external declaration (variable or function)", peek());
        }

        var variableDeclaration = (VariableDeclaration) statement;

        if(match(TokenType.L_PAREN)){
            var parameters = new ArrayList<VariableDeclaration>();

            while(!check(TokenType.R_PAREN)){
                var decl = variableDeclaration(false, false);
                if(decl.type != Statement.Type.DECLARATION && ((Declaration) decl).declarationType != Declaration.Type.VARIABLE){
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
                return new FunctionDeclaration(variableDeclaration.declarator, variableDeclaration.name, parameters.toArray(new VariableDeclaration[0]), block());
            }else {
                throw new CompileException("expected function body or ';'", peek());
            }
        }else if(match(TokenType.SEMICOLON)){
            return variableDeclaration;
        }

        throw new CompileException("expected ';'", peek());
    }

    private Statement variableDeclaration(boolean allowInitializer, boolean expectSemicolon) {
        Declarator declarator = declarator();

        if(declarator == null){
            return statement();
        }

        //while we have a pointer, keep wrapping the type specifier
        while(match(TokenType.STAR)){
            declarator = Declarator.wrapPointer(declarator);
        }

        Token ident = consume("expected identifier", TokenType.IDENTIFIER);

        VariableDeclaration decl = new VariableDeclaration(declarator, ident, allowInitializer && match(TokenType.EQUALS) ? expression() : null);

        if(expectSemicolon){
            consume("expected ';'", TokenType.SEMICOLON);
        }

        return decl;
    }

    private Declarator declarator(){
        TypeQualifier qualifier;
        qualifier = TypeQualifier.parse(this);
        TypeSpecifier typeSpecifier = typeSpecifier();

        if(typeSpecifier == null){
            return null;
        }

        return new Declarator(qualifier, typeSpecifier);
    }

    private TypeSpecifier typeSpecifier() {
        if(check(TypeSpecifier.VALID_TOKENS)){
            return TypeSpecifier.parsePrimaryType(this);
        }
        return null;
    }


    private BlockStatement block(){
        var statements = new ArrayList<Statement>();
        while(!check(TokenType.R_CURLY_BRACE) && !isAtEnd()){
            statements.add(declaration());
        }
        consume("expected '}'", TokenType.R_CURLY_BRACE);
        return new BlockStatement(statements.toArray(new Statement[0]));
    }

    private Statement declaration() {
        return variableDeclaration(true, true);
    }

    private Statement statement() {

        if(match(TokenType.L_CURLY_BRACE)){
            return block();
        }else if(match(TokenType.RETURN)){
            if(check(TokenType.SEMICOLON)){
                return new ReturnStatement(null);
            }else{
                var expr = expression();
                return new ReturnStatement(expr);
            }
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
                initializer = variableDeclaration(true, true);
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
        }else{
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
            var right = assignment();
            return new AssignmentExpression(left, right);
        }
        return left;
    }

    private Expression logicalOr() {
        var left = logicalAnd();
        while(match(TokenType.LOGICAL_OR)){
            var right = logicalAnd();
            left = new BinaryExpression(left, right, BinaryExpression.Operator.OR);
        }
        return left;
    }

    private Expression logicalAnd() {
        var left = bitwiseOr();
        while(match(TokenType.LOGICAL_AND)){
            var right = bitwiseOr();
            left = new BinaryExpression(left, right, BinaryExpression.Operator.AND);
        }
        return left;
    }

    private Expression bitwiseOr() {
        var left = bitwiseXor();
        while(match(TokenType.BITWISE_OR)){
            var right = bitwiseXor();
            left = new BinaryExpression(left, right, BinaryExpression.Operator.OR);
        }
        return left;
    }

    private Expression bitwiseXor() {
        var left = bitwiseAnd();
        while(match(TokenType.BITWISE_XOR)){
            var right = bitwiseAnd();
            left = new BinaryExpression(left, right, BinaryExpression.Operator.XOR);
        }
        return left;
    }

    private Expression bitwiseAnd() {
        var left = equality();
        while(match(TokenType.AMPERSAND)){
            var right = equality();
            left = new BinaryExpression(left, right, BinaryExpression.Operator.AND);
        }
        return left;
    }

    private Expression equality() {
        var left = comparison();
        while(match(TokenType.DOUBLE_EQUALS, TokenType.NOT_EQUALS)){
            Token op = previous();
            var right = comparison();
            left = new BinaryExpression(left, right, BinaryExpression.Operator.fromTokenType(op));
        }
        return left;
    }

    private Expression comparison() {
        var left = shift();
        while(match(TokenType.GREATER_THAN, TokenType.LESS_THAN, TokenType.GREATER_THAN_OR_EQUAL, TokenType.LESS_THAN_OR_EQUAL)){
            var op = previous();
            var right = shift();
            left = new BinaryExpression(left, right, BinaryExpression.Operator.fromTokenType(op));
        }
        return left;
    }

    private Expression shift() {
        var left = addition();
        while(match(TokenType.BITWISE_LEFT, TokenType.BITWISE_RIGHT)){
            var op = previous();
            var right = addition();
            left = new BinaryExpression(left, right, BinaryExpression.Operator.fromTokenType(op));
        }
        return left;
    }

    private Expression addition() {
        var left = multiplication();
        while(match(TokenType.PLUS, TokenType.MINUS)){
            Token op = previous();
            var right = multiplication();
            left = new BinaryExpression(left, right, BinaryExpression.Operator.fromTokenType(op));
        }
        return left;
    }

    private Expression multiplication() {
        var left = leftUnary();
        while(match(TokenType.STAR, TokenType.SLASH)){
            var op = previous();
            var right = leftUnary();
            left = new BinaryExpression(left, right, BinaryExpression.Operator.fromTokenType(op));
        }
        return left;
    }

    private Expression leftUnary() {
        if(match(TokenType.MINUS, TokenType.LOGICAL_NOT, TokenType.BITWISE_NOT, TokenType.STAR, TokenType.AMPERSAND, TokenType.DOUBLE_PLUS, TokenType.DOUBLE_MINUS, TokenType.SIZEOF)){
            var op = previous();
            var right = leftUnary();
            return new UnaryExpression(right, UnaryExpression.Operator.fromTokenType(op), UnaryExpression.Associativity.LEFT);
        }
        return rightUnary();
    }

    private Expression rightUnary() {
        var left = memberAccess();
        if(match(TokenType.DOUBLE_PLUS, TokenType.DOUBLE_MINUS)){
            var op = previous();
            return new UnaryExpression(left, UnaryExpression.Operator.fromTokenType(op), UnaryExpression.Associativity.RIGHT);
        }
        return left;
    }

    private Expression memberAccess() {
        var left = subscript();
        while(match(TokenType.ARROW, TokenType.DOT)){
            var op = previous();
            var right = consume("expected member access", TokenType.IDENTIFIER);
            left = new MemberAccessExpression(left, right, op);
        }
        return left;
    }

    private Expression subscript() {
        var left = call();
        while(match(TokenType.L_SQUARE_BRACKET)){
            var right = expression();
            consume("expected ']'", TokenType.R_SQUARE_BRACKET);
            left = new SubscriptExpression(left, right);
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
