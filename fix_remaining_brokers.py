import re

def replace_in_file(filepath, replacements):
    with open(filepath, 'r') as f:
        content = f.read()
    for old, new in replacements:
        content = content.replace(old, new)
    with open(filepath, 'w') as f:
        f.write(content)

replace_in_file('app/src/main/java/com/example/PortfolioAnalysisView.kt', [
    ('"zerodha" -> "zerodha.com"', '"robinhood" -> "robinhood.com"'),
    ('"groww" -> "groww.in"', '"fidelity" -> "fidelity.com"'),
    ('"upstox" -> "upstox.com"', '"schwab" -> "schwab.com"'),
    ('"angel one", "angel" -> "angelone.in"', '"etrade", "e-trade" -> "etrade.com"'),
    ('"icici direct", "icici" -> "icicidirect.com"', '"interactive brokers", "ibkr" -> "interactivebrokers.com"'),
    ('"hdfc securities", "hdfc" -> "hdfcsec.com"', '"vanguard" -> "vanguard.com"'),
    ('"zerodha" -> Pair("Z"', '"robinhood" -> Pair("R"'),
    ('"groww" -> Pair("G"', '"fidelity" -> Pair("F"'),
    ('"upstox" -> Pair("U"', '"schwab" -> Pair("S"'),
    ('"angel one", "angel" -> Pair("A"', '"etrade", "e-trade" -> Pair("E"'),
    ('"icici direct", "icici" -> Pair("I"', '"interactive brokers", "ibkr" -> Pair("I"'),
    ('"hdfc securities", "hdfc" -> Pair("H"', '"vanguard" -> Pair("V"')
])

replace_in_file('app/src/main/java/com/example/NewsScreen.kt', [
    ('GROWW LATEST', 'FIDELITY LATEST'),
    ('"GROWW"', '"FIDELITY"'),
    ('s.contains("groww") -> "groww.in"', 's.contains("fidelity") -> "fidelity.com"'),
    ('id = "zerodha"', 'id = "robinhood"'),
    ('logoDomain = "zerodha.com"', 'logoDomain = "robinhood.com"'),
    ('https://zerodha.com/?c=JWD589&s=CONSOLE', 'https://robinhood.com'),
    ('id = "groww"', 'id = "fidelity"'),
    ('logoDomain = "groww.in"', 'logoDomain = "fidelity.com"'),
    ('https://groww.in/open-demat-account', 'https://fidelity.com'),
    ('id = "upstox"', 'id = "schwab"'),
    ('logoDomain = "upstox.com"', 'logoDomain = "schwab.com"'),
    ('https://upstox.com/open-account/', 'https://schwab.com'),
    ('name = "Angel One"', 'name = "E-Trade"')
])

replace_in_file('app/src/main/java/com/example/MainActivity.kt', [
    ('"active_provider", "Angel One"', '"active_provider", "E-Trade"'),
    ('?: "Angel One"', '?: "E-Trade"'),
    ('// Angel One', '// E-Trade'),
    ('listOf("Angel One", "Fyers", "Dhan")', 'listOf("E-Trade", "Interactive Brokers", "Charles Schwab")'),
    ('activeProvider == "Angel One"', 'activeProvider == "E-Trade"'),
    ('activeProvider == "Fyers"', 'activeProvider == "Interactive Brokers"'),
    ('activeProvider == "Dhan"', 'activeProvider == "Charles Schwab"'),
    ('angelClientCode', 'etradeClientCode'),
    ('angelApiKey', 'etradeApiKey'),
    ('angel_client_code', 'etrade_client_code'),
    ('angel_api_key', 'etrade_api_key'),
    ('SmartAPI Client Code', 'E-Trade Client ID'),
    ('SmartAPI Key', 'E-Trade API Key'),
    ('fyersAppId', 'ibkrAppId'),
    ('fyers_app_id', 'ibkr_app_id'),
    ('Fyers App ID (Client ID)', 'IBKR App ID'),
    ('fyersSecretId', 'ibkrSecretId'),
    ('fyers_secret_id', 'ibkr_secret_id'),
    ('Fyers Secret ID', 'IBKR Secret ID'),
    ('dhanClientId', 'schwabClientId'),
    ('dhan_client_id', 'schwab_client_id'),
    ('Dhan Client ID', 'Schwab Client ID'),
    ('dhanAccessToken', 'schwabAccessToken'),
    ('dhan_access_token', 'schwab_access_token'),
    ('Dhan Access Token', 'Schwab Access Token')
])

