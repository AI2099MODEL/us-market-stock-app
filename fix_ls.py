import re

filepath = 'app/src/main/java/com/example/LiveScreen.kt'
with open(filepath, 'r') as f:
    content = f.read()

start_marker = "val STOCK_DICTIONARY = listOf("
end_marker = ")\n\ndata class LiveStock("

start_idx = content.find(start_marker)
end_idx = content.find(end_marker)

replacement = """val STOCK_DICTIONARY = listOf(
    StockInfo("AAPL", "Apple Inc."),
    StockInfo("MSFT", "Microsoft Corp."),
    StockInfo("GOOGL", "Alphabet Inc."),
    StockInfo("AMZN", "Amazon.com Inc."),
    StockInfo("TSLA", "Tesla Inc."),
    StockInfo("META", "Meta Platforms Inc."),
    StockInfo("NVDA", "NVIDIA Corp."),
    StockInfo("JPM", "JPMorgan Chase & Co."),
    StockInfo("V", "Visa Inc."),
    StockInfo("JNJ", "Johnson & Johnson"),
    StockInfo("WMT", "Walmart Inc."),
    StockInfo("PG", "Procter & Gamble Co."),
    StockInfo("MA", "Mastercard Inc."),
    StockInfo("UNH", "UnitedHealth Group Inc."),
    StockInfo("HD", "Home Depot Inc."),
    StockInfo("BAC", "Bank of America Corp."),
    StockInfo("XOM", "Exxon Mobil Corp."),
    StockInfo("DIS", "Walt Disney Co."),
    StockInfo("CVX", "Chevron Corp."),
    StockInfo("KO", "Coca-Cola Co."),
    StockInfo("PEP", "PepsiCo Inc."),
    StockInfo("MRK", "Merck & Co. Inc."),
    StockInfo("PFE", "Pfizer Inc."),
    StockInfo("CSCO", "Cisco Systems Inc."),
    StockInfo("INTC", "Intel Corp."),
    StockInfo("MCD", "McDonald's Corp."),
    StockInfo("WFC", "Wells Fargo & Co."),
    StockInfo("CRM", "Salesforce Inc."),
    StockInfo("ADBE", "Adobe Inc."),
    StockInfo("NFLX", "Netflix Inc."),
    StockInfo("NKE", "NIKE Inc."),
    StockInfo("ABT", "Abbott Laboratories"),
    StockInfo("T", "AT&T Inc."),
    StockInfo("VZ", "Verizon Communications Inc."),
    StockInfo("CMCSA", "Comcast Corp."),
    StockInfo("TMO", "Thermo Fisher Scientific"),
    StockInfo("AVGO", "Broadcom Inc."),
    StockInfo("COST", "Costco Wholesale Corp."),
    StockInfo("MDT", "Medtronic plc"),
    StockInfo("DHR", "Danaher Corp."),
    StockInfo("NEE", "NextEra Energy Inc."),
    StockInfo("TXN", "Texas Instruments Inc."),
    StockInfo("HON", "Honeywell International Inc."),
    StockInfo("UNP", "Union Pacific Corp."),
    StockInfo("PM", "Philip Morris International"),
    StockInfo("ORCL", "Oracle Corp."),
    StockInfo("QCOM", "QUALCOMM Inc."),
    StockInfo("SBUX", "Starbucks Corp."),
    StockInfo("INTU", "Intuit Inc."),
    StockInfo("IBM", "International Business Machines"),
    StockInfo("AMD", "Advanced Micro Devices Inc.")
"""

if start_idx != -1 and end_idx != -1:
    new_content = content[:start_idx] + replacement + content[end_idx:]
    with open(filepath, 'w') as f:
        f.write(new_content)
    print("Success replacing dict")
else:
    print("Failed to find dict markers")

with open(filepath, 'r') as f:
    content = f.read()

replacements = [
    ('listOf("Reliance", "RIL", "Reliance Industries")', 'listOf("Apple", "AAPL", "Apple Inc.")'),
    ('listOf("TCS", "Tata Consultancy Services")', 'listOf("Microsoft", "MSFT", "Microsoft Corp.")'),
    ('listOf("Infosys", "INFY")', 'listOf("NVIDIA", "NVDA", "Nvidia Corp.")'),
    ('listOf("ITC", "ITC Limited")', 'listOf("Procter & Gamble", "PG", "Procter & Gamble Co.")'),
    ('listOf("SBI", "State Bank of India")', 'listOf("Wells Fargo", "WFC", "Wells Fargo & Co.")'),
    ('listOf("Bajaj Finance", "BAJFINANCE")', 'listOf("Bank of America", "BAC", "Bank of America Corp.")'),
    ('listOf("L&T", "Larsen & Toubro")', 'listOf("Exxon Mobil", "XOM", "Exxon Mobil Corp.")'),
    ('listOf("Bharti Airtel", "Airtel")', 'listOf("AT&T", "T", "AT&T Inc.")'),
    ('NSE or BSE', 'NYSE or NASDAQ'),
    ('RELIANCE, TATAMOTORS', 'AAPL, TSLA'),
    ('listOf("Infosys", "Infy")', 'listOf("Nvidia", "NVDA")'),
    ('cleanSym.contains("TATASTEEL") -> listOf("Tata Steel")', 'cleanSym.contains("WMT") -> listOf("Walmart")'),
    ('listOf("ICICI Bank")', 'listOf("Bank of America", "BofA")')
]

for old, new in replacements:
    content = content.replace(old, new)

with open(filepath, 'w') as f:
    f.write(content)

print("Success replacing other references")
