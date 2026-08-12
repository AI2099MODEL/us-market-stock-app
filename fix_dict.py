import re

filepath = 'app/src/main/java/com/example/LiveScreen.kt'
with open(filepath, 'r') as f:
    content = f.read()

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
)"""

content = re.sub(r'val STOCK_DICTIONARY = listOf\(.*?StockInfo\("PERSESTENT", "Persistent Systems"\)\)', replacement, content, flags=re.DOTALL)

with open(filepath, 'w') as f:
    f.write(content)
