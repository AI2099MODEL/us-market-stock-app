import re

filepath = 'app/src/main/java/com/example/GeminiService.kt'
with open(filepath, 'r') as f:
    content = f.read()

content = content.replace('"TCS / Tech Leader"', '"Microsoft Corp."')
content = content.replace('"Tata Motors"', '"Tesla Inc."')
content = content.replace('"Apple Inc."', '"Apple Inc."')

with open(filepath, 'w') as f:
    f.write(content)
