package com.lnc.common.frontend;

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
