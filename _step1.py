path = r"app\src\main\java\com\app\knowledgegraph\data\repository\CardRepository.kt"
with open(path, "r", encoding="utf-8") as f:
    c = f.read()
# Add insertCard method if not exists
if "suspend fun insertCard" not in c:
    insert_pos = c.rfind("}")
    addition = """
    suspend fun insertCard(card: Card): Long {
        return cardDao.insert(card)
    }

    suspend fun insertCards(cards: List<Card>): List<Long> {
        return cards.map { cardDao.insert(it) }
    }
"""
    c = c[:insert_pos] + addition + c[insert_pos:]
    # Make sure Card import exists
    if "import com.app.knowledgegraph.data.db.entity.Card" not in c:
        c = c.replace(
            "import com.app.knowledgegraph.data.db.entity.Card",
            "import com.app.knowledgegraph.data.db.entity.Card"
        )
        if "import com.app.knowledgegraph.data.db.entity.Card" not in c:
            c = c.replace("package ", "package ", 1)
            # add after imports
            last_import = c.rfind("import ")
            end_of_line = c.index("\n", last_import)
            c = c[:end_of_line+1] + "import com.app.knowledgegraph.data.db.entity.Card\n" + c[end_of_line+1:]
    with open(path, "w", encoding="utf-8") as f:
        f.write(c)
print("done: CardRepository")
