with open("app/src/main/java/com/example/MarketEngine.kt", "r") as f:
    text = f.read()

import re

# 1. Total Options Capital
text = re.sub(r'const val MAX_OPTIONS_SLOTS = 2\n.*?const val TOTAL_OPTIONS_CAPITAL = 200000\.0.*?Cap\)\n', '', text, flags=re.MULTILINE)

# 2. Market & Options open
text = re.sub(r'\(Market & Options Open at 9:15 AM IST\)', '(MCX Market Open at 9:00 AM IST)', text)

# 3. isOptionTrade
text = re.sub(r'val isOptionTrade = trade\.name\.contains\("Option"\) \|\| trade\.ticker\.contains\("CE"\) \|\| trade\.ticker\.contains\("PE"\)\n', '', text)
text = re.sub(r'isOptions = false', 'isOptions = false', text)

# 4. activeOptionsCount and partialThreshold
replacement_4 = """
                        // Partial profit booking threshold (requires solid gain before booking 50% to maximize profit run)
                        val partialThreshold = 6.0 // +6.0% MCX commodity return"""
text = re.sub(r'// Count active options.*?5\.0 // \+5\.0% equity gain\s*\}', replacement_4, text, flags=re.DOTALL)

# 5. optionTag logging
text = re.sub(r'val optionTag = if \(isOptionTrade.*?\) ""\n(.*?)on \$\{trade\.ticker\}\$optionTag', r'\1on ${trade.ticker}', text)
text = re.sub(r'val optionTag =.*?\n', '', text)

# 6. breakeven
text = re.sub(r'val breakevenGainPct = if \(isOptionTrade\) 1\.2 else 1\.5', 'val breakevenGainPct = 1.5', text)

# 7. trailing profit 
text = re.sub(r'val minTrailingGainPct = if \(isOptionTrade\) 1\.8 else 2\.0 // Activates after \+1\.8% / \+2\.0% gain', 'val minTrailingGainPct = 2.0 // Activates after +2.0% gain', text)
text = re.sub(r'val trailDistance = if \(isOptionTrade\) 0\.025 else 0\.020 // 2\.5% for options, 2\.0% for equity', 'val trailDistance = 0.020 // 2.0% for commodity', text)

# 8. Special Market Close Rules for options
text = re.sub(r'// Special Market Close Prep Rules.*?return@async\s*\}\s*\}', '', text, flags=re.DOTALL)

# 9. Active Options Trades stats 
replacement_9 = """        val activeCommodityCapital = currentActive.sumOf { it.allocatedAmount }

        val canEnter = !isDailyRiskCapHit && (timeInMinutes < 1410 || isSimulationMode.value)

        if (canEnter) {
            addLog("Active Allocations: MCX Commodities (${currentActive.size}/2 slots, ₹${String.format("%,.0f", activeCommodityCapital)}/₹2,00,000 Cap)")"""

text = re.sub(r'val activeOptionTrades = currentActive\.filter \{ it\.name.*?addLog\("Active Allocations: Options.*?\)', replacement_9, text, flags=re.DOTALL)

# 10. remainingOptions square off
text = re.sub(r'val remainingOptions = activeTrades\.filter \{ it\.name.*?return@withContext', 'return@withContext', text, flags=re.DOTALL)

# 11. Fetch Index Options comment
text = re.sub(r'// 2\. Fetch Index Options, Stock Options & Commodity Breakout candidates \(score >= 75\)', '// 2. Fetch MCX Commodity Breakout candidates (score >= 75)', text)

# 12. Save breakout candidates comment
text = re.sub(r'// Save breakout candidates \(Indices, Stocks Options & Commodities\) to database', '// Save breakout candidates to database', text)

with open("app/src/main/java/com/example/MarketEngine.kt", "w") as f:
    f.write(text)

