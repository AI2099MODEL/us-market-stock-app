import re

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    code = f.read()

# 1. Remove selectedCategoryFilter state
code = re.sub(
    r'\s*var selectedCategoryFilter by remember \{ mutableStateOf\("ALL"\) \}.*',
    '',
    code
)

# 2. Replace the section with the Row
start_marker = "val filteredResults = remember(scanResults, selectedCategoryFilter) {"
end_marker = "LazyVerticalGrid("

if start_marker in code and end_marker in code:
    start_idx = code.find(start_marker)
    end_idx = code.find(end_marker, start_idx)
    
    if start_idx != -1 and end_idx != -1:
        replacement = """val filteredResults = remember(scanResults) {
                    scanResults.filter { it.assetType == "COMMODITY" }
                }
                Column(modifier = Modifier.fillMaxSize()) {
                    LazyVerticalGrid("""
        
        # Replace the substring
        code = code[:start_idx] + replacement + code[end_idx + len("LazyVerticalGrid("):]
        
with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(code)

print("Done")
