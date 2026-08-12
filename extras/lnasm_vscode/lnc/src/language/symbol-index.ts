import * as vscode from "vscode";

import { activeAddresses } from "../debug/address-registry";

export type SymKind = "label" | "sublabel" | "macro" | "section";
export type SymDef = {
  readonly name: string;
  readonly kind: SymKind;
  readonly uri: vscode.Uri;
  readonly range: vscode.Range;
  readonly defLine: number;
  readonly topLabel?: string;
};

export class LnasnIndex {
  private readonly symbols = new Map<string, SymDef[]>();
  private readonly docVersions = new Map<string, number>();

  async buildWorkspace(globs: readonly string[]): Promise<void> {
    this.symbols.clear();
    this.docVersions.clear();
    for (const glob of globs) {
      for (const uri of await vscode.workspace.findFiles(glob)) this.indexDocument(await vscode.workspace.openTextDocument(uri));
    }
  }

  getAllSymbols(): readonly SymDef[] {
    return [...this.symbols.values()].flat();
  }

  searchSymbols(query: string): readonly SymDef[] {
    const normalized = query.trim().toLowerCase();
    if (normalized.length === 0) return this.getAllSymbols();
    return this.getAllSymbols().filter((symbol) => `${symbol.topLabel === undefined ? "" : `${symbol.topLabel}.`}${symbol.name}`.toLowerCase().includes(normalized));
  }

  indexDocument(document: vscode.TextDocument): void {
    if (document.languageId !== "lnasm" && !/\.(lnasm|s)$/i.test(document.fileName)) return;
    const uri = document.uri.toString();
    if (this.docVersions.get(uri) === document.version) return;
    this.docVersions.set(uri, document.version);
    for (const [key, definitions] of this.symbols) this.symbols.set(key, definitions.filter((definition) => definition.uri.toString() !== uri));
    let topLabel: string | undefined;
    for (let line = 0; line < document.lineCount; line += 1) {
      const text = document.lineAt(line).text;
      const label = /^\s*([A-Za-z_][A-Za-z0-9_]*)\s*:\s*(?:;.*)?$/.exec(text);
      if (label !== null) {
        const name = label[1] ?? "";
        const sublabel = name.startsWith("_");
        this.add({ name, kind: sublabel ? "sublabel" : "label", uri: document.uri, range: this.range(line, text, name), defLine: line, ...(sublabel && topLabel !== undefined ? { topLabel } : {}) });
        if (!sublabel) topLabel = name;
        continue;
      }
      const define = /^\s*%define\s+([A-Za-z_][A-Za-z0-9_]*)\b/.exec(text);
      if (define !== null) {
        const name = define[1] ?? "";
        this.add({ name, kind: "macro", uri: document.uri, range: this.range(line, text, name), defLine: line });
        continue;
      }
      const section = /^\s*\.section\s+([A-Za-z_][A-Za-z0-9_]*)\b/.exec(text);
      if (section !== null) {
        const name = section[1] ?? "";
        this.add({ name, kind: "section", uri: document.uri, range: this.range(line, text, name), defLine: line });
      }
    }
  }

  resolveAt(document: vscode.TextDocument, position: vscode.Position): readonly SymDef[] {
    const wordRange = document.getWordRangeAtPosition(position, /[A-Za-z_][A-Za-z0-9_]*/);
    if (wordRange === undefined) return [];
    const symbol = document.getText(wordRange);
    if (symbol.startsWith("_")) {
      const scoped = this.symbols.get(`sublabel::${this.findTopLabel(document, position.line) ?? ""}::${symbol}`);
      if (scoped !== undefined && scoped.length > 0) return scoped;
    }
    for (const kind of ["label", "macro", "section"] as const) {
      const definitions = this.symbols.get(`${kind}::${symbol}`);
      if (definitions !== undefined && definitions.length > 0) return definitions;
    }
    return [];
  }

  resolveActiveAddress(document: vscode.TextDocument, position: vscode.Position): { readonly symbol: string; readonly address: number } | undefined {
    const definition = this.resolveAt(document, position)[0];
    if (definition === undefined || definition.kind === "macro") return undefined;
    const emitted = definition.kind === "sublabel" && definition.topLabel !== undefined ? `${definition.topLabel}$${definition.name}` : definition.name;
    const address = activeAddresses.resolve(emitted);
    return address === undefined ? undefined : { symbol: definition.name, address };
  }

  async getLeadingCommentBlock(definition: SymDef): Promise<string | undefined> {
    const document = vscode.workspace.textDocuments.find((candidate) => candidate.uri.toString() === definition.uri.toString())
      ?? await vscode.workspace.openTextDocument(definition.uri);
    const lines: string[] = [];
    for (let line = definition.defLine - 1; line >= 0; line -= 1) {
      const text = document.lineAt(line).text;
      if (!/^\s*;/.test(text)) break;
      lines.unshift(text.replace(/^\s*;\s?/, ""));
    }
    return lines.length === 0 ? undefined : lines.join("\n");
  }

  private add(definition: SymDef): void {
    const key = definition.kind === "sublabel" ? `${definition.kind}::${definition.topLabel ?? ""}::${definition.name}` : `${definition.kind}::${definition.name}`;
    this.symbols.set(key, [...(this.symbols.get(key) ?? []), definition]);
  }

  private range(line: number, text: string, name: string): vscode.Range {
    const start = new vscode.Position(line, text.indexOf(name));
    return new vscode.Range(start, start.translate(0, name.length));
  }

  private findTopLabel(document: vscode.TextDocument, fromLine: number): string | undefined {
    for (let line = fromLine; line >= 0; line -= 1) {
      const match = /^\s*([A-Za-z][A-Za-z0-9_]*)\s*:\s*(?:;.*)?$/.exec(document.lineAt(line).text);
      if (match !== null) return match[1];
    }
    return undefined;
  }
}

export function toVscodeKind(kind: SymKind): vscode.SymbolKind {
  switch (kind) {
    case "label": return vscode.SymbolKind.Function;
    case "sublabel": return vscode.SymbolKind.Namespace;
    case "macro": return vscode.SymbolKind.Constant;
    case "section": return vscode.SymbolKind.Module;
  }
}
