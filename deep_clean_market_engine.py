import re

with open("app/src/main/java/com/example/MarketEngine.kt", "r") as f:
    code = f.read()

# Remove option caps
code = re.sub(r'const val MAX_OPTIONS_SLOTS = 2\s*const val TOTAL_OPTIONS_CAPITAL = 200000\.0 \/\/ .*?\n', '', code)
# Replace option checks with false
code = re.sub(r'val isOptionTrade = trade\.name\.contains\("Option"\).*?\n', 'val isOptionTrade = false\n', code)
code = re.sub(r'isOptions = isOptionTrade', 'isOptions = false', code)
code = re.sub(r'if \(isPut\) -underlyingChangePct \* 8\.0 else underlyingChangePct \* 8\.0', 'underlyingChangePct', code)

# Remove the whole // 2. Check if it's an Option Trade block
code = re.sub(r'// 2\. Check if it\'s an Option Trade.*?return@withContext maxOf\(1\.0, computedOptionPrice\)', '', code, flags=re.DOTALL)

# Remove overnight decay close
code = re.sub(r'// Market is closed\. Auto square off any remaining active options to prevent overnight decay.*?// Log simulation mode if enabled', '// Log simulation mode if enabled', code, flags=re.DOTALL)

# Remove active option counts
code = re.sub(r'val activeOptionsCount = refreshedActiveTrades\.count \{.*?\}\n', '', code)
code = re.sub(r'val partialThreshold = if \(isOptionTrade\) \{.*?\} else \{.*?(if \(isShort\).*?)\}', r'val partialThreshold = \1', code, flags=re.DOTALL)
code = re.sub(r'val optionTag = if \(isOptionTrade.*?\) ".*?" else ""\n', '', code)
code = re.sub(r'\$optionTag', '', code)

# Clean breakevenGainPct
code = re.sub(r'val breakevenGainPct = if \(isOptionTrade\) 1\.2 else 1\.5\n', 'val breakevenGainPct = 1.5\n', code)
code = re.sub(r'val minTrailingGainPct = if \(isOptionTrade\) 1\.8 else 2\.0.*?\n', 'val minTrailingGainPct = 2.0\n', code)
code = re.sub(r'val trailDistance = if \(isOptionTrade\) 0\.025 else 0\.020.*?\n', 'val trailDistance = 0.020\n', code)

# Remove special market close prep rules
code = re.sub(r'// Special Market Close Prep Rules.*?// Ensure stop-loss only tightens in favor of profit', '// Ensure stop-loss only tightens in favor of profit', code, flags=re.DOTALL)

# Remove activeOptionTrades variables
code = re.sub(r'val activeOptionTrades = currentActive\.filter \{.*?\}\n\s*val activeOptionCapital = activeOptionTrades\.sumOf \{ it\.allocatedAmount \}\n', '', code)
code = re.sub(r'val activeCommodityTrades = currentActive\.filter \{ !it\.name\.contains\("Option"\) && !it\.ticker\.contains\("CE"\) && !it\.ticker\.contains\("PE"\) \}\n', 'val activeCommodityTrades = currentActive\n', code)
code = re.sub(r'Options \(\$\{activeOptionTrades\.size\}/2 slots, ₹\$\{String\.format\("%,\.0f", activeOptionCapital\)\}/₹2,00,000 Cap\) \| MCX ', 'MCX ', code)

with open("app/src/main/java/com/example/MarketEngine.kt", "w") as f:
    f.write(code)

