import re

with open("app/src/main/java/com/example/StockScanner.kt", "r") as f:
    lines = f.readlines()

new_lines = []
for line in lines:
    if line.strip() == 'reasons = reasons.joinToString("':
        new_lines.append('                reasons = reasons.joinToString("\\n"),\n')
    elif line.strip() == '"),':
        pass
    else:
        new_lines.append(line)

with open("app/src/main/java/com/example/StockScanner.kt", "w") as f:
    f.writelines(new_lines)

