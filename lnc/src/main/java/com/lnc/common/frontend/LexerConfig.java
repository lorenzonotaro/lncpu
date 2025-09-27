package com.lnc.common.frontend;

/**
 * Configuration settings for the lexical analyzer (lexer) used in a parser or compiler.
 * This configuration is used to customize the behavior and functionality of the lexer.
 * It specifies how keywords, preprocessors, comments, directives, and case sensitivity are handled.
 *
 * @param keywordSet               Array of TokenType keywords recognized by the lexer.
 * @param preprocessorConfig       Configures handling of preprocessor directives (e.g., disabled, C-style, or assembly style).
 * @param singleLineCommentsConfig Configures handling of single-line comments (e.g., disabled, C-style, or assembly style).
 * @param multiLineCommentsConfig  Configures handling of multi-line comments (e.g., disabled or C-style).
 * @param directivesEnabled        Boolean flag indicating whether directives are enabled.
 * @param caseSensitive            Boolean flag indicating whether the lexer operates in a case-sensitive manner.
 */
public record LexerConfig(TokenType[] keywordSet,
                          PreprocessorConfig preprocessorConfig,
                          SingleLineCommentsConfig singleLineCommentsConfig,
                          MultiLineCommentsConfig multiLineCommentsConfig,
                          boolean directivesEnabled,
                          boolean caseSensitive
                          ) {

    public enum SingleLineCommentsConfig {
        DISABLED,
        C_STYLE,
        ASM_STYLE
    }

    public enum MultiLineCommentsConfig {
        DISABLED,
        C_STYLE,
    }

    public enum PreprocessorConfig {
        DISABLED(""),
        C_STYLE("#"),
        ASM_STYLE("#");

        public final String preprocessorChar;

        PreprocessorConfig(String preprocessorChar) {
            this.preprocessorChar = preprocessorChar;
        }
    }
}
