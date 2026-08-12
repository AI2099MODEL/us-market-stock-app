import re

with open("app/src/main/java/com/example/MarketEngine.kt", "r") as f:
    lines = f.readlines()

out_lines = []
for i, line in enumerate(lines):
    if "val newHighest = max(max(trade.highestPrice, trade.entryPrice), currentPrice)" in line:
        out_lines.append(line.replace("max(max(trade.highestPrice, trade.entryPrice), currentPrice)", "if (trade.targetPrice < trade.entryPrice) minOf(minOf(trade.highestPrice, trade.entryPrice), currentPrice) else maxOf(maxOf(trade.highestPrice, trade.entryPrice), currentPrice)"))
    elif "val peakUnderlyingChangePct =" in line and "(newHighest - trade.entryPrice)" in line:
        out_lines.append(line.replace("((newHighest - trade.entryPrice) / trade.entryPrice) * 100.0", "if (isShort) ((trade.entryPrice - newHighest) / trade.entryPrice) * 100.0 else ((newHighest - trade.entryPrice) / trade.entryPrice) * 100.0"))
    elif "val safeSL = trade.entryPrice * (1.0 + cushionPct)" in line:
        out_lines.append(line.replace("1.0 + cushionPct", "if (isShort) (1.0 - cushionPct) else (1.0 + cushionPct)"))
    elif "stopLoss = max(trade.stopLoss, safeSL)" in line:
        out_lines.append(line.replace("max(trade.stopLoss, safeSL)", "if (isShort) minOf(trade.stopLoss, safeSL) else maxOf(trade.stopLoss, safeSL)"))
    elif "val breakevenSL = trade.entryPrice * (1.0 + feeCushionPct)" in line:
        out_lines.append(line.replace("1.0 + feeCushionPct", "if (isShort) (1.0 - feeCushionPct) else (1.0 + feeCushionPct)"))
    elif "activeStopLoss = max(activeStopLoss, breakevenSL)" in line:
        out_lines.append(line.replace("max(activeStopLoss, breakevenSL)", "if (isShort) minOf(activeStopLoss, breakevenSL) else maxOf(activeStopLoss, breakevenSL)"))
    elif "val dynamicTrailingSL = newHighest * (1.0 - trailDistance)" in line:
        out_lines.append(line.replace("1.0 - trailDistance", "if (isShort) (1.0 + trailDistance) else (1.0 - trailDistance)"))
    elif "activeStopLoss = max(activeStopLoss, dynamicTrailingSL)" in line:
        out_lines.append(line.replace("max(activeStopLoss, dynamicTrailingSL)", "if (isShort) minOf(activeStopLoss, dynamicTrailingSL) else maxOf(activeStopLoss, dynamicTrailingSL)"))
    elif "val percentageLockedSL = trade.entryPrice * (1.0 + (lockedGainPct / 100.0))" in line:
        out_lines.append(line.replace("1.0 + (lockedGainPct / 100.0)", "if (isShort) (1.0 - (lockedGainPct / 100.0)) else (1.0 + (lockedGainPct / 100.0))"))
    elif "activeStopLoss = max(activeStopLoss, percentageLockedSL)" in line:
        out_lines.append(line.replace("max(activeStopLoss, percentageLockedSL)", "if (isShort) minOf(activeStopLoss, percentageLockedSL) else maxOf(activeStopLoss, percentageLockedSL)"))
    elif "activeStopLoss = max(updatedTrade.stopLoss, activeStopLoss)" in line:
        out_lines.append(line.replace("max(updatedTrade.stopLoss, activeStopLoss)", "if (isShort) minOf(updatedTrade.stopLoss, activeStopLoss) else maxOf(updatedTrade.stopLoss, activeStopLoss)"))
    else:
        out_lines.append(line)

with open("app/src/main/java/com/example/MarketEngine.kt", "w") as f:
    f.writelines(out_lines)

