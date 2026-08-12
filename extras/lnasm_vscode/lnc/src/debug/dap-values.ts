import { DebugConfigurationError, ProtocolError } from "./errors";

export function record(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null;
}

export function requiredString(value: Record<string, unknown>, key: string): string {
  const field = value[key];
  if (typeof field !== "string" || field.length === 0) throw new DebugConfigurationError(`${key} must be a non-empty string`);
  return field;
}

export function optionalString(value: Record<string, unknown>, key: string, fallback: string): string {
  const field = value[key];
  if (field === undefined) return fallback;
  if (typeof field !== "string") throw new DebugConfigurationError(`${key} must be a string`);
  return field;
}

export function stringArray(value: Record<string, unknown>, key: string): readonly string[] {
  const field = value[key];
  if (!Array.isArray(field) || field.some((item) => typeof item !== "string")) {
    throw new DebugConfigurationError(`${key} must be a string array`);
  }
  return field.filter((item): item is string => typeof item === "string");
}

export function integer(value: Record<string, unknown>, key: string, fallback?: number): number {
  const field = value[key];
  if (field === undefined && fallback !== undefined) return fallback;
  if (typeof field !== "number" || !Number.isInteger(field)) throw new DebugConfigurationError(`${key} must be an integer`);
  return field;
}

export function parseAddress(value: string): number {
  if (!/^(?:0x[0-9a-f]+|\d+)$/i.test(value)) throw new ProtocolError(`invalid memory reference: ${value}`);
  const address = Number(value);
  if (!Number.isSafeInteger(address) || address < 0 || address > 0xffff) throw new ProtocolError(`address out of range: ${value}`);
  return address;
}

export function parseRegisterPayload(payload: string): readonly { readonly name: string; readonly value: string }[] {
  return payload.split(" ").map((entry) => {
    const match = /^([A-Z_]+)=([0-9a-f]+)$/i.exec(entry);
    if (match === null) throw new ProtocolError(`invalid register payload: ${entry}`);
    return { name: match[1], value: `0x${match[2]}` };
  });
}
