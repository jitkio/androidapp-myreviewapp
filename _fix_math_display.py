# Fix CardDetailScreen - use MathView for prompt, hint, answer
path = r"app\src\main\java\com\app\knowledgegraph\ui\library\CardDetailScreen.kt"
with open(path, "r", encoding="utf-8") as f:
    c = f.read()

# Add MathView import
if "MathView" not in c:
    c = c.replace(
        "import com.app.knowledgegraph.data.db.entity.Card",
        "import com.app.knowledgegraph.data.db.entity.Card\nimport com.app.knowledgegraph.ui.components.MathView"
    )

# Replace prompt display
c = c.replace(
    'Text(currentCard.prompt, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)',
    'MathView(text = currentCard.prompt, modifier = Modifier.fillMaxWidth(), baseFontSize = 22)'
)

# Replace hint display
c = c.replace(
    'Text(currentCard.hint, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodyMedium)',
    'MathView(text = currentCard.hint, modifier = Modifier.fillMaxWidth().padding(12.dp))'
)

# Replace answer display
c = c.replace(
    'Text(currentCard.answer, style = MaterialTheme.typography.bodyLarge)',
    'MathView(text = currentCard.answer, modifier = Modifier.fillMaxWidth())'
)

with open(path, "w", encoding="utf-8") as f:
    f.write(c)
print("done: CardDetailScreen.kt")

# Fix LibraryScreen - use MathView for prompt in card list
path2 = r"app\src\main\java\com\app\knowledgegraph\ui\library\LibraryScreen.kt"
with open(path2, "r", encoding="utf-8") as f:
    c2 = f.read()

if "MathView" not in c2:
    c2 = c2.replace(
        "import com.app.knowledgegraph.data.db.entity.Card",
        "import com.app.knowledgegraph.data.db.entity.Card\nimport com.app.knowledgegraph.ui.components.MathView"
    )

# Replace prompt in CardListItem - the one with titleSmall style
c2 = c2.replace(
    """Text(
                text = card.prompt,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )""",
    """MathView(
                text = card.prompt,
                modifier = Modifier.fillMaxWidth(),
                baseFontSize = 14
            )"""
)

with open(path2, "w", encoding="utf-8") as f:
    f.write(c2)
print("done: LibraryScreen.kt")

# Fix TodayScreen - use MathView for prompt, hint, answer in review
path3 = r"app\src\main\java\com\app\knowledgegraph\ui\today\TodayScreen.kt"
with open(path3, "r", encoding="utf-8") as f:
    c3 = f.read()

if "MathView" not in c3:
    c3 = c3.replace(
        "import com.app.knowledgegraph.data.db.entity.Card",
        "import com.app.knowledgegraph.data.db.entity.Card\nimport com.app.knowledgegraph.ui.components.MathView"
    )

# Replace prompt
c3 = c3.replace(
    """Text(
                text = card.prompt,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )""",
    """MathView(
                text = card.prompt,
                modifier = Modifier.fillMaxWidth(),
                baseFontSize = 22
            )"""
)

# Replace hint
c3 = c3.replace(
    """Text(
                            text = card.hint,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(12.dp)
                        )""",
    """MathView(
                            text = card.hint,
                            modifier = Modifier.fillMaxWidth().padding(12.dp)
                        )"""
)

# Replace answer
c3 = c3.replace(
    """Text(
                    text = card.answer,
                    style = MaterialTheme.typography.bodyLarge
                )""",
    """MathView(
                    text = card.answer,
                    modifier = Modifier.fillMaxWidth()
                )"""
)

with open(path3, "w", encoding="utf-8") as f:
    f.write(c3)
print("done: TodayScreen.kt")
