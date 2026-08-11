import pathlib
import socket
import subprocess
import sys
import tempfile


class Lines:
    def __init__(self, connection):
        self.connection = connection
        self.buffer = b""

    def next(self):
        while b"\n" not in self.buffer:
            chunk = self.connection.recv(4096)
            if not chunk:
                raise AssertionError("debug server disconnected before a complete frame")
            self.buffer += chunk
        line, self.buffer = self.buffer.split(b"\n", 1)
        return line.decode("ascii").rstrip("\r")


def expect(lines, expected):
    actual = lines.next()
    if actual != expected:
        raise AssertionError(f"expected {expected!r}, got {actual!r}")


def main():
    executable = pathlib.Path(sys.argv[1]).resolve()
    with tempfile.TemporaryDirectory() as directory:
        rom = pathlib.Path(directory) / "debug.rom"
        rom.write_bytes(bytes([0x04, 0x2A, 0x00, 0x01]))
        process = subprocess.Popen(
            [str(executable), "--debug-server", "--stop-on-entry", "--rom", str(rom), "--nopauseonhalt"],
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
        )
        try:
            listen_line = process.stdout.readline().strip()
            prefix = "LNDBG-LISTEN "
            if not listen_line.startswith(prefix):
                raise AssertionError(f"missing listen line: {listen_line!r}")
            port = int(listen_line[len(prefix):])
            if port <= 0 or port > 65535:
                raise AssertionError(f"invalid ephemeral port: {port}")

            with socket.create_connection(("127.0.0.1", port), timeout=3) as connection:
                connection.settimeout(3)
                lines = Lines(connection)
                expect(lines, "! stopped entry 0000")

                connection.sendall(b"1 hel")
                connection.sendall(b"lo\n2 regs\n")
                expect(lines, "1 ok LNDBG 1")
                registers = lines.next()
                if not registers.startswith("2 ok RA=00 RB=00 RC=00 RD=00 DS=00 SS=00 SP=00 BP=00 FLAGS=00 CSPC=0000 INSTRUCTIONS=0 CYCLES=0"):
                    raise AssertionError(f"unexpected registers: {registers!r}")

                connection.sendall(b"3 readmem 0000 4\n4 writemem 0100 deadbeef\n5 readmem 0100 4\n")
                expect(lines, "3 ok 042a0001")
                expect(lines, "4 ok")
                expect(lines, "5 ok deadbeef")

                connection.sendall(b"6 setreg RA 11\n7 setreg INSTRUCTIONS 1\n8 stepout\n")
                expect(lines, "6 ok")
                expect(lines, "7 err invalid or immutable register")
                expect(lines, "8 err no representable return frame")

                connection.sendall(b"9 writemem 0000 f000\n10 setreg CSPC 0000\n11 continue\n12 pause\n")
                expect(lines, "9 ok")
                expect(lines, "10 ok")
                expect(lines, "11 ok")
                expect(lines, "12 ok")
                expect(lines, "! stopped pause 0000")

                connection.sendall(b"13 writemem 0000 042a0001\n14 setreg CSPC 0000\n15 bp set 0002\n16 continue\n")
                expect(lines, "13 ok")
                expect(lines, "14 ok")
                expect(lines, "15 ok")
                expect(lines, "16 ok")
                expect(lines, "! stopped breakpoint 0002")

                connection.sendall(b"17 regs\n18 step\n")
                registers = lines.next()
                if "RA=2a" not in registers or "CSPC=0002" not in registers or "INSTRUCTIONS=1" not in registers:
                    raise AssertionError(f"breakpoint executed too late: {registers!r}")
                expect(lines, "18 ok")
                expect(lines, "! stopped step 0003")

                connection.sendall(b"19 bp clear 0002\n20 bp clearall\n21 continue\n")
                expect(lines, "19 ok")
                expect(lines, "20 ok")
                expect(lines, "21 ok")
                expect(lines, "! stopped halt 0004")
                expect(lines, "! exited 0")

            return_code = process.wait(timeout=3)
            if return_code != 0:
                raise AssertionError(f"emulator exited with {return_code}: {process.stderr.read()}")
        finally:
            if process.poll() is None:
                process.kill()
                process.wait()

        stepout_rom = pathlib.Path(directory) / "stepout.rom"
        stepout_rom.write_bytes(bytes([
            0xF6, 0x00, 0x06,
            0x01,
            0x00, 0x00,
            0x00,
            0xF6, 0x00, 0x0C,
            0xF8,
            0x00,
            0x00,
            0xF8,
        ]))
        process = subprocess.Popen(
            [str(executable), "--debug-server", "--stop-on-entry", "--rom", str(stepout_rom), "--nopauseonhalt"],
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
        )
        try:
            listen_line = process.stdout.readline().strip()
            prefix = "LNDBG-LISTEN "
            if not listen_line.startswith(prefix):
                raise AssertionError(f"missing stepout listen line: {listen_line!r}")
            port = int(listen_line[len(prefix):])
            with socket.create_connection(("127.0.0.1", port), timeout=3) as connection:
                connection.settimeout(3)
                lines = Lines(connection)
                expect(lines, "! stopped entry 0000")
                connection.sendall(b"22 hello\n23 bp set 0006\n24 continue\n")
                expect(lines, "22 ok LNDBG 1")
                expect(lines, "23 ok")
                expect(lines, "24 ok")
                expect(lines, "! stopped breakpoint 0006")

                connection.sendall(b"25 bp clearall\n26 stepout\n")
                expect(lines, "25 ok")
                expect(lines, "26 ok")
                expect(lines, "! stopped step 0003")

                connection.sendall(b"27 regs\n28 quit\n")
                registers = lines.next()
                if "CSPC=0003" not in registers or "SP=00" not in registers or "INSTRUCTIONS=6" not in registers:
                    raise AssertionError(f"stepout stopped at the wrong frame: {registers!r}")
                expect(lines, "28 ok")
                expect(lines, "! exited 0")

            return_code = process.wait(timeout=3)
            if return_code != 0:
                raise AssertionError(f"stepout emulator exited with {return_code}: {process.stderr.read()}")
        finally:
            if process.poll() is None:
                process.kill()
                process.wait()


if __name__ == "__main__":
    main()
