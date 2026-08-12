import re

with open("app/src/main/java/com/example/DhanPortfolioService.kt", "r") as f:
    code = f.read()

# Replace demoHoldings usage
code = re.sub(
    r'val demoHoldings = getFallbackDhanHoldings\(todayStr\)',
    'val demoHoldings = emptyList<PortfolioHolding>()',
    code
)

# Replace the fallback function
code = re.sub(
    r'private fun getFallbackDhanHoldings.*?return listOf\(.*?\n    \}',
    'private fun getFallbackDhanHoldings(todayStr: String): List<PortfolioHolding> = emptyList()',
    code,
    flags=re.DOTALL
)

with open("app/src/main/java/com/example/DhanPortfolioService.kt", "w") as f:
    f.write(code)

