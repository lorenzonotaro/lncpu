import { strict as assert } from "node:assert";
import { chmod, mkdir, mkdtemp, readFile, writeFile } from "node:fs/promises";
import { createServer } from "node:net";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { test } from "node:test";

import { activeAddresses } from "../debug/address-registry";
import { DebugRuntime } from "../debug/runtime";

test("compiles, handshakes with a hermetic emulator, and terminates cleanly", async (context) => {
  // Given
  const root = await mkdtemp(join(tmpdir(), "lncpu-debug-"));
  const source = join(root, "main.lnc");
  const compiler = join(root, "fake-lnc");
  const emulator = join(root, "fake-emu");
  await writeFile(source, "void main() {}\n");
  await writeFile(compiler, `#!/usr/bin/env node
const fs=require("node:fs"); const path=require("node:path");
const value=(name)=>{const i=process.argv.findIndex((v)=>v===name); const eq=process.argv.find((v)=>v.startsWith(name+"=")); return eq?eq.slice(name.length+1):process.argv[i+1]};
const bin=value("-oB"), map=value("-oG"); fs.mkdirSync(path.dirname(bin),{recursive:true}); fs.writeFileSync(bin,Buffer.from([0])); fs.writeFileSync(map,JSON.stringify({version:1,files:[${JSON.stringify(source)}],lines:[{a:0,s:1,f:0,l:1,c:1,sec:"text"}],sections:[{name:"text",target:"ROM",a:0,s:1}],labels:[{name:"main",a:0,sec:"text"}]}));
`);
  await writeFile(emulator, `#!/usr/bin/env node
const net=require("node:net"); const server=net.createServer((socket)=>{let data=""; socket.on("data",(chunk)=>{data+=chunk; for(;;){const i=data.indexOf("\\n"); if(i<0)break; const line=data.slice(0,i); data=data.slice(i+1); const [id,cmd]=line.split(/\\s+/); if(cmd==="hello")socket.write(id+" ok LNDBG 1\\n"); if(cmd==="quit"){socket.write(id+" ok\\n! exited 0\\n"); server.close();}}});}); server.listen(0,"127.0.0.1",()=>console.log("LNDBG-LISTEN "+server.address().port));
`);
  await Promise.all([chmod(compiler, 0o755), chmod(emulator, 0o755)]);
  const runtime = new DebugRuntime();
  context.after(() => runtime.terminate());

  // When
  const launched = await runtime.launch({
    cwd: root,
    lncPath: compiler,
    javaPath: "java",
    emulatorPath: emulator,
    sourceFiles: [source],
    compilerOptions: [],
    emulatorOptions: [],
    deviceImages: [],
  });
  await runtime.terminate();

  // Then
  assert.equal(launched.map.location(0)?.label, "main");
  assert.deepEqual(await readFile(launched.plan.binaryPath), Buffer.from([0]));
});

test("loads every requested legacy immediate listing when sections metadata is absent", async (context) => {
  // Given
  const root = await mkdtemp(join(tmpdir(), "lncpu-debug-legacy-"));
  const source = join(root, "main.lnc");
  const compiler = join(root, "fake-lnc");
  const emulator = join(root, "fake-emu");
  await writeFile(source, "void main() {}\n");
  await writeFile(compiler, `#!/usr/bin/env node
const fs=require("node:fs"); const path=require("node:path");
const value=(name)=>{const i=process.argv.findIndex((v)=>v===name); const eq=process.argv.find((v)=>v.startsWith(name+"=")); return eq?eq.slice(name.length+1):process.argv[i+1]};
const bin=value("-oB"), map=value("-oG"), immediate=value("-oI"); const binParts=path.parse(bin), immediateParts=path.parse(immediate); fs.mkdirSync(path.dirname(bin),{recursive:true}); fs.writeFileSync(bin,Buffer.from([0])); fs.writeFileSync(path.join(binParts.dir,binParts.name+"_RAM"+binParts.ext),Buffer.from([0])); fs.writeFileSync(map,JSON.stringify({version:1,files:[${JSON.stringify(source)}],lines:[],labels:[]})); fs.writeFileSync(immediate,"rom_label: 000010: 01"); fs.writeFileSync(path.join(immediateParts.dir,immediateParts.name+"_RAM"+immediateParts.ext),"ram_label: 000020: 01");
`);
  await writeFile(emulator, `#!/usr/bin/env node
const net=require("node:net"); const server=net.createServer((socket)=>{let data=""; socket.on("data",(chunk)=>{data+=chunk; for(;;){const i=data.indexOf("\\n"); if(i<0)break; const line=data.slice(0,i); data=data.slice(i+1); const [id,cmd]=line.split(/\\s+/); if(cmd==="hello")socket.write(id+" ok LNDBG 1\\n"); if(cmd==="quit"){socket.write(id+" ok\\n! exited 0\\n"); server.close();}}});}); server.listen(0,"127.0.0.1",()=>console.log("LNDBG-LISTEN "+server.address().port));
`);
  await Promise.all([chmod(compiler, 0o755), chmod(emulator, 0o755)]);
  const runtime = new DebugRuntime();
  context.after(() => runtime.terminate());

  // When
  await runtime.launch({ cwd: root, lncPath: compiler, javaPath: "java", emulatorPath: emulator, sourceFiles: [source], compilerOptions: ["-oD=ROM,RAM"], emulatorOptions: [], deviceImages: [] });

  // Then
  assert.equal(activeAddresses.resolve("rom_label"), 0x10);
  assert.equal(activeAddresses.resolve("ram_label"), 0x20);
});

test("launches external overrides once without exposing replaced compiler metadata", async (context) => {
  // Given
  const root = await mkdtemp(join(tmpdir(), "lncpu-debug-external-"));
  const source = join(root, "main.lnc");
  const compiler = join(root, "fake-lnc");
  const emulator = join(root, "fake-emu");
  const externalD0 = join(root, "assets", "d0.bin");
  const externalRam = join(root, "assets", "ram.bin");
  const recordedArgs = join(root, "emulator-args.json");
  await mkdir(join(root, "assets"), { recursive: true });
  await writeFile(source, "void main() {}\n");
  await writeFile(compiler, `#!/usr/bin/env node
const fs=require("node:fs"); const path=require("node:path");
const value=(name)=>{const i=process.argv.findIndex((v)=>v===name); const eq=process.argv.find((v)=>v.startsWith(name+"=")); return eq?eq.slice(name.length+1):process.argv[i+1]};
const bin=value("-oB"), map=value("-oG"); const parts=path.parse(bin); fs.mkdirSync(path.dirname(bin),{recursive:true}); fs.writeFileSync(bin,Buffer.from([1])); fs.writeFileSync(path.join(parts.dir,parts.name+"_D0"+parts.ext),Buffer.from([2])); fs.writeFileSync(map,JSON.stringify({version:1,files:[${JSON.stringify(source)}],lines:[],sections:[{name:"rom_text",target:"ROM",a:0,s:1},{name:"ram_data",target:"RAM",a:8192,s:1},{name:"d0_data",target:"D0",a:16384,s:1}],labels:[{name:"rom_symbol",a:0,sec:"rom_text"},{name:"ram_symbol",a:8192,sec:"ram_data"},{name:"d0_symbol",a:16384,sec:"d0_data"}]}));
`);
  await writeFile(emulator, `#!/usr/bin/env node
const fs=require("node:fs"); const net=require("node:net"); fs.writeFileSync(${JSON.stringify(recordedArgs)},JSON.stringify(process.argv.slice(2))); const server=net.createServer((socket)=>{let data=""; socket.on("data",(chunk)=>{data+=chunk; for(;;){const i=data.indexOf("\\n"); if(i<0)break; const line=data.slice(0,i); data=data.slice(i+1); const [id,cmd]=line.split(/\\s+/); if(cmd==="hello")socket.write(id+" ok LNDBG 1\\n"); if(cmd==="quit"){socket.write(id+" ok\\n! exited 0\\n"); server.close();}}});}); server.listen(0,"127.0.0.1",()=>console.log("LNDBG-LISTEN "+server.address().port));
`);
  await Promise.all([writeFile(externalD0, Buffer.from([3])), writeFile(externalRam, Buffer.from([4])), chmod(compiler, 0o755), chmod(emulator, 0o755)]);
  const runtime = new DebugRuntime();
  context.after(() => runtime.terminate());

  // When
  const launched = await runtime.launch({ cwd: root, lncPath: compiler, javaPath: "java", emulatorPath: emulator, sourceFiles: [source], compilerOptions: ["-oD=ROM,D0"], emulatorOptions: [], deviceImages: [{ target: "D0", path: "assets/d0.bin" }, { target: "RAM", path: "assets/ram.bin" }] });

  // Then
  assert.deepEqual(JSON.parse(await readFile(recordedArgs, "utf8")), ["--rom", launched.plan.binaryPath, "--d0", externalD0, "--ram", externalRam, "--debug-server", "--stop-on-entry", "--nopauseonhalt"]);
  assert.equal(activeAddresses.resolve("rom_symbol"), 0);
  assert.equal(activeAddresses.resolve("d0_symbol"), undefined);
  assert.equal(activeAddresses.resolve("ram_symbol"), undefined);
  assert.deepEqual(launched.ramMemoryVariables, []);
  assert.deepEqual(await readFile(join(root, ".lncpu-debug", "program_D0.bin")), Buffer.from([2]));
  await runtime.terminate();
});
