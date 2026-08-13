import * as vscode from 'vscode';

import { activeAddresses } from './debug/address-registry';
import { escapeCell, InstrIndex } from './language/instruction-index';
import { LnasnIndex, toVscodeKind } from './language/symbol-index';
import { openMemoryAtSymbol, parseMemorySymbolArgument } from './memory-navigation';

export function activate(context: vscode.ExtensionContext) {
  const idx = new LnasnIndex();
  const instr = new InstrIndex(context);

  const config = vscode.workspace.getConfiguration('lnasmBasics');
  const globs = config.get<string[]>('indexGlobs', ["**/*.lnasm", "**/*.s"]);

  idx.buildWorkspace(globs);
  instr.loadFromConfig();

  context.subscriptions.push({ dispose: () => activeAddresses.clear() });
  context.subscriptions.push(vscode.commands.registerCommand('lncpu.openMemoryAtSymbol', async (raw: unknown) => {
    const argument = parseMemorySymbolArgument(JSON.parse(raw as string));
    if (argument === undefined) {
      await vscode.window.showErrorMessage('Invalid LNCPU memory symbol address.');
      return;
    }
    try {
      await openMemoryAtSymbol({
        activeSession: () => vscode.debug.activeDebugSession,
        execute: async (command, value) => vscode.commands.executeCommand(command, value),
      }, argument);
    } catch (error: unknown) {
      const message = error instanceof Error ? error.message : 'Memory Inspector command failed';
      await vscode.window.showErrorMessage(`${message}. Install Eclipse CDT Memory Inspector and add "lncpu" to memory-inspector.debugTypes.`);
    }
  }));

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
        const active = vscode.debug.activeDebugSession;
        const memory = active?.type === 'lncpu' ? idx.resolveActiveAddress(doc, pos) : undefined;
        if (memory !== undefined) {
          vscode.commands.executeCommand('lncpu.openMemoryAtSymbol', JSON.stringify(memory));
          return undefined;
        }
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
