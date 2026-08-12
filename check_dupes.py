import re

with open('app/src/main/java/com/example/StockScanner.kt', 'r') as f:
    content = f.read()

m = re.search(r'val SP500_TICKERS = listOf\((.*?)\)', content, re.DOTALL)
if m:
    items = m.group(1).replace('\n', '').replace(' ', '').split(',')
    items = [x.strip('"') for x in items if x]
    seen = set()
    for item in items:
        if item in seen:
            print(f"Duplicate in SP500_TICKERS: {item}")
        seen.add(item)
