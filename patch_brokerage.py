import re

# 1. Update MarketEngine.kt
with open('app/src/main/java/com/example/MarketEngine.kt', 'r') as f:
    content = f.read()

content = content.replace("via Dhan", "via Shoonya")
content = content.replace("After Dhan brokerage", "After Shoonya zero brokerage")
content = content.replace("Dhan fees", "Shoonya zero brokerage fees")
content = content.replace("Dhan Live Tick Stream", "Shoonya Live Tick Stream")

with open('app/src/main/java/com/example/MarketEngine.kt', 'w') as f:
    f.write(content)

# 2. Update NewsScreen.kt to add Shoonya broker referral
with open('app/src/main/java/com/example/NewsScreen.kt', 'r') as f:
    news_content = f.read()

shoonya_item = """    BrokerReferralItem(
        id = "shoonya",
        name = "Shoonya (Finvasia)",
        logoDomain = "shoonya.com",
        tagline = "Zero Brokerage Across All Segments, Free Trading APIs & Advanced WebSockets",
        primaryColor = Color(0xFF10B981),
        referralUrl = "https://shoonya.com",
        patMetric = "High Profit Margin Scale",
        revenueMetric = "Established FinTech Leader",
        activeClients = "1.0M+ Active Traders",
        features = listOf(
            "PAT Metric: Zero Brokerage Forever across Equity, F&O, Currency & MCX",
            "Absolute ₹0 Brokerage & ₹0 AMC on Trading Accounts",
            "Live WebSockets, Open API & Automated Algo Trading Support"
        ),
        isRecommended = true
    ),
"""

if "id = \"shoonya\"" not in news_content:
    news_content = news_content.replace("val BROKER_REFERRAL_LEST = listOf(\n    BrokerReferralItem(", "val BROKER_REFERRAL_LEST = listOf(\n" + shoonya_item + "    BrokerReferralItem(")
    with open('app/src/main/java/com/example/NewsScreen.kt', 'w') as f:
        f.write(news_content)

print("Brokerage references updated successfully!")
