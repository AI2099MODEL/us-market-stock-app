import re

with open("app/src/main/java/com/example/IndianCommodityService.kt", "r") as f:
    code = f.read()

code = code.replace("suspend fun calculateDhanBrokerage(turnover: Double, isSell: Boolean, isOptions: Boolean = false): BrokerageDetails = withContext(Dispatchers.IO) {", "fun calculateDhanBrokerage(turnover: Double, isSell: Boolean, isOptions: Boolean = false): BrokerageDetails {")

with open("app/src/main/java/com/example/IndianCommodityService.kt", "w") as f:
    f.write(code)

