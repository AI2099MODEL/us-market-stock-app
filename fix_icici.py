import re

def replace_in_file(filepath, replacements):
    with open(filepath, 'r') as f:
        content = f.read()
    for old, new in replacements:
        content = content.replace(old, new)
    with open(filepath, 'w') as f:
        f.write(content)

replace_in_file('app/src/main/java/com/example/DividendsScreen.kt', [
    ('"ICICI" to "icicibank.com"', '"C" to "citigroup.com"'),
    ('"ICICIGI" to "icicilombard.com"', '"TRV" to "travelers.com"'),
    ('StockBrandInfo("ICICI"', 'StockBrandInfo("Bank of America"'),
])

replace_in_file('app/src/main/java/com/example/LiveScreen.kt', [
    ('listOf("ICICI Bank")', 'listOf("Bank of America", "BofA")')
])

replace_in_file('app/src/main/java/com/example/StockScanner.kt', [
    ('"ICICIGI"', '"TRV"')
])

replace_in_file('app/src/main/java/com/example/NewsScreen.kt', [
    ('name = "ICICI Direct"', 'name = "Interactive Brokers"')
])
