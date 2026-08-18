#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"

if [ ! -d "build" ]; then
    mkdir build
fi

echo "Building ln-bios..."
cd "$SCRIPT_DIR/../ln-bios"
./make.sh || {
    echo "Build failed!"
    exit 1
}

echo "Building test-os..."
cd "$SCRIPT_DIR/../test-os"

SOURCES=()
while IFS= read -r -d '' f; do
    SOURCES+=("$f")
done < <(find src -type f \( -name '*.lnc' -o -name '*.lnasm' \) -print0)

echo "Source files: ${SOURCES[*]}"

lnc "${SOURCES[@]}" \
    -oD="ROM" \
    -oI="build/test-os.out.imm.txt" \
    -oM="build/test-os.out.ir.txt" \
    -oA="build/__lncout.lnasm" \
    -oB="build/test-os.out" \
    -S="../ln-bios/build/bios.sym" \
    -I="../ln-bios/include" || {
        echo "Build failed!"
        exit 1
    }

if [[ "${1:-}" == "--run" ]]; then
    echo "Running project..."
    lncpu_emu --rom="../ln-bios/build/bios.out" -t D1 --d0=build/test-os.out ${2:-}
fi