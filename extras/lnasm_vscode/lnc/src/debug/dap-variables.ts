import { integer, parseRegisterPayload } from "./dap-values";
import type { RamMemoryVariable } from "./debug-map";
import { ProtocolError } from "./errors";

export const REGISTERS_VARIABLES_REFERENCE = 1;
export const RAM_VARIABLES_REFERENCE = 2;

export type DapVariable = {
  readonly name: string;
  readonly value: string;
  readonly variablesReference: 0;
  readonly type?: "RAM";
  readonly evaluateName?: string;
  readonly memoryReference?: string;
  readonly byteLength?: number;
};

export type DapScope = { readonly name: string; readonly variablesReference: number; readonly expensive: false };

export function scopesForRamVariables(ramVariables: readonly RamMemoryVariable[]): readonly DapScope[] {
  const registers: DapScope = { name: "Registers", variablesReference: REGISTERS_VARIABLES_REFERENCE, expensive: false };
  return ramVariables.length === 0
    ? [registers]
    : [registers, { name: "RAM", variablesReference: RAM_VARIABLES_REFERENCE, expensive: false }];
}

export async function variablesForReference(
  reference: number,
  ramVariables: readonly RamMemoryVariable[],
  loadRegisters: () => Promise<string>,
): Promise<readonly DapVariable[]> {
  switch (reference) {
    case REGISTERS_VARIABLES_REFERENCE:
      return parseRegisterPayload(await loadRegisters()).map((register) => ({
        ...register,
        variablesReference: 0,
        evaluateName: register.name,
      }));
    case RAM_VARIABLES_REFERENCE:
      return ramVariables.map((variable) => {
        const address = `0x${variable.address.toString(16)}`;
        return {
          name: variable.name,
          value: address,
          type: "RAM",
          memoryReference: address,
          byteLength: variable.byteLength,
          variablesReference: 0,
        };
      });
    default:
      throw new ProtocolError(`unknown variables reference: ${reference}`);
  }
}

export function variablesReference(argumentsValue: Record<string, unknown>): number {
  return integer(argumentsValue, "variablesReference");
}
