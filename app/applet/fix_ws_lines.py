with open('app/src/main/java/com/example/ShoonyaWebSocketManager.kt', 'r') as f:
    lines = f.readlines()

new_lines = []
i = 0
while i < len(lines):
    if "if (ltp != null && ts.isNotEmpty()) {" in lines[i]:
        # skip until matching closing brace of this if block
        # we know exactly what we want to replace
        new_lines.append("            if (ltp != null && ts.isNotEmpty()) {\n")
        new_lines.append("                var cleanTs = ts\n")
        new_lines.append("                val bases = listOf(\"GOLD\", \"SILVER\", \"CRUDEOIL\", \"NATURALGAS\", \"COPPER\", \"ZINC\", \"ALUMINIUM\", \"NICKEL\")\n")
        new_lines.append("                for (base in bases) {\n")
        new_lines.append("                    if (ts.startsWith(base + \"M\") || ts.startsWith(base + \"MIC\")) {\n")
        new_lines.append("                        cleanTs = base + \"M\"\n")
        new_lines.append("                        break\n")
        new_lines.append("                    } else if (ts.startsWith(base)) {\n")
        new_lines.append("                        cleanTs = base\n")
        new_lines.append("                        break\n")
        new_lines.append("                    }\n")
        new_lines.append("                }\n")
        new_lines.append("                val isMini = cleanTs.endsWith(\"M\") && cleanTs != \"GOLD\"\n")
        new_lines.append("                val finalLtp = if (isMini && ltp > 1000.0) ltp / 10.0 else ltp\n")
        new_lines.append("                val finalChange = if (isMini && ltp > 1000.0) (changeStr.toDoubleOrNull() ?: 0.0) / 10.0 else (changeStr.toDoubleOrNull() ?: 0.0)\n\n")
        new_lines.append("                val currentQuotes = _liveQuotes.value.toMutableMap()\n")
        new_lines.append("                val existing = currentQuotes[ts]\n")
        new_lines.append("                val updatedQuote = if (existing != null) {\n")
        new_lines.append("                    existing.copy(price = finalLtp, change = finalChange.takeIf { it != 0.0 } ?: existing.change)\n")
        new_lines.append("                } else {\n")
        new_lines.append("                    CommodityQuote(cleanTs, cleanTs, finalLtp, finalChange, 0.0, 0.0, 0.0, 0, \"SHOONYA\")\n")
        new_lines.append("                }\n")
        new_lines.append("                currentQuotes[ts] = updatedQuote\n")
        new_lines.append("                currentQuotes[cleanTs] = updatedQuote.copy(symbol = cleanTs, name = cleanTs)\n\n")
        new_lines.append("                _liveQuotes.value = currentQuotes\n")
        new_lines.append("            }\n")
        # skip old block lines
        i += 1
        brace_count = 1
        while i < len(lines) and brace_count > 0:
            if "{" in lines[i]:
                brace_count += lines[i].count("{")
            if "}" in lines[i]:
                brace_count -= lines[i].count("}")
            i += 1
    else:
        new_lines.append(lines[i])
        i += 1

with open('app/src/main/java/com/example/ShoonyaWebSocketManager.kt', 'w') as f:
    f.writelines(new_lines)

print("Line-based replacement successful!")
