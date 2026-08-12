import re

with open("app/src/main/java/com/example/AutoTraderTab.kt", "r") as f:
    code = f.read()

code = code.replace(
    """• Post-3:00 PM Engine Shift: Auto-squares off option gains & executes BTST Equity (High RVOL > 2.5x Breakout Stocks)\\n• Weekly Expiry Contracts: Scans high-liquidity Weekly Index Options (NIFTY, BANKNIFTY, FINNIFTY)\\n• Capital Limits: ₹2,00,000 Options + ₹2,00,000 Commodities/BTST (₹4 Lakhs Total Cap)""",
    """• Strictly MCX Commodities Only (9:00 AM - 11:30 PM IST)\\n• Real-Time Dhan WebSockets: Trades executed using live tick streaming P&L\\n• Capital Limit: ₹2,00,000 Total Commodity Allocation (₹50,000 per trade)"""
)

with open("app/src/main/java/com/example/AutoTraderTab.kt", "w") as f:
    f.write(code)

