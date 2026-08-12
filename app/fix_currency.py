import os
import glob

files = glob.glob("app/src/main/java/com/example/**/*.kt", recursive=True)
for path in files:
    with open(path, "r") as f:
        content = f.read()
    
    # Fix accidental ₹{ interpolation
    content = content.replace("₹{", "${")
    
    # Replace price string formatting like "$%" or "$," or `"$` + digit
    # For instance, `"$${` -> `"₹${`, `"$%" -> `"₹%`, `"$," -> `"₹,`
    content = content.replace('"$${', '"₹${')
    content = content.replace('"$%', '"₹%')
    content = content.replace('"$', '"₹')
    content = content.replace('($valSign$', '(valSign + "₹"')
    
    with open(path, "w") as f:
        f.write(content)
    print(f"Fixed currency in {path}")

print("Currency fix complete.")
