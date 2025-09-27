# LN-BIOS

LN-BIOS is a simple BIOS for the LNCPU designed to be OS-agnostic.

It is very lightweight:
1. It sets up the LNCPU
2. It runs device discovery and sets up a basic text IO system (`stdtxt`), that it will expose to the loaded OS.
3. It runs through the devices and looks for a bootable device according to the DDI specification (see below), if found, it transfer control depending on the compile options.

## Building

1. Edit `opts.lnasm` to set the desired options.
2. Run the following command (lnc must be installed in in the system path):

    ```lnc .startup.lnasm -oB ln-bios.bin -oS symbols.syt```

    This will generate a ln-bios.bin file that can be flashed to the LNCPU ROM, and a symbols.syt that contains all exposed symbols that can be used to compile an OS.

## Device discovery interface (DDI)

LN-BIOS will look for devices that implement the DDI specification. The DDI specification is a simple way for devices to expose their functionality to the BIOS and the OS.

LN-BIOS will start by looking at the last 16 bytes of each device's memory space for a DDI signature. If it finds the signature, it will read the DDI header and add the device to the list of discovered devices.

### DDI Header Format
| Bytes  | Description |
|--------|-------------|
| `0xXff0` | `LNDI` (hex: 4C 4E 44 49) - DDI signature |
| `0xXff4` | Version (1 byte) - DDI version (currently 0x1)|
| `0xXff5` | Device Type (1 byte) - Type of device (see below) |
| `0xXff6` | Flags (1 byte) - Device flags (see below) |
| `0xXff7` | Device size in pages (1 byte) - Size of the device in 256-byte pages |
| `0xXff8` | Boot target (2 bytes), or 0x0000 if none |
| `0xXffa` | Pointer to control page, or 0x0000 if none |
| `0xXffc` | Reserved (3 bytes) - Must be 0x0000 |
| `0xXfff` | Control byte (1 byte) : `0xA5` |

### Device Types
Currently supported devices:
| Value | Description |
|-------|-------------|
| **Memory devices**  |
| `0x00`| ROM, 8192 bytes (32 pages) |
| `0x01`| RAM, 8192 bytes (32 pages) |
| **I/O devices** |
| `0x08`| Emulated serial TTY |
| **Address space managers (ASM)** |
| `0x10`| 31x256-byte ASM |
| `0x11`| 15x512-byte ASM |
| `0x12`| 7x1024-byte ASM |

When encountering an ASM device, LN-BIOS will recursively scan the address spaces it manages for more devices (**not implemented yet**).