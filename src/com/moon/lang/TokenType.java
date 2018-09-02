package com.moon.lang;
enum TokenType {
    //Single-character Tokens
    LEFT_PAREN, RIGHT_PAREN, LEFT_BRACE, RIGHT_BRACE, LEFT_SQUARE, RIGHT_SQUARE, COMMA, DOT, MINUS, PLUS, SEMICOLON, SLASH, STAR,

    //one or two character Tokens

    BANG, BANG_EQUAL, GREATER, GREATER_EQUAL, LESS, LESS_EQUAL, EQUAL, EQUAL_EQUAL, VAR_ARROW,

    // combined assignment ops
    PLUS_ASSIGN, MINUS_ASSIGN, SLASH_ASSIGN, STAR_ASSIGN,
    
    //Increment and decrement
    PLUS_PLUS, MINUS_MINUS,
    
    //Literals
    IDENTIFIER, NUMBER, STRING,

    //KEYWORDS

    AND, CLASS, ELSE, FALSE, FN, FOR, IF, NIL, OR, PRINT, FAG, RETURN, SUPER, THIS, TRUE, LET, WHILE,LOOP, BREAK,

    //End
    EOF

}