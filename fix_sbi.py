import re

def replace_in_file(filepath, replacements):
    with open(filepath, 'r') as f:
        content = f.read()
    for old, new in replacements:
        content = content.replace(old, new)
    with open(filepath, 'w') as f:
        f.write(content)

replace_in_file('app/src/main/java/com/example/DividendsScreen.kt', [
    ('"SBI" to "sbi.co.in"', '"WFC" to "wellsfargo.com"'),
    ('"SBICARD" to "sbicard.com"', '"AXP" to "americanexpress.com"'),
    ('"SBILIFE" to "sbilife.co.in"', '"MET" to "metlife.com"'),
    ('StockBrandInfo("SBI"', 'StockBrandInfo("Wells Fargo"'),
])

replace_in_file('app/src/main/java/com/example/NewsTickerService.kt', [
    ('HDFC / ICICI / SBI', 'JPM / BAC / WFC'),
    ('hdfc") || lower.contains("icici") || lower.contains("sbi")', 'jpm") || lower.contains("bac") || lower.contains("wfc")')
])

replace_in_file('app/src/main/java/com/example/StockScanner.kt', [
    ('"SBICARD", "SBILIFE",', '"AXP", "MET",')
])
