path = r"app\src\main\java\com\app\knowledgegraph\ui\components\MathView.kt"
with open(path, "r", encoding="utf-8") as f:
    content = f.read()

# Fix the broken newline char literal - Python turned \n into actual newline
content = content.replace("input[i] == '\n'", "input[i] == '\\n'")

# Also fix $ in Kotlin raw strings if needed
# The MathView uses regular strings so $ should be fine

with open(path, "w", encoding="utf-8") as f:
    f.write(content)
print("done")
