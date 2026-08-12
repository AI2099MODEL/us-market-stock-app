with open("app/src/main/java/com/example/MarketEngine.kt", "r") as f:
    lines = f.readlines()

new_lines = []
skip = False
for i, line in enumerate(lines):
    if "const val TOTAL_OPTIONS_CAPITAL" in line:
        continue
    if "Market & Options Open at 9:15 AM" in line:
        line = line.replace("Market & Options Open at 9:15 AM", "MCX Market Open at 9:00 AM")
    if "val activeOptionsCount =" in line:
        continue
    if "val partialThreshold = if (isOptionTrade)" in line:
        skip = True
        new_lines.append("                        val partialThreshold = 6.0\n")
        continue
    if skip:
        if "+6.0% commodity return" in line:
            skip = False
            continue
        elif "}" in line and i > 0 and "+6.0% commodity return" in lines[i-1]:
            # Just wait until skip ends
            pass
        if "}" in line.strip() and not skip:
            pass # wait this isn't robust
        # Let's just do a simpler search/replace on the whole string

