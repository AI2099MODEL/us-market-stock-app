import re

with open("app/src/main/java/com/example/AutoTraderTab.kt", "r") as f:
    code = f.read()

old_pnl_logic = """    val profitPct = if (trade.entryPrice > 0.0) {
        if (isShort) ((trade.entryPrice - currentCmp) / trade.entryPrice) * 100.0 else ((currentCmp - trade.entryPrice) / trade.entryPrice) * 100.0
    } else trade.profitPercent
    val grossProfit = trade.allocatedAmount * (profitPct / 100.0)
    val netProfit = grossProfit - 30.0"""

new_pnl_logic = """    val profitPct = if (trade.entryPrice > 0.0) {
        if (isShort) ((trade.entryPrice - currentCmp) / trade.entryPrice) * 100.0 else ((currentCmp - trade.entryPrice) / trade.entryPrice) * 100.0
    } else trade.profitPercent
    
    val grossProfit = trade.allocatedAmount * (profitPct / 100.0)
    val turnover = trade.allocatedAmount * 2.0
    val brokerageDetails = IndianCommodityRepository.calculateDhanBrokerage(turnover, isSell = true, isOptions = false)
    val netProfit = grossProfit - brokerageDetails.totalCharges"""

code = code.replace(old_pnl_logic, new_pnl_logic)

with open("app/src/main/java/com/example/AutoTraderTab.kt", "w") as f:
    f.write(code)

