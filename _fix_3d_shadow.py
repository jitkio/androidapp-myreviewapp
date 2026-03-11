"""
批量给所有 Card / Button / FilledTonalButton 加 3D 阴影
运行: python _fix_3d_shadow.py
"""
import os, re

UI_DIR = os.path.join("app", "src", "main", "java", "com", "app", "knowledgegraph", "ui")

SHADOW_IMPORT = "import com.app.knowledgegraph.ui.components.cardShadow3d"
BUTTON_SHADOW_IMPORT = "import com.app.knowledgegraph.ui.components.buttonShadow3d"
LIGHT_SHADOW_IMPORT = "import com.app.knowledgegraph.ui.components.lightShadow3d"
STATIC_SHADOW_IMPORT = "import com.app.knowledgegraph.ui.components.staticShadow3d"

# 不改的文件
SKIP_FILES = {"Shadow3D.kt", "Buttons.kt", "Cards.kt", "Theme.kt", "Color.kt",
              "DesignTokens.kt", "Animation.kt", "Type.kt"}

changes_log = []

def process_file(filepath):
    with open(filepath, "r", encoding="utf-8") as f:
        content = f.read()

    original = content
    needs_card_import = False
    needs_button_import = False
    needs_light_import = False
    needs_static_import = False

    # --- Card( 加阴影 ---
    # 模式1: Card(\n        modifier = Modifier\n            .xxx → 在 Modifier 后加 .cardShadow3d()
    # 模式2: Card(\n        modifier = modifier\n            .xxx → 在 modifier 后加 .cardShadow3d()
    # 模式3: Card( 没有 modifier → 加一个

    lines = content.split("\n")
    new_lines = []
    i = 0
    while i < len(lines):
        line = lines[i]
        stripped = line.strip()

        # 检测 Card( 调用（排除 import、注释、自定义组件引用）
        is_card_call = False
        is_button_call = False

        if re.match(r'^(\s*)(Card|ElevatedCard)\s*\(', stripped) and "import" not in line and "//" not in line.split("Card")[0]:
            is_card_call = True
        if re.match(r'^(\s*)(Button|FilledTonalButton)\s*\(', stripped) and "import" not in line and "//" not in line.split("Button")[0]:
            is_button_call = True

        if is_card_call or is_button_call:
            shadow_fn = ".cardShadow3d()" if is_card_call else ".buttonShadow3d()"

            # 先把这个调用的所有行收集起来（到匹配的括号为止找 modifier）
            # 简单策略：往下看10行找 modifier
            found_modifier = False
            block = [line]
            j = i + 1
            paren_depth = line.count("(") - line.count(")")
            search_limit = min(i + 15, len(lines))

            while j < search_limit and paren_depth > 0:
                next_line = lines[j]
                block.append(next_line)
                paren_depth += next_line.count("(") - next_line.count(")")

                # 找 modifier = Modifier 或 modifier = modifier
                modifier_match = re.search(r'(modifier\s*=\s*)(Modifier|modifier)', next_line)
                if modifier_match and shadow_fn not in next_line and "shadow" not in next_line.lower():
                    # 检查这行后面是不是接着 .xxx
                    # 在 Modifier/modifier 后面加 .cardShadow3d()
                    indent_and_before = next_line[:modifier_match.end()]
                    after = next_line[modifier_match.end():]
                    new_next_line = indent_and_before + shadow_fn + after
                    block[-1] = new_next_line
                    found_modifier = True
                    if is_card_call:
                        needs_card_import = True
                    else:
                        needs_button_import = True
                    break
                j += 1

            if found_modifier:
                new_lines.extend(block)
                i = j + 1
                continue

        new_lines.append(line)
        i += 1

    content = "\n".join(new_lines)

    # 加 import
    if needs_card_import and SHADOW_IMPORT not in content:
        content = add_import(content, SHADOW_IMPORT)
    if needs_button_import and BUTTON_SHADOW_IMPORT not in content:
        content = add_import(content, BUTTON_SHADOW_IMPORT)

    if content != original:
        with open(filepath, "w", encoding="utf-8") as f:
            f.write(content)
        changes_log.append(filepath)


def add_import(content, import_line):
    """在最后一个 import 语句后面插入新 import"""
    lines = content.split("\n")
    last_import_idx = 0
    for idx, line in enumerate(lines):
        if line.strip().startswith("import "):
            last_import_idx = idx
    lines.insert(last_import_idx + 1, import_line)
    return "\n".join(lines)


def main():
    for root, dirs, files in os.walk(UI_DIR):
        for fname in files:
            if not fname.endswith(".kt"):
                continue
            if fname in SKIP_FILES:
                continue
            # 跳过 components 目录下的自定义组件文件
            if "components" in root:
                continue
            filepath = os.path.join(root, fname)
            process_file(filepath)

    print(f"\n=== 3D Shadow 批量修改完成 ===")
    print(f"修改了 {len(changes_log)} 个文件:")
    for f in changes_log:
        print(f"  ✓ {f}")
    if not changes_log:
        print("  没有需要修改的文件")


if __name__ == "__main__":
    main()
