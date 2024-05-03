package com.lnasm.compiler.parser;

public record ParseResult(ParsedBlock[] blocks, ParserLabelSectionLocator labelSectionLocator) {
}
