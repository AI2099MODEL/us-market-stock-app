import re

def replace_in_file(filepath, replacements):
    with open(filepath, 'r') as f:
        content = f.read()
    for old, new in replacements:
        content = content.replace(old, new)
    with open(filepath, 'w') as f:
        f.write(content)

replace_in_file('app/src/main/java/com/example/PortfolioAnalysisView.kt', [
    ('Zerodha', 'Robinhood'),
    ('Groww', 'Fidelity'),
    ('Upstox', 'Charles Schwab'),
    ('Angel One', 'E-Trade'),
    ('ICICI Direct', 'Interactive Brokers'),
    ('HDFC Securities', 'Vanguard')
])

replace_in_file('app/src/main/java/com/example/NewsScreen.kt', [
    ('Zerodha', 'Robinhood'),
    ('Groww', 'Fidelity'),
    ('Upstox', 'Charles Schwab')
])
