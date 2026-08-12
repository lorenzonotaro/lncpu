export class DebugConfigurationError extends Error {
  readonly name = "DebugConfigurationError";
}

export class ProcessFailureError extends Error {
  readonly name = "ProcessFailureError";

  constructor(readonly program: string, readonly exitCode: number | null) {
    super(`${program} exited with code ${exitCode ?? "unknown"}`);
  }
}

export class ProtocolError extends Error {
  readonly name = "ProtocolError";
}
