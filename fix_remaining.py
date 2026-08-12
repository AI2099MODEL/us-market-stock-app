import re

def replace_in_file(filepath, replacements):
    with open(filepath, 'r') as f:
        content = f.read()
    for old, new in replacements:
        content = content.replace(old, new)
    with open(filepath, 'w') as f:
        f.write(content)

replace_in_file('app/src/main/java/com/example/DividendsScreen.kt', [
    ('"HDFC" to "jpmorganchase.com"', '"JPM" to "jpmorganchase.com"'),
    ('"HDFCAMC" to "hdfcfund.com"', '"GS" to "goldmansachs.com"'),
    ('"HDFCLIFE" to "hdfclife.com"', '"PRU" to "prudential.com"'),
    ('"WIPRO" to "wipro.com"', '"IBM" to "ibm.com"'),
    ('"HDFC" to StockBrandInfo("JPMorgan"', '"JPM" to StockBrandInfo("JPMorgan"'),
    ('"SBI" to StockBrandInfo("Wells Fargo"', '"WFC" to StockBrandInfo("Wells Fargo"'),
    ('"WIPRO" to StockBrandInfo("WIPRO"', '"IBM" to StockBrandInfo("IBM"'),
    ('"TATACONSUM" to StockBrandInfo("Walmart"', '"KO" to StockBrandInfo("Coca-Cola"')
])

replace_in_file('app/src/main/java/com/example/StockScanner.kt', [
    ('"HDFCAMC"', '"GS"'),
    ('"HDFCLIFE"', '"PRU"'),
    ('"WIPRO"', '"IBM"')
])

replace_in_file('app/src/main/java/com/example/PortfolioAnalysisView.kt', [
    ('"INFOSYS"', '"NVDA"'),
    ('"WIPRO"', '"IBM"')
])

replace_in_file('app/src/main/java/com/example/LiveScreen.kt', [
    ('cleanSym.contains("WIPRO")', 'cleanSym.contains("IBM")')
])

