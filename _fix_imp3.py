path = r"app\src\main\java\com\app\knowledgegraph\ui\library\LibraryScreen.kt"
with open(path, "r", encoding="utf-8") as f:
    c = f.read()

# Add CardType import after CardEntity import
c = c.replace(
    "import com.app.knowledgegraph.data.db.entity.Card as CardEntity",
    "import com.app.knowledgegraph.data.db.entity.Card as CardEntity\nimport com.app.knowledgegraph.data.db.entity.CardType"
)

with open(path, "w", encoding="utf-8") as f:
    f.write(c)
print("done")
