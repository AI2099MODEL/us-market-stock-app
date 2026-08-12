import re

def replace_in_file(filepath, replacements):
    with open(filepath, 'r') as f:
        content = f.read()
    for old, new in replacements:
        content = content.replace(old, new)
    with open(filepath, 'w') as f:
        f.write(content)

# Fix PortfolioAnalysisView.kt sector mapping
portfolio_sectors = """
// Sector Mapping for US Tickers
fun getStockSector(ticker: String): String {
    val clean = ticker.uppercase()
    return when {
        clean in listOf("XOM", "CVX", "COP", "SLB", "OXY", "BP", "TTE") -> "Energy & Oil"
        clean in listOf("JPM", "BAC", "WFC", "C", "GS", "MS", "BLK", "AXP", "USB", "PNC", "SCHW", "COIN") -> "Banking & Financials"
        clean in listOf("TSLA", "GM", "F", "TM", "RIVN", "LCID") -> "Automotive"
        clean in listOf("MSFT", "AAPL", "NVDA", "IBM", "ORCL", "CRM", "ADBE", "INTC", "AMD", "QCOM", "AVGO", "TXN") -> "Information Technology"
        clean in listOf("PG", "KO", "PEP", "WMT", "COST", "TGT", "PM", "MO", "CL", "K") -> "Consumer Goods & Retail"
        clean in listOf("UNH", "JNJ", "PFE", "MRK", "ABBV", "LLY", "TMO", "DHR", "MDT", "BMY") -> "Healthcare & Pharma"
        clean in listOf("BA", "LMT", "RTX", "NOC", "GD", "CAT", "HON", "GE", "MMM") -> "Aerospace & Industrials"
        clean in listOf("FCX", "NEM", "VALE", "AA", "RIO", "BHP") -> "Metals & Mining"
        clean in listOf("NEE", "DUK", "SO", "D", "EXC", "AEP", "PCG") -> "Utilities & Power"
        clean in listOf("AMZN", "GOOGL", "META", "NFLX", "DIS", "T", "VZ", "CMCSA") -> "Communication & Media"
        else -> "Diversified / Other"
    }
}
"""

with open('app/src/main/java/com/example/PortfolioAnalysisView.kt', 'r') as f:
    cv_content = f.read()

# Replace getStockSector block
start_sec = cv_content.find("// Sector Mapping for US Tickers")
end_sec = cv_content.find("// Storage helpers using SharedPreferences")
if start_sec != -1 and end_sec != -1:
    cv_content = cv_content[:start_sec] + portfolio_sectors + "\n" + cv_content[end_sec:]
    with open('app/src/main/java/com/example/PortfolioAnalysisView.kt', 'w') as f:
        f.write(cv_content)

# Clean up StockScanner.kt default symbols list
stock_scanner_replacement = """val DEFAULT_SCANNER_SYMBOLS = listOf(
    "AAPL", "MSFT", "GOOGL", "AMZN", "TSLA", "META", "NVDA", "JPM", "V", "JNJ",
    "WMT", "PG", "MA", "UNH", "HD", "BAC", "XOM", "DIS", "CVX", "KO",
    "PEP", "MRK", "PFE", "CSCO", "INTC", "MCD", "WFC", "CRM", "ADBE", "NFLX",
    "NKE", "ABT", "T", "VZ", "CMCSA", "TMO", "AVGO", "COST", "MDT", "DHR",
    "NEE", "TXN", "HON", "UNP", "PM", "ORCL", "QCOM", "SBUX", "INTU", "IBM",
    "AMD", "BA", "LMT", "CAT", "GS", "MS", "BLK", "AXP", "PYPL", "UBER"
)"""

with open('app/src/main/java/com/example/StockScanner.kt', 'r') as f:
    ss_content = f.read()

# Replace default scanner symbols
ss_start = ss_content.find("val DEFAULT_SCANNER_SYMBOLS")
ss_end = ss_content.find("\n\nclass StockScanner")
if ss_start != -1 and ss_end != -1:
    ss_content = ss_content[:ss_start] + stock_scanner_replacement + "\n\n" + ss_content[ss_end+2:]
    with open('app/src/main/java/com/example/StockScanner.kt', 'w') as f:
        f.write(ss_content)

print("Cleaned PortfolioAnalysisView and StockScanner successfully.")
