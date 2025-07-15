import os
import csv

OPCODES_FILE = "./v1/controlunit/opcodes.tsv"

TESTS_DIR = "./tests"

# find and print all opcodes[1:][1] for which no subfolder with that name exists in TESTS_DIR
def find_missing_tests():
    with open(OPCODES_FILE, newline='') as csvfile:
        reader = csv.reader(csvfile, delimiter='\t')
        opcodes = [row[1] for row in reader if row]  # Read the second column of each row

    missing_tests = []
    for opcode in opcodes[1:]:  # Skip the header
        test_path = os.path.join(TESTS_DIR, opcode)
        if not os.path.exists(test_path):
            missing_tests.append(opcode)

    return missing_tests


if __name__ == "__main__":
    missing_tests = find_missing_tests()
    if missing_tests:
        print("Missing tests for opcodes:")
        for opcode in missing_tests:
            print(opcode)
    else:
        print("All opcodes have corresponding test folders.")