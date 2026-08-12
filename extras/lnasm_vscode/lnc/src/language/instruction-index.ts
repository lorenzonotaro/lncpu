import { isAbsolute } from "node:path";
import * as vscode from "vscode";

export type InstrDoc = {
  readonly opcode: string;
  readonly name: string;
  readonly dataLength: string;
  readonly clockCycles: string;
  readonly description: string;
  readonly flagsModified: string;
  readonly columns: readonly string[];
};

export class InstrIndex {
  private readonly byKey = new Map<string, InstrDoc>();
  private rows: readonly InstrDoc[] = [];
  private headers: readonly string[] = [];

  constructor(private readonly context: vscode.ExtensionContext) {}

  async loadFromConfig(): Promise<void> {
    const path = vscode.workspace.getConfiguration("lnasmBasics").get<string>("instrTsvPath");
    if (path === undefined || path.length === 0) return;
    const uri = isAbsolute(path) ? vscode.Uri.file(path) : vscode.Uri.joinPath(this.context.extensionUri, path);
    try {
      const data = await vscode.workspace.fs.readFile(uri);
      this.ingestTsv(new TextDecoder("utf-8").decode(data));
    } catch (error: unknown) {
      console.warn("[lnasmBasics] Could not load instruction TSV at", uri.toString(), error);
      this.byKey.clear();
      this.rows = [];
      this.headers = [];
    }
  }

  findByMnemonicPrefix(mnemonic: string | undefined): readonly InstrDoc[] {
    if (mnemonic === undefined) return [];
    const normalized = mnemonic.toUpperCase();
    return this.rows.filter((row) => row.name.toUpperCase().startsWith(normalized));
  }

  getHeaders(): readonly string[] {
    return this.headers.length > 0 ? this.headers : ["Opcode", "Name", "Data length", "Clock cycles", "Description", "Flags modified"];
  }

  private ingestTsv(text: string): void {
    this.byKey.clear();
    const lines = text.split(/\r?\n/).filter((line) => line.trim().length > 0);
    const first = lines[0]?.split("\t").map((column) => column.trim()) ?? [];
    const hasHeader = first.length >= 6 && /opcode/i.test(first[0] ?? "") && /name/i.test(first[1] ?? "");
    this.headers = hasHeader ? first : [];
    const rows: InstrDoc[] = [];
    for (const line of lines.slice(hasHeader ? 1 : 0)) {
      const columns = line.split("\t").map((column) => column.trim());
      if (columns.length < 6) continue;
      const [opcode = "", name = "", dataLength = "", clockCycles = "", description = "", flagsModified = ""] = columns;
      const record = { opcode, name, dataLength, clockCycles, description, flagsModified, columns };
      if (opcode.length > 0) this.byKey.set(opcode.toUpperCase(), record);
      if (name.length > 0) this.byKey.set(name.toUpperCase(), record);
      rows.push(record);
    }
    this.rows = rows;
  }
}

export function escapeCell(value: string): string {
  return value.replace(/\|/g, "\\|");
}
