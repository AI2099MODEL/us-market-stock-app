import re

with open('app/src/main/java/com/example/ShoonyaWebSocketManager.kt', 'r') as f:
    content = f.read()

target = """                // Subscribe to MCX Commodities
                val subscribeJson = JSONObject().apply {
                    put("t", "t")
                    // Subscribe to common MCX instruments using Shoonya exchange format
                    put("k", "MCX|GOLDM23OCTFUT#MCX|SILVERMIC23NOVFUT#MCX|CRUDEOIL23OCTFUT")
                }
                ws.send(subscribeJson.toString())"""

replacement = """                // Subscribe to MCX Commodities dynamically for 2026/2027
                val subscribeJson = JSONObject().apply {
                    put("t", "t")
                    val subscriptions = mutableListOf<String>()
                    val symbols = listOf("GOLD", "GOLDM", "SILVER", "SILVERM", "SILVERMIC", "CRUDEOIL", "CRUDEOILM", "NATURALGAS")
                    val months = listOf("JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC")
                    for (sym in symbols) {
                        for (month in months) {
                            subscriptions.add("MCX|${sym}26${month}FUT")
                            subscriptions.add("MCX|${sym}27${month}FUT")
                        }
                    }
                    put("k", subscriptions.joinToString("#"))
                }
                ws.send(subscribeJson.toString())"""

new_content = content.replace(target, replacement)

with open('app/src/main/java/com/example/ShoonyaWebSocketManager.kt', 'w') as f:
    f.write(new_content)
