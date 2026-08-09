# lnfsutils

---

`lnfsutils` is a simple utility to work with `lnfs` filesystems (see below for details)

## 1. What is `lnfs`?

`lnfs` is a very, _very_ basic filesystem designed to work reasonably well on the LNCPU.
It is designed to be simple, easy to implement and with minimal storage overhead. Its blocks are variable in size. V1 is designed to be used within a single top-level device.

### 1.1 Filesystem layout
It is structured as follows:

1. The first 12 bytes of the partition are reserved for the superblock, which is structured as follows:

    | Offset | Description                                         |
    |--------|-----------------------------------------------------|
    | `0-3`  | Magic word "LNFS"                                   |
    | `4`    | Version number (currently 1)                        |
    | `5`    | filesystem total size in pages (256 bytes per page) |
    | `6`    | reserved, must be 0                                 |
    | `7`    | max inodes number                                   |
    | `8`    | reserved, must be 0                                 |
    | `9`    | first free inode slot                               |
    | `10`   | flags (see note)                                    |
    | `11`   | magic byte, A5                                      |

    > 
    >     
    > Superblock `flags` is a bitfield currently composed of a single bit:
    >
    >   | Bit | Description             |
    >   |-----|-------------------------|
    >   | 0   | filesystem is read-only |
    >   | 1-7 | reserved/unused         |

2. The superblock is followed by the inode table (or directory table). That is a contiguous array of inodes up to the maximum specified in the superblock. Each inode is structured as follows:

    | Offset  | Description                                                   |
    |---------|---------------------------------------------------------------|
    | `0-7`   | inode name (null-terminated string)                           |
    | `8`     | parent inode index (0 if the inode is root)                   |
    | `9`     | inode flags                                                   |
    | `10-11` | inode block offset from the start of the filesystem, in bytes |
    > Inode `flags` is a bitfield currently composed of:
    > 
    > | Bit | Description          |
    > |-----|----------------------|
    > | 0   | inode slot is free   |
    > | 1   | inode is a directory |
    > | 2   | inode is readonly    |
    > | 3   | inode is hidden      |
    > | 4   | inode is root        |
    > | 5-7 | reserved/unused      |
3. The inode table is followed by a doubly linked list of data blocks. Each data block is variable in size, and is structured as follows:

    | Offset    | Description                                                       |
    |-----------|-------------------------------------------------------------------|
    | `0-1`     | pointer to start of previous data block                           |
    | `2-3`     | pointer to start of next data block                               |
    | `4-5`     | pointer to inode that owns this block (or 0 if the block is free) |
    | `6-7`     | size of the data block in bytes, excluding the header (`S`)       |
    | `8-(S+7)` | data (S bytes)                                                    |


> [!NOTE]
> All pointers are offsets from the start of the filesystem, in bytes.

### Filesystem Upkeep

* The `first free inode` field in the superblock (bytes 9-10) must be updated whenever the inode table is modified. It must always point to the first free inode slot in the inode table. If there are no free slots, it must be set to `0xFFFF`.
* Whenever an inode is freed (its `free` flag is set), the data block owned by that inode must also be freed.
* The block doubly linked list must be kept valid at all times.
* Defragmentation of both the inode table and the data block list is not required, but it is recommended to keep the filesystem in a good state.