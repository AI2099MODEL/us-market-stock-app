import re

filepath = 'app/src/main/java/com/example/DividendsScreen.kt'
with open(filepath, 'r') as f:
    content = f.read()
    
content = content.replace('"hdfcbank.com"', '"jpmorganchase.com"')
content = content.replace('StockBrandInfo("HDFC", Color(0xFF004C8F))', 'StockBrandInfo("JPMorgan", Color(0xFF004C8F))')
content = content.replace('"HDFC Bank Limited"', '"JPMorgan Chase & Co."')
with open(filepath, 'w') as f:
    f.write(content)

filepath = 'app/src/main/java/com/example/GeminiService.kt'
with open(filepath, 'r') as f:
    content = f.read()
content = content.replace('"HDFC Bank"', '"JPMorgan Chase"')
with open(filepath, 'w') as f:
    f.write(content)

filepath = 'app/src/main/java/com/example/LiveScreen.kt'
with open(filepath, 'r') as f:
    content = f.read()
content = content.replace('listOf("HDFC Bank")', 'listOf("JPMorgan Chase")')
with open(filepath, 'w') as f:
    f.write(content)
