import re

def replace_in_file(filepath, replacements):
    with open(filepath, 'r') as f:
        content = f.read()
    for old, new in replacements:
        content = content.replace(old, new)
    with open(filepath, 'w') as f:
        f.write(content)

replace_in_file('app/src/main/java/com/example/LiveScreen.kt', [
    ('NSE or BSE', 'NYSE or NASDAQ'),
    ('RELIANCE, TATAMOTORS', 'AAPL, TSLA')
])

replace_in_file('app/src/main/java/com/example/PortfolioAnalysisView.kt', [
    ('"NSE"', '"NYSE"'),
    ('NSE:', 'NYSE:')
])

replace_in_file('app/src/main/java/com/example/NewsScreen.kt', [
    ('else -> "NSE"', 'else -> "NYSE"')
])

replace_in_file('app/src/main/java/com/example/MainActivity.kt', [
    ('NSE Top 100', 'S&P 500')
])
