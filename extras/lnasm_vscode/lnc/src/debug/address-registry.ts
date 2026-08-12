export class AddressRegistry {
  private symbols: ReadonlyMap<string, number> = new Map();

  resolve(symbol: string): number | undefined {
    return this.symbols.get(symbol);
  }

  replace(symbols: ReadonlyMap<string, number>): void {
    this.symbols = new Map(symbols);
  }

  clear(): void {
    this.symbols = new Map();
  }
}

export const activeAddresses = new AddressRegistry();
