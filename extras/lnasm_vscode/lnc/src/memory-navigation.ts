import * as vscode from 'vscode';

export type MemorySymbolArgument = { readonly symbol: string; readonly address: number };
export type DebugSessionIdentity = { readonly id: string; readonly type: string };

export interface MemoryNavigationHost {
  activeSession(): DebugSessionIdentity | undefined;
  execute(command: string, argument: unknown): Promise<unknown>;
}

export class MemoryNavigationError extends Error {
  readonly name = "MemoryNavigationError";
}

export function parseMemorySymbolArgument(value: unknown): MemorySymbolArgument | undefined {
  if (typeof value !== "object" || value === null) return undefined;
  if (!("symbol" in value) || !("address" in value)) return undefined;
  const symbol = value.symbol;
  const address = value.address;
  if (typeof symbol !== "string" || symbol.length === 0) return undefined;
  if (typeof address !== "number" || !Number.isInteger(address) || address < 0 || address > 0xffffff) return undefined;
  return { symbol, address };
}

export async function openMemoryAtSymbol(host: MemoryNavigationHost, argument: MemorySymbolArgument): Promise<void> {
  const session = host.activeSession();
  if (session?.type !== "lncpu") throw new MemoryNavigationError("Memory navigation requires an active LNCPU debug session");
  const memoryReference = `0x${argument.address.toString(16)}`;
  await host.execute("memory-inspector.show", { sessionId: session.id, memoryReference });
  await host.execute("memory-inspector.show-variable", {
    sessionId: session.id,
    variable: {
      name: argument.symbol,
      value: memoryReference,
      type: "address",
      variablesReference: 0,
      memoryReference,
    },
  });
}
