import re

with open('app/src/main/java/com/example/ShoonyaWebSocketManager.kt', 'r') as f:
    content = f.read()

target = """            if (ltp != null && ts.isNotEmpty()) {
                val currentQuotes = _liveQuotes.value.toMutableMap()
                val existing = currentQuotes[ts]
                if (existing != null) {
                    currentQuotes[ts] = existing.copy(price = ltp, change = changeStr.toDoubleOrNull() ?: existing.change)
                } else {
                    currentQuotes[ts] = CommodityQuote(ts, ts, ltp, changeStr.toDoubleOrNull() ?: 0.0, 0.0, 0.0, 0.0, 0, "SHOONYA")
                }
                _liveQuotes.value = currentQuotes
            }"""

replacement = """            if (ltp != null && ts.isNotEmpty()) {
                val currentQuotes = _liveQuotes.value.toMutableMap()
                val existing = currentQuotes[ts]
                val updatedQuote = if (existing != null) {
                    existing.copy(price = ltp, change = changeStr.toDoubleOrNull() ?: existing.change)
                } else {
                    CommodityQuote(ts, ts, ltp, changeStr.toDoubleOrNull() ?: 0.0, 0.0, 0.0, 0.0, 0, "SHOONYA")
                }
                currentQuotes[ts] = updatedQuote
                
                // Map back to active base symbol like "GOLD", "GOLDM"
                var cleanTs = ts
                val bases = listOf("GOLD", "SILVER", "CRUDEOIL", "NATURALGAS", "COPPER", "ZINC", "ALUMINIUM", "NICKEL")
                for (base in bases) {
                    if (ts.startsWith(base + "M") || ts.startsWith(base + "MIC")) {
                        cleanTs = base + "M"
                        break
                    } else if (ts.startsWith(base)) {
                        cleanTs = base
                        break
                    }
                }
                currentQuotes[cleanTs] = updatedQuote.copy(symbol = cleanTs, name = cleanTs)
                
                _liveQuotes.value = currentQuotes
            }"""

new_content = content.replace(target, replacement)

with open('app/src/main/java/com/example/ShoonyaWebSocketManager.kt', 'w') as f:
    f.write(new_content)
