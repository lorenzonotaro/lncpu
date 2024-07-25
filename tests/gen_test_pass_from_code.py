import os

# for each directory in ./

dirs = [d for d in os.listdir('./') if os.path.isdir(d)]

for dir in dirs:
    # open dir/test.lnasm
    with open(f'{dir}/test.lnasm', 'r') as test_lnasm, open(f"{dir}/pass.txt", "w") as pass_txt:
        code = test_lnasm.read()
        pass_conditions = []

        for line in code.split("\n")[::-1]:
            if line.strip().startswith(";"):
                pass_conditions.append(line.strip()[1:].strip())
            elif len(line.strip()) != 0:
                break
        
        pass_txt.write("\n".join(pass_conditions))