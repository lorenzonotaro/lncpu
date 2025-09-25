import * as vscode from 'vscode';

type SymKind = 'label' | 'sublabel' | 'macro' | 'section';

interface SymDef {
  name: string;                 // raw name (e.g., "FOO", "_bar", "MY_MACRO", "CODE")
  kind: SymKind;
  uri: vscode.Uri;
  range: vscode.Range;          // the identifier range
  defLine: number;              // line number of the definition (for scanning comments)
  topLabel?: string;            // for sublabels: the nearest enclosing top-level label
}

interface InstrDoc {
  opcode: string;
  name: string;
  dataLength: string;
  clockCycles: string;
  description: string;
  flagsModified: string;
  columns: string[];   // raw row, for "all-columns" table rendering
}

class InstrIndex {
  private byKey = new Map<string, InstrDoc>(); // still useful for exact lookups
  private rows: InstrDoc[] = [];               // full table for prefix searches
  private headers: string[] = [];              // TSV headers as-is
  private lastUri: vscode.Uri | null = null;

  constructor(private ctx: vscode.ExtensionContext) {}

  async loadFromConfig() {
    const cfg = vscode.workspace.getConfiguration('lnasmBasics');
    const p = cfg.get<string>('instrTsvPath');
    if (!p) return;

    const uri = this.resolvePathFromExtension(p);
    this.lastUri = uri;
    await this.loadFromUri(uri);
  }

  dispose() {}

  private resolvePathFromExtension(p: string): vscode.Uri {
    const path = require('path');
    if (path.isAbsolute(p)) return vscode.Uri.file(p);
    if (/^[a-z]+:\/\//i.test(p)) return vscode.Uri.parse(p);
    return vscode.Uri.joinPath(this.ctx.extensionUri, p);
  }

  private async loadFromUri(uri: vscode.Uri) {
    try {
      const data = await vscode.workspace.fs.readFile(uri);
      const text = new TextDecoder('utf-8').decode(data);
      this.ingestTsv(text);
    } catch (e) {
      console.warn('[lnasmBasics] Could not load instruction TSV at', uri.toString(), e);
      this.byKey.clear();
      this.rows = [];
      this.headers = [];
    }
  }

  private ingestTsv(text: string) {
    this.byKey.clear();
    this.rows = [];
    this.headers = [];

    const lines = text.split(/\r?\n/).filter(l => l.trim().length > 0);
    if (lines.length === 0) return;

    const firstCols = lines[0].split('\t').map(s => s.trim());
    const looksHeader = firstCols.length >= 6 &&
      /opcode/i.test(firstCols[0]) && /name/i.test(firstCols[1]);

    let start = 0;
    if (looksHeader) {
      this.headers = firstCols;
      start = 1;
    } else {
      // default headers if none provided
      this.headers = ["Opcode", "Name", "Data length", "Clock cycles", "Description", "Flags modified"];
    }

    for (let i = start; i < lines.length; i++) {
      const cols = lines[i].split('\t').map(s => s.trim());
      if (cols.length < 6) continue;
      const [opcode, name, dataLen, clocks, desc, flags] = cols;
      const rec: InstrDoc = {
        opcode, name, dataLength: dataLen, clockCycles: clocks,
        description: desc, flagsModified: flags, columns: cols
      };
      if (opcode) this.byKey.set(opcode.toUpperCase(), rec);
      if (name)   this.byKey.set(name.toUpperCase(), rec);
      this.rows.push(rec);
    }
  }

  /** Return all rows whose Name column starts with the given mnemonic (case-insensitive). */
  findByMnemonicPrefix(mnemonic: string | undefined): InstrDoc[] {
    if (!mnemonic) return [];
    const m = mnemonic.toUpperCase();
    return this.rows.filter(r => r.name.toUpperCase().startsWith(m));
  }

  getHeaders(): string[] {
    return this.headers.length ? this.headers : ["Opcode", "Name", "Data length", "Clock cycles", "Description", "Flags modified"];
  }
}



class LnasnIndex {
  private symbols = new Map<string, SymDef[]>(); // key -> list of definitions
  private docVersions = new Map<string, number>();

  constructor(private ctx: vscode.ExtensionContext) {}

  async buildWorkspace(globs: string[]) {
    this.symbols.clear();
    this.docVersions.clear();
    for (const g of globs) {
      const uris = await vscode.workspace.findFiles(g);
      for (const uri of uris) {
        const doc = await vscode.workspace.openTextDocument(uri);
        this.indexDocument(doc);
      }
    }
  }

  getAllSymbols(): SymDef[] {
    const out: SymDef[] = [];
    for (const [, defs] of this.symbols) out.push(...defs);
    return out;
  }

  searchSymbols(query: string): SymDef[] {
    const q = query.trim().toLowerCase();
    if (!q) return this.getAllSymbols();
    return this.getAllSymbols().filter(s => {
      // name match, or "topLabel.name" match for sublabels
      const full = s.topLabel ? `${s.topLabel}.${s.name}` : s.name;
      return full.toLowerCase().includes(q);
    });
  }

  indexDocument(doc: vscode.TextDocument) {
    if (doc.languageId !== 'lnasm' && !doc.fileName.match(/\.(lnasm|s)$/i)) return;
    const key = doc.uri.toString();
    const ver = doc.version;
    const prev = this.docVersions.get(key);
    if (prev === ver) return; // unchanged
    this.docVersions.set(key, ver);

    // Remove previous symbols for this doc
    for (const [k, list] of this.symbols) {
      this.symbols.set(k, list.filter(s => s.uri.toString() !== key));
    }

    // Simple line-based parse
    const labelRe = /^\s*([A-Za-z_][A-Za-z0-9_]*)\s*:\s*(?:;.*)?$/;
    const defineRe = /^\s*%define\s+([A-Za-z_][A-Za-z0-9_]*)\b/;        // macro
    const sectionRe = /^\s*\.section\s+([A-Za-z_][A-Za-z0-9_]*)\b/;     // section

    let currentTopLabel: string | undefined;

    for (let line = 0; line < doc.lineCount; line++) {
      const text = doc.lineAt(line).text;

      // labels
      const lm = text.match(labelRe);
      if (lm) {
        const name = lm[1];
        const start = new vscode.Position(line, text.indexOf(name));
        const range = new vscode.Range(start, start.translate(0, name.length));
        const isSub = name.startsWith('_');

        const def: SymDef = {
          name,
          kind: isSub ? 'sublabel' : 'label',
          uri: doc.uri,
          range,
          defLine: line,
          topLabel: isSub ? currentTopLabel : undefined,
        };
        this.addSymbol(this.makeKey(def), def);

        // update current top-level label scope
        if (!isSub) currentTopLabel = name;
        continue;
      }

      // %define
      const dm = text.match(defineRe);
      if (dm) {
        const name = dm[1];
        const start = new vscode.Position(line, text.indexOf(name));
        const range = new vscode.Range(start, start.translate(0, name.length));
        const def: SymDef = {
          name,
          kind: 'macro',
          uri: doc.uri,
          range,
          defLine: line,
        };
        this.addSymbol(this.makeKey(def), def);
        continue;
      }

      // .section
      const sm = text.match(sectionRe);
      if (sm) {
        const name = sm[1];
        const start = new vscode.Position(line, text.indexOf(name));
        const range = new vscode.Range(start, start.translate(0, name.length));
        const def: SymDef = {
          name,
          kind: 'section',
          uri: doc.uri,
          range,
          defLine: line,
        };
        this.addSymbol(this.makeKey(def), def);
        continue;
      }
    }
  }

  private addSymbol(key: string, def: SymDef) {
    const arr = this.symbols.get(key) ?? [];
    arr.push(def);
    this.symbols.set(key, arr);
  }
  

  private makeKey(def: Pick<SymDef, 'name' | 'kind' | 'topLabel'>): string {
    if (def.kind === 'sublabel') {
      return `${def.kind}::${def.topLabel ?? ''}::${def.name}`;
    }
    return `${def.kind}::${def.name}`;
  }

  /**
   * Resolve a symbol name at a given position.
   * - If it looks like a sublabel (_name), scope it to the nearest enclosing top-level label in the current doc.
   * - Otherwise try label/macro/section keys in order.
   */
  resolveAt(doc: vscode.TextDocument, pos: vscode.Position): SymDef[] {
    const wordRange = doc.getWordRangeAtPosition(pos, /[A-Za-z_][A-Za-z0-9_]*/);
    if (!wordRange) return [];
    const sym = doc.getText(wordRange);

    // Check sublabel scope
    if (sym.startsWith('_')) {
      const top = this.findEnclosingTopLabel(doc, pos.line);
      const key = `sublabel::${top ?? ''}::${sym}`;
      const got = this.symbols.get(key);
      if (got && got.length) return got;
    }

    // Try plain labels, macros, sections
    const kinds: SymKind[] = ['label', 'macro', 'section'];
    for (const k of kinds) {
      const key = `${k}::${sym}`;
      const got = this.symbols.get(key);
      if (got && got.length) return got;
    }
    return [];
  }

  private findEnclosingTopLabel(doc: vscode.TextDocument, fromLine: number): string | undefined {
    const topRe = /^\s*([A-Za-z][A-Za-z0-9_]*)\s*:\s*(?:;.*)?$/; // no leading underscore
    for (let l = fromLine; l >= 0; l--) {
      const t = doc.lineAt(l).text;
      const m = t.match(topRe);
      if (m) return m[1];
    }
    return undefined;
  }

  async getLeadingCommentBlock(def: SymDef): Promise<string | undefined> {
  // Get the definition's document, opening it if needed.
  let doc = vscode.workspace.textDocuments.find(d => d.uri.toString() === def.uri.toString());
  if (!doc) {
    try {
      doc = await vscode.workspace.openTextDocument(def.uri);
    } catch {
      return undefined;
    }
  }

  const lines: string[] = [];
  for (let l = def.defLine - 1; l >= 0; l--) {
    const text = doc.lineAt(l).text;
    if (/^\s*;/.test(text)) {
      lines.unshift(text.replace(/^\s*;\s?/, ''));
      continue;
    }
    if (/^\s*$/.test(text)) {
      // stop at blank line to keep the docstring tight
      break;
    }
    break; // non-comment line
  }
  if (lines.length === 0) return undefined;
  return lines.join('\n');
}
}

function escapeCell(s: string): string {
  // Escape pipes to avoid breaking the table; keep it minimal
  return (s ?? '').replace(/\|/g, '\\|');
}

function toVscodeKind(k: SymKind): vscode.SymbolKind {
  switch (k) {
    case 'label':    return vscode.SymbolKind.Function;
    case 'sublabel': return vscode.SymbolKind.Namespace;
    case 'macro':    return vscode.SymbolKind.Constant;
    case 'section':  return vscode.SymbolKind.Module;
  }
}

export function activate(context: vscode.ExtensionContext) {
  const idx = new LnasnIndex(context);
  const instr = new InstrIndex(context);

  const config = vscode.workspace.getConfiguration('lnasmBasics');
  const globs = config.get<string[]>('indexGlobs', ["**/*.lnasm", "**/*.s"]);

  idx.buildWorkspace(globs);
  instr.loadFromConfig();

  context.subscriptions.push({ dispose: () => instr.dispose() });

  // Re-index on open/change
  context.subscriptions.push(
    vscode.workspace.onDidOpenTextDocument(doc => idx.indexDocument(doc)),
    vscode.workspace.onDidChangeTextDocument(e => idx.indexDocument(e.document)),
    vscode.workspace.onDidCreateFiles(async () => idx.buildWorkspace(globs)),
    vscode.workspace.onDidDeleteFiles(async () => idx.buildWorkspace(globs)),
    vscode.workspace.onDidRenameFiles(async () => idx.buildWorkspace(globs)),
    vscode.workspace.onDidChangeConfiguration(async ev => {
      if (ev.affectsConfiguration('lnasmBasics.indexGlobs')) {
        const newGlobs = vscode.workspace.getConfiguration('lnasmBasics').get<string[]>('indexGlobs', globs);
        await idx.buildWorkspace(newGlobs);
      }
    })
  );

  // Definition Provider
  context.subscriptions.push(
    vscode.languages.registerDefinitionProvider({ language: 'lnasm' }, {
      provideDefinition(doc, pos) {
        const defs = idx.resolveAt(doc, pos);
        if (defs.length === 0) return undefined;
        // Pick the first for now (basic behavior)
        const d = defs[0];
        return new vscode.Location(d.uri, d.range);
      }
    })
  );

  // Hover Provider (documentation from leading comment block)
  context.subscriptions.push(
  vscode.languages.registerHoverProvider({ language: 'lnasm' }, {
    async provideHover(doc, pos) {
        const wordRange = doc.getWordRangeAtPosition(pos, /[A-Za-z_][A-Za-z0-9_]*/);
        const word = wordRange ? doc.getText(wordRange) : undefined;

        // 1) Instruction hover (TSV)
        const matches = instr.findByMnemonicPrefix(word);
        if (matches.length > 0) {
          const headers = instr.getHeaders();
          const md = new vscode.MarkdownString();
          md.isTrusted = false;
          md.supportHtml = false;

          // Table header
          md.appendMarkdown(
            `| ${headers.map(h => escapeCell(h)).join(' | ')} |\n` +
            `| ${headers.map(() => '---').join(' | ')} |\n`
          );

          // Rows
          for (const rec of matches) {
            // pad/truncate to headers length to avoid misaligned rows
            const row = rec.columns.slice(0, headers.length);
            while (row.length < headers.length) row.push('');
            md.appendMarkdown(`| ${row.map(escapeCell).join(' | ')} |\n`);
          }

          return new vscode.Hover(md, wordRange ?? undefined);
        }
        const defs = idx.resolveAt(doc, pos);
        if (defs.length === 0) return undefined;

        const d = defs[0];
        const comments = await idx.getLeadingCommentBlock(d);
        if (!comments) return undefined;

        const header = `**${d.kind}** \`${d.name}\``;
        const md = new vscode.MarkdownString(`${header}\n\n${comments}`);
        md.appendMarkdown(`\n\n— _${vscode.workspace.asRelativePath(d.uri)}:${d.defLine + 1}_`);
        md.isTrusted = false;
        md.supportHtml = false;
        return new vscode.Hover(md, doc.getWordRangeAtPosition(pos, /[A-Za-z_][A-Za-z0-9_]*/));
      }
    })
  );

  context.subscriptions.push(
    vscode.languages.registerWorkspaceSymbolProvider({
      provideWorkspaceSymbols(query: string, _token: vscode.CancellationToken) {
        const matches = idx.searchSymbols(query);
        // Return as WorkspaceSymbol for modern API
        return matches.map(m => new vscode.SymbolInformation(
          m.topLabel ? `${m.topLabel}.{m.name}` : m.name,
          toVscodeKind(m.kind),
          '', // containerName is optional, empty for now
          new vscode.Location(m.uri, m.range)
        ));
      },
      resolveWorkspaceSymbol(sym: vscode.SymbolInformation, _token: vscode.CancellationToken) {
        // Nothing extra to resolve for now
        return sym;
      }
    })
  );
  vscode.languages.setLanguageConfiguration('lnasm', {
  onEnterRules: [
    {
      // If the previous line is a comment and the next line is a comment,
      // insert a "; " on the line we just created (after indentation).
      // We also handle single-line continuation when only prev is a comment.
      beforeText: /^\s*;/,
      afterText: /\s*^\s*;/,
      action: {
        indentAction: vscode.IndentAction.None,
        appendText: '; '
      }
    }
  ]
});
}

export function deactivate() {}
