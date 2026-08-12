import re

def replace_in_file(filepath, replacements):
    with open(filepath, 'r') as f:
        content = f.read()
    for old, new in replacements:
        content = content.replace(old, new)
    with open(filepath, 'w') as f:
        f.write(content)

replace_in_file('app/src/main/java/com/example/DividendsScreen.kt', [
    ('"TATACOMM" to "tatacommunications.com"', '"T" to "att.com"'),
    ('"TATACONSUM" to "tataconsumer.com"', '"KO" to "coca-cola.com"'),
    ('"TATAELXSI" to "tataelxsi.com"', '"CRM" to "salesforce.com"'),
    ('"TATAPOWER" to "tatapower.com"', '"NEE" to "nexteraenergy.com"'),
    ('"TATASTEEL" to "walmart.com"', '"WMT" to "walmart.com"'),
    ('"TATATECH" to "tatatechnologies.com"', '"INTU" to "intuit.com"'),
    ('StockBrandInfo("TATA"', 'StockBrandInfo("Walmart"'),
    ('"TATASTEEL"', '"WMT"')
])

replace_in_file('app/src/main/java/com/example/LiveScreen.kt', [
    ('cleanSym.contains("TATASTEEL") -> listOf("Tata Steel")', 'cleanSym.contains("WMT") -> listOf("Walmart")')
])

replace_in_file('app/src/main/java/com/example/StockScanner.kt', [
    ('"TATACOMM"', '"T"'),
    ('"TATACONSUM"', '"KO"'),
    ('"TATAELXSI"', '"CRM"'),
    ('"TATAPOWER"', '"NEE"'),
    ('"TATASTEEL"', '"WMT"'),
    ('"TATATECH"', '"INTU"')
])

replace_in_file('app/src/main/java/com/example/PortfolioAnalysisView.kt', [
    ('"TATAELXSI"', '"CRM"'),
    ('"TATASTEEL"', '"WMT"'),
    ('"TATAPOWER"', '"NEE"'),
    ('RELIANCE, TATAMOTORS', 'AAPL, TSLA')
])

replace_in_file('app/src/main/java/com/example/NewsScreen.kt', [
    ('text.contains("TATA MOTORS") || text.contains("TATA")', 'text.contains("TESLA")')
])
