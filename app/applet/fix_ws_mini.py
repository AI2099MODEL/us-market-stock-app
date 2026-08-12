with open('app/src/main/java/com/example/ShoonyaWebSocketManager.kt', 'r') as f:
    ws_content = f.read()

target_parse = """            if (ltp != null && ts.isNotEmpty()) {
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

replacement_parse = """            if (ltp != null && ts.isNotEmpty()) {
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
                val isMini = cleanTs.endsWith("M") && cleanTs != "GOLD"
                val finalLtp = if (isMini && ltp > 1000.0) ltp / 10.0 else ltp
                val finalChange = if (isMini && ltp > 1000.0) (changeStr.toDoubleOrNull() ?: 0.0) / 10.0 else (changeStr.toDoubleOrNull() ?: 0.0)

                val currentQuotes = _liveQuotes.value.toMutableMap()
                val existing = currentQuotes[ts]
                val updatedQuote = if (existing != null) {
                    existing.copy(price = finalLtp, change = finalChange.takeIf { it != 0.0 } ?: existing.change)
                } else {
                    CommodityQuote(cleanTs, cleanTs, finalLtp, finalChange, 0.0, 0.0, 0.0, 0, "SHOONYA")
                }
                currentQuotes[ts] = updatedQuote
                currentQuotes[cleanTs] = updatedQuote.copy(symbol = cleanTs, name = cleanTs)

                _liveQuotes.value = currentQuotes
            }"""

if target_parse in ws_content:
    ws_content = ws_content.replace(target_parse, replacement_parse)
    with open('app/src/main/java/com/example/ShoonyaWebSocketManager.kt', 'w') as f:
        f.write(ws_content)
    print("ShoonyaWebSocketManager patched successfully!")
else:
    print("Target parse block NOT found!")
