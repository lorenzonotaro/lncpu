import * as vscode from "vscode";

import { LncpuDebugAdapter } from "./debug/dap";
import { activate as activateLanguageFeatures, deactivate as deactivateLanguageFeatures } from "./extension";

export function activate(context: vscode.ExtensionContext): void {
  activateLanguageFeatures(context);
  context.subscriptions.push(vscode.debug.registerDebugAdapterDescriptorFactory("lncpu", {
    createDebugAdapterDescriptor: () => new vscode.DebugAdapterInlineImplementation(new LncpuDebugAdapter()),
  }));
  context.subscriptions.push(vscode.debug.registerDebugConfigurationProvider("lncpu", {
    resolveDebugConfiguration: (_folder, configuration) => ({
      cwd: "${workspaceFolder}",
      compilerOptions: [],
      emulatorOptions: [],
      stopOnEntry: true,
      ...configuration,
    }),
  }));
}

export function deactivate(): void {
  deactivateLanguageFeatures();
}
