import { strict as assert } from "node:assert";
import { chmod, mkdtemp, readFile, writeFile } from "node:fs/promises";
import { createServer } from "node:net";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { test } from "node:test";

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
const bin=value("-oB"), map=value("-oG"); fs.mkdirSync(path.dirname(bin),{recursive:true}); fs.writeFileSync(bin,Buffer.from([0])); fs.writeFileSync(map,JSON.stringify({version:1,files:[${JSON.stringify(source)}],lines:[{a:0,s:1,f:0,l:1,c:1,sec:"text"}],labels:[{name:"main",a:0,sec:"text"}]}));
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
  });
  await runtime.terminate();

  // Then
  assert.equal(launched.map.location(0)?.label, "main");
  assert.deepEqual(await readFile(launched.plan.binaryPath), Buffer.from([0]));
});
