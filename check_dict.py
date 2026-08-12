import re

with open('app/src/main/java/com/example/LiveScreen.kt', 'r') as f:
    content = f.read()

m = re.search(r'val STOCK_DICTIONARY = listOf\((.*?)\)', content, re.DOTALL)
if m:
    lines = m.group(1).split('),')
    keys = []
    for line in lines:
        match = re.search(r'StockInfo\("([^"]+)"', line)
        if match:
            keys.append(match.group(1))
    
    seen = set()
    for key in keys:
        if key in seen:
            print(f"Duplicate in STOCK_DICTIONARY: {key}")
        seen.add(key)
