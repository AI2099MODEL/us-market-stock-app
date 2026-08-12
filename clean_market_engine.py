import re

with open("app/src/main/java/com/example/MarketEngine.kt", "r") as f:
    lines = f.readlines()

new_lines = []
skip = False
for i, line in enumerate(lines):
    if "// 3. SEGMENT A: INDEX & STOCK OPTIONS ENTRY & POST-3 PM PROFIT SELLING" in line:
        skip = True
        new_lines.append(line.replace("// 3. SEGMENT A: INDEX & STOCK OPTIONS ENTRY & POST-3 PM PROFIT SELLING", "// 3. SEGMENT A: MCX COMMODITIES ENTRY"))
        continue
    if "// 4. SEGMENT B: BTST EQUITY & MCX COMMODITIES ENTRY (Post 3:00 PM BTST Shift)" in line:
        skip = False
        continue
    
    if not skip:
        new_lines.append(line)

with open("app/src/main/java/com/example/MarketEngine.kt", "w") as f:
    f.writelines(new_lines)

