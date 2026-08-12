import re

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

target = """                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    val sign = if (changePct > 0) "+" else ""
                    Text(
                        text = "$sign${String.format(Locale.US, "%.2f", changePct)}%",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (changePct >= 0) TrendTextGreen else StopLossRedText
                    )
                    Text(
                        text = formattedPrice,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = cmpColor,
                        maxLines = 1
                    )
                }"""

replacement = """                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    val sign = if (changePct > 0) "+" else ""
                    Text(
                        text = "Shoonya Live",
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF10B981),
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "$sign${String.format(Locale.US, "%.2f", changePct)}%",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (changePct >= 0) TrendTextGreen else StopLossRedText
                        )
                        Text(
                            text = formattedPrice,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = cmpColor,
                            maxLines = 1
                        )
                    }
                }"""

new_content = content.replace(target, replacement)

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(new_content)
