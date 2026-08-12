import re
import os

filepath = 'app/src/main/java/com/example/LiveScreen.kt'
with open(filepath, 'r') as f:
    lines = f.readlines()

keys = []
for line in lines:
    m = re.search(r'StockInfo\(\"([^\"]+)\"', line)
    if m:
        key = m.group(1)
        if key in keys:
            print(f"Duplicate key in LiveScreen: {key}")
        keys.append(key)
        
filepath = 'app/src/main/java/com/example/WatchlistScreen.kt'
with open(filepath, 'r') as f:
    content = f.read()
m = re.search(r'val quickPresets = listOf\((.*?)\)', content)
if m:
    presets = m.group(1).split(', ')
    presets = [p.replace('"', '').strip() for p in presets]
    seen = set()
    for p in presets:
        if p in seen:
            print(f"Duplicate key in Watchlist: {p}")
        seen.add(p)

