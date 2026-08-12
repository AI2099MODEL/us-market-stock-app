import re

with open("app/src/main/java/com/example/MarketEngine.kt", "r") as f:
    code = f.read()

# Remove option constants
code = re.sub(r'const val MAX_OPTIONS_SLOTS = 2\s*const val TOTAL_OPTIONS_CAPITAL = 200000\.0.*?Cap\)\s*', '', code)

# Remove option check in getLivePriceForAsset
code = re.sub(r'// 2\. Check if it\'s an Option.*?// 3\. Equity Stock Ticker', '// 2. Fallback Stock Ticker', code, flags=re.DOTALL)

# In update active trades: remove isOptionTrade and use standard trailing distances
code = re.sub(r'val isOptionTrade = trade\.name\.contains\("Option"\) \|\| trade\.ticker\.contains\("CE"\) \|\| trade\.ticker\.contains\("PE"\)\s*', '', code)
code = re.sub(r'isOptions = false', 'isOptions = false', code) # keep it as is, just need to make sure we don't break calculateDhanBrokerage

# Remove all activeOptionsCount logic and partialThreshold conditionals
code = re.sub(r'// Count active options.*?val partialThreshold = if \(isOptionTrade\) \{.*?\} else \{.*?\+6\.0% commodity return\s*\}', 'val partialThreshold = 6.0', code, flags=re.DOTALL)

# Fix partial tag log
code = re.sub(r'val optionTag = if \(isOptionTrade.*?\) ""\s*', '', code)
code = re.sub(r'\$optionTag', '', code)

# Fix breakeven
code = re.sub(r'val breakevenGainPct = if \(isOptionTrade\) 1\.2 else 1\.5', 'val breakevenGainPct = 1.5', code)

# Fix trail distances
code = re.sub(r'val minTrailingGainPct = if \(isOptionTrade\) 1\.8 else 2\.0.*?\n', 'val minTrailingGainPct = 2.0\n', code)
code = re.sub(r'val trailDistance = if \(isOptionTrade\) 0\.025 else 0\.020.*?\n', 'val trailDistance = 0.020\n', code)

# Remove special market close rule for options
code = re.sub(r'// Special Market Close Prep Rules \(Last 45 mins: book options in profit; Last 5 mins: close ALL options\).*?// --- End Option Close Logic ---', '', code, flags=re.DOTALL)

# In runEngineCycle, simplify the active allocations log
code = re.sub(r'val activeOptionTrades = currentActive\.filter \{ it\.name\.contains\("Option"\).*?val activeCommodityCapital = activeCommodityTrades\.sumOf \{ it\.allocatedAmount \}\s*if \(isVerboseLogs\.value\) \{\s*addLog\("Active Allocations: Options.*?\)\s*\}', r'''val activeCommodityCapital = currentActive.sumOf { it.allocatedAmount }
        if (isVerboseLogs.value) {
            addLog("Active Allocations: MCX Commodities (${currentActive.size}/2 slots, ₹${String.format("%,.0f", activeCommodityCapital)}/₹2,00,000 Cap)")
        }''', code, flags=re.DOTALL)

# Remove remainingOptions square off
code = re.sub(r'val remainingOptions = activeTrades\.filter \{ it\.name\.contains\("Option"\) \}.*?\}\s*return@withContext', 'return@withContext', code, flags=re.DOTALL)

with open("app/src/main/java/com/example/MarketEngine.kt", "w") as f:
    f.write(code)

print("Done")
