#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"

echo "Building ln-bios..."
cd "$SCRIPT_DIR/../ln-bios"
./make.sh || {
    echo "Build failed!"
    exit 1
}

echo "Building test-os..."
cd "$SCRIPT_DIR/../test-os"

echo "Cleaning up..."
rm -f test-os.out.imm.txt test-os.out.ir.txt test-os.out.lnasm test-os.out

SOURCES=()
while IFS= read -r -d '' f; do
    SOURCES+=("$f")
done < <(find src -type f \( -name '*.lnc' -o -name '*.lnasm' \) -print0)

echo "Source files: ${SOURCES[*]}"

lnc "${SOURCES[@]}" \
    -oD="D0" \
    -oI="test-os.out.imm.txt" \
    -oM="test-os.out.ir.txt" \
    -oA="test-os.out.lnasm" \
    -oB="test-os.out" \
    -S="../ln-bios/bios.sym" \
    -I="../ln-bios/include" || {
        echo "Build failed!"
        exit 1
    }

if [[ "${1:-}" == "--run" ]]; then
    echo "Running project..."
    lncpu_emu --rom="../ln-bios/bios.out" -t D1 --d0=test-os_D0.out ${2:-}
fi