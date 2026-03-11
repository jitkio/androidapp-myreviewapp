path = r"app\src\main\java\com\app\knowledgegraph\ui\library\LibraryScreen.kt"
with open(path, "r", encoding="utf-8") as f:
    c = f.read()

# Remove duplicate import and non-existent MathViewType
c = c.replace(
    "import com.app.knowledgegraph.data.db.entity.Card as CardEntity\nimport com.app.knowledgegraph.ui.components.MathView\nimport com.app.knowledgegraph.data.db.entity.Card as CardEntity\nimport com.app.knowledgegraph.ui.components.MathViewType",
    "import com.app.knowledgegraph.data.db.entity.Card as CardEntity\nimport com.app.knowledgegraph.ui.components.MathView"
)

with open(path, "w", encoding="utf-8") as f:
    f.write(c)
print("done")
