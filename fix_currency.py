import os
import glob

files = glob.glob("app/src/main/java/com/example/**/*.kt", recursive=True)
for path in files:
    with open(path, "r") as f:
        content = f.read()
    
    content = content.replace("₹{", "${")
    content = content.replace('"$${', '"₹${')
    content = content.replace('"$%', '"₹%')
    content = content.replace('"$', '"₹')
    
    with open(path, "w") as f:
        f.write(content)
    print(f"Fixed currency in {path}")

print("Currency fix complete.")
