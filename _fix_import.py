path = r"app\src\main\java\com\app\knowledgegraph\ui\library\LibraryScreen.kt"
with open(path, "r", encoding="utf-8") as f:
    c = f.read()

# Remove the entity Card import since LibraryScreen uses it as a parameter type
# but also uses material3 Card composable. We need to alias one.
c = c.replace(
    "import com.app.knowledgegraph.data.db.entity.Card\nimport com.app.knowledgegraph.ui.components.MathView",
    "import com.app.knowledgegraph.data.db.entity.Card as CardEntity\nimport com.app.knowledgegraph.ui.components.MathView"
)

# Now fix references to entity Card in function parameters
c = c.replace("card: Card,", "card: CardEntity,")
c = c.replace("card: Card\n", "card: CardEntity\n")
c = c.replace(": List<Card>", ": List<CardEntity>")

with open(path, "w", encoding="utf-8") as f:
    f.write(c)
print("done")
