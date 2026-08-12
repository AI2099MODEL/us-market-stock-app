import re
import os

def replace_in_file(filepath, replacements):
    with open(filepath, 'r') as f:
        content = f.read()
    for old, new in replacements:
        content = content.replace(old, new)
    with open(filepath, 'w') as f:
        f.write(content)

# DividendsScreen.kt
replace_in_file('app/src/main/java/com/example/DividendsScreen.kt', [
    ('"Reliance Industries Ltd"', '"Apple Inc."'),
    ('"Tata Consultancy Services"', '"Microsoft Corp."'),
    ('"Infosys Limited"', '"NVIDIA Corp."'),
    ('"ITC Limited"', '"Procter & Gamble Co."'),
    ('"Coal India Limited"', '"Bank of America Corp."'),
    ('"Vedanta Limited"', '"Exxon Mobil Corp."'),
    ('"Power Finance Corp"', '"Walt Disney Co."'),
    ('"REC Limited"', '"Chevron Corp."'),
    ('"Bharat Petroleum Corp"', '"Coca-Cola Co."'),
    ('"Oil & Natural Gas Corp"', '"PepsiCo Inc."'),
    ('"NTPC Limited"', '"Merck & Co. Inc."'),
    ('"Power Grid Corp of India"', '"Pfizer Inc."'),
    ('"Hindustan Unilever Ltd"', '"Johnson & Johnson"'),
    ('"Tata Steel Limited"', '"Walmart Inc."'),
    ('"HCL Technologies Ltd"', '"Visa Inc."'),
    ('StockBrandInfo("RIL",', 'StockBrandInfo("Apple",'),
    ('StockBrandInfo("TCS",', 'StockBrandInfo("Microsoft",'),
    ('StockBrandInfo("INFY",', 'StockBrandInfo("NVIDIA",'),
    ('StockBrandInfo("ITC",', 'StockBrandInfo("P&G",'),
    ('StockBrandInfo("COALINDIA",', 'StockBrandInfo("BofA",'),
    ('StockBrandInfo("VEDL",', 'StockBrandInfo("Exxon",'),
    ('StockBrandInfo("PFC",', 'StockBrandInfo("Disney",'),
    ('StockBrandInfo("RECLTD",', 'StockBrandInfo("Chevron",'),
    ('StockBrandInfo("BPCL",', 'StockBrandInfo("Coca-Cola",'),
    ('StockBrandInfo("ONGC",', 'StockBrandInfo("PepsiCo",'),
    ('StockBrandInfo("NTPC",', 'StockBrandInfo("Merck",'),
    ('StockBrandInfo("POWERGRID",', 'StockBrandInfo("Pfizer",'),
    ('StockBrandInfo("HINDUNILVR",', 'StockBrandInfo("J&J",'),
    ('StockBrandInfo("TATASTEEL",', 'StockBrandInfo("Walmart",'),
    ('StockBrandInfo("HCLTECH",', 'StockBrandInfo("Visa",'),
    ('"ril.com"', '"apple.com"'),
    ('"tcs.com"', '"microsoft.com"'),
    ('"infosys.com"', '"nvidia.com"'),
    ('"itcportal.com"', '"pg.com"'),
    ('"coalindia.in"', '"bankofamerica.com"'),
    ('"vedantalimited.com"', '"exxonmobil.com"'),
    ('"pfcindia.com"', '"thewaltdisneycompany.com"'),
    ('"recindia.nic.in"', '"chevron.com"'),
    ('"bharatpetroleum.in"', '"coca-colacompany.com"'),
    ('"ongcindia.com"', '"pepsico.com"'),
    ('"ntpc.co.in"', '"merck.com"'),
    ('"powergrid.in"', '"pfizer.com"'),
    ('"hul.co.in"', '"jnj.com"'),
    ('"tatasteel.com"', '"walmart.com"'),
    ('"hcltech.com"', '"visa.com"')
])

# GeminiService.kt
replace_in_file('app/src/main/java/com/example/GeminiService.kt', [
    ('"Reliance Industries"', '"Apple Inc."'),
    ('"TCS"', '"Microsoft"'),
    ('"Infosys"', '"NVIDIA"'),
    ('"ITC"', '"Procter & Gamble"'),
    ('"Larsen & Toubro"', '"Bank of America"'),
    ('"Bajaj Finance"', '"Exxon Mobil"'),
    ('"State Bank of India"', '"Walt Disney"'),
    ('"Bharti Airtel"', '"Chevron"')
])

# LiveScreen.kt - fixing the cleanSym.contains stuff
replace_in_file('app/src/main/java/com/example/LiveScreen.kt', [
    ('listOf("Reliance", "RIL", "Reliance Industries")', 'listOf("Apple", "AAPL", "Apple Inc.")'),
    ('listOf("TCS", "Tata Consultancy Services")', 'listOf("Microsoft", "MSFT", "Microsoft Corp.")'),
    ('listOf("Infosys", "INFY")', 'listOf("NVIDIA", "NVDA", "Nvidia Corp.")'),
    ('listOf("ITC", "ITC Limited")', 'listOf("Procter & Gamble", "PG", "Procter & Gamble Co.")'),
    ('listOf("SBI", "State Bank of India")', 'listOf("Wells Fargo", "WFC", "Wells Fargo & Co.")'),
    ('listOf("Bajaj Finance", "BAJFINANCE")', 'listOf("Bank of America", "BAC", "Bank of America Corp.")'),
    ('listOf("L&T", "Larsen & Toubro")', 'listOf("Exxon Mobil", "XOM", "Exxon Mobil Corp.")'),
    ('listOf("Bharti Airtel", "Airtel")', 'listOf("AT&T", "T", "AT&T Inc.")')
])

# NewsTickerService.kt
replace_in_file('app/src/main/java/com/example/NewsTickerService.kt', [
    ('Reliance Industries announces $20,000 Crore', 'Apple Inc. announces $20 Billion'),
    ('Reliance Industries Board Approves $20,000 Crore', 'Apple Inc. Board Approves $20 Billion'),
    ('TCS wins massive $1.5 Billion AI contract', 'Microsoft wins massive $1.5 Billion AI contract'),
    ('Tata Consultancy Services Secures Historic $1.5 Billion', 'Microsoft Secures Historic $1.5 Billion'),
    ('Infosys reports better-than-expected Q3 margins', 'NVIDIA reports better-than-expected Q3 margins'),
    ('Nifty 50 touches new all-time high', 'S&P 500 touches new all-time high'),
    ('RBI Monetary Policy:', 'Fed Monetary Policy:'),
    ('RBI Keeps Repo Rate Unchanged', 'Fed Keeps Interest Rate Unchanged'),
    ('SEBI issues new guidelines', 'SEC issues new guidelines'),
    ('FII inflows hit record high', 'Foreign inflows hit record high'),
    ('LIC block deal', 'Berkshire Hathaway block deal'),
    ('Nifty Bank', 'Financial Sector')
])

# WatchlistScreen.kt
replace_in_file('app/src/main/java/com/example/WatchlistScreen.kt', [
    ('e.g. Tata Motors, Reliance, SBI, Infosys', 'e.g. Apple, Tesla, Meta, Google')
])

