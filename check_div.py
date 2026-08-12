import re

with open('app/src/main/java/com/example/DividendsScreen.kt', 'r') as f:
    content = f.read()

m = re.search(r'val UPCOMING_DIVIDENDS_MOCK = listOf\((.*?)\)', content, re.DOTALL)
if m:
    lines = m.group(1).split('),')
    keys = []
    for line in lines:
        match = re.search(r'UpcomingDividend\("([^"]+)"', line)
        if match:
            keys.append(match.group(1))
    
    seen = set()
    for key in keys:
        if key in seen:
            print(f"Duplicate in UPCOMING_DIVIDENDS_MOCK: {key}")
        seen.add(key)
