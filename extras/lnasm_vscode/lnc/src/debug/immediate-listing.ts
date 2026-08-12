const MAX_PHYSICAL_ADDRESS = 0xffffff;
const SECTION = /^#{8} Section '([^']+)', origin at 0x([0-9a-fA-F]+) \(size = 0x[0-9a-fA-F]+\)$/;
const INSTRUCTION = /^(.*?)\b([0-9a-fA-F]{6,}):/;
const LABEL = /^[A-Za-z_.$#][A-Za-z0-9_.$#]*$/;

function physicalAddress(hex: string): number | undefined {
  const address = Number.parseInt(hex, 16);
  return Number.isSafeInteger(address) && address <= MAX_PHYSICAL_ADDRESS ? address : undefined;
}

export function parseImmediateListing(listing: string): ReadonlyMap<string, number> {
  const symbols = new Map<string, number>();
  for (const rawLine of listing.split(/\r?\n/)) {
    const line = rawLine.trim();
    const section = SECTION.exec(line);
    if (section !== null) {
      const name = section[1];
      const address = physicalAddress(section[2] ?? "");
      if (name !== undefined && address !== undefined) symbols.set(name, address);
      continue;
    }
    const instruction = INSTRUCTION.exec(line);
    if (instruction === null) continue;
    const prefix = instruction[1]?.trimEnd();
    const address = physicalAddress(instruction[2] ?? "");
    if (prefix === undefined || address === undefined || !prefix.endsWith(":")) continue;
    for (const rawLabel of prefix.slice(0, -1).split(",")) {
      const label = rawLabel.trim();
      if (LABEL.test(label)) symbols.set(label, address);
    }
  }
  return symbols;
}
