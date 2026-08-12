import re
import os

def fix_stockscanner():
    filepath = 'app/src/main/java/com/example/StockScanner.kt'
    with open(filepath, 'r') as f:
        content = f.read()
    
    content = content.replace('"HDFCAMC", "JPM",', '"HDFCAMC",')
    with open(filepath, 'w') as f:
        f.write(content)

fix_stockscanner()

