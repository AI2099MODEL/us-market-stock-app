import re
import os

def fix_livescreen():
    filepath = 'app/src/main/java/com/example/LiveScreen.kt'
    with open(filepath, 'r') as f:
        lines = f.readlines()
    
    with open(filepath, 'w') as f:
        for line in lines:
            if 'StockInfo("JPM", "HDFC Bank")' in line:
                continue # remove
            f.write(line)

def fix_stockscanner():
    filepath = 'app/src/main/java/com/example/StockScanner.kt'
    with open(filepath, 'r') as f:
        content = f.read()
    
    content = content.replace('"JPM", "IREDA"', '"IREDA"')
    with open(filepath, 'w') as f:
        f.write(content)

def fix_watchlist():
    filepath = 'app/src/main/java/com/example/WatchlistScreen.kt'
    with open(filepath, 'r') as f:
        content = f.read()
    
    content = content.replace(', "NVDA")', ')')
    with open(filepath, 'w') as f:
        f.write(content)

fix_livescreen()
fix_stockscanner()
fix_watchlist()

