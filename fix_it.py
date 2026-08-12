import re

def replace_in_file(filepath, replacements):
    with open(filepath, 'r') as f:
        content = f.read()
    for old, new in replacements:
        content = content.replace(old, new)
    with open(filepath, 'w') as f:
        f.write(content)

replace_in_file('app/src/main/java/com/example/LiveScreen.kt', [
    ('listOf("Infosys", "Infy")', 'listOf("Nvidia", "NVDA")')
])

replace_in_file('app/src/main/java/com/example/NewsTickerService.kt', [
    ('TCS, Infosys & Wipro', 'Microsoft, Google & Amazon'),
    ('TCS / Infosys / Wipro', 'MSFT / GOOGL / AMZN'),
    ('TCS & Infosys', 'Microsoft & Alphabet')
])
