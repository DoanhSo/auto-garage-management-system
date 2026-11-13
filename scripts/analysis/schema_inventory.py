"""Inventory the MySQL schema built by the Flyway migrations (read-only).

Usage:
    python -I scripts/analysis/schema_inventory.py [--check docs/erd]

Reads src/main/resources/db/migration/V*.sql in version order, replays
CREATE TABLE / ALTER TABLE / CREATE VIEW statements and writes
docs/erd/schema-inventory.md. With --check DIR it also verifies that every
table and every foreign key appears in the Chen diagrams (*.puml) under DIR.
"""
import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parents[2]
MIGRATIONS = ROOT / "src" / "main" / "resources" / "db" / "migration"
OUT = ROOT / "docs" / "erd" / "schema-inventory.md"


def version_key(path):
    return int(re.match(r"V(\d+)__", path.name).group(1))


def strip_comments(sql):
    sql = re.sub(r"/\*.*?\*/", "", sql, flags=re.S)
    return "\n".join(line.split("--", 1)[0] for line in sql.splitlines())


def split_top_level(body):
    """Split a parenthesised body on commas that are not nested."""
    parts, depth, current = [], 0, []
    for ch in body:
        if ch == "(":
            depth += 1
        elif ch == ")":
            depth -= 1
        if ch == "," and depth == 0:
            parts.append("".join(current).strip())
            current = []
        else:
            current.append(ch)
    if "".join(current).strip():
        parts.append("".join(current).strip())
    return parts


def cols(text):
    return [c.strip().strip("`") for c in text.split(",")]


class Table:
    def __init__(self, name, version):
        self.name = name
        self.version = version
        self.columns = {}  # name -> definition
        self.pk = []
        self.fks = {}  # constraint -> (cols, ref_table, ref_cols)
        self.uniques = {}
        self.checks = 0


def parse_column(table, item):
    m = re.match(r"`?(\w+)`?\s+(.*)", item, flags=re.S)
    if not m:
        return
    name, definition = m.group(1), " ".join(m.group(2).split())
    table.columns[name] = definition
    if re.search(r"\bPRIMARY KEY\b", definition, re.I):
        table.pk = [name]
    if re.search(r"\bUNIQUE\b", definition, re.I):
        table.uniques[f"uq_{table.name}_{name}"] = [name]


def parse_constraint(table, item, anon_index):
    m = re.match(r"(?:CONSTRAINT\s+`?(\w+)`?\s+)?(.*)", item, flags=re.S | re.I)
    name, rest = m.group(1), m.group(2).strip()
    fk = re.match(r"FOREIGN KEY\s*\(([^)]*)\)\s*REFERENCES\s+`?(\w+)`?\s*\(([^)]*)\)", rest, re.I)
    if fk:
        table.fks[name or f"fk_{table.name}_{anon_index}"] = (cols(fk.group(1)), fk.group(2), cols(fk.group(3)))
        return True
    uq = re.match(r"UNIQUE(?:\s+(?:KEY|INDEX))?\s*(?:`?(\w+)`?)?\s*\(([^)]*)\)", rest, re.I)
    if uq:
        table.uniques[name or uq.group(1) or f"uq_{table.name}_{anon_index}"] = cols(uq.group(2))
        return True
    pk = re.match(r"PRIMARY KEY\s*\(([^)]*)\)", rest, re.I)
    if pk:
        table.pk = cols(pk.group(1))
        return True
    if re.match(r"CHECK\b", rest, re.I):
        table.checks += 1
        return True
    if re.match(r"(INDEX|KEY|FULLTEXT)\b", rest, re.I):
        return True
    return False


def load():
    tables, views = {}, {}
    for path in sorted(MIGRATIONS.glob("V*__*.sql"), key=version_key):
        version = f"V{version_key(path)}"
        sql = strip_comments(path.read_text(encoding="utf-8"))
        for statement in sql.split(";"):
            stmt = statement.strip()
            if not stmt:
                continue
            create = re.match(r"CREATE TABLE\s+(?:IF NOT EXISTS\s+)?`?(\w+)`?\s*\((.*)\)[^)]*$", stmt, re.S | re.I)
            if create:
                table = Table(create.group(1), version)
                for i, item in enumerate(split_top_level(create.group(2))):
                    if not parse_constraint(table, item, i):
                        parse_column(table, item)
                tables[table.name] = table
                continue
            view = re.match(r"CREATE (?:OR REPLACE )?VIEW\s+`?(\w+)`?", stmt, re.I)
            if view:
                views[view.group(1)] = version
                continue
            alter = re.match(r"ALTER TABLE\s+`?(\w+)`?\s+(.*)", stmt, re.S | re.I)
            if alter and alter.group(1) in tables:
                table = tables[alter.group(1)]
                for i, action in enumerate(split_top_level(alter.group(2))):
                    action = " ".join(action.split())
                    add_col = re.match(r"ADD COLUMN\s+(.*)", action, re.I)
                    add_con = re.match(r"ADD\s+(CONSTRAINT .*|FOREIGN KEY .*|UNIQUE .*|CHECK .*|INDEX .*|KEY .*)", action, re.I)
                    drop_col = re.match(r"DROP COLUMN\s+`?(\w+)`?", action, re.I)
                    drop_idx = re.match(r"DROP (?:INDEX|KEY|CONSTRAINT|CHECK|FOREIGN KEY)\s+`?(\w+)`?", action, re.I)
                    modify = re.match(r"(?:MODIFY|CHANGE)\s+(?:COLUMN\s+)?(.*)", action, re.I)
                    if add_col:
                        parse_column(table, add_col.group(1))
                    elif add_con:
                        parse_constraint(table, add_con.group(1), f"{version}_{i}")
                    elif drop_col:
                        table.columns.pop(drop_col.group(1), None)
                    elif drop_idx:
                        table.uniques.pop(drop_idx.group(1), None)
                        table.fks.pop(drop_idx.group(1), None)
                    elif modify:
                        parse_column(table, modify.group(1))
    return tables, views


def write_markdown(tables, views):
    fk_total = sum(len(t.fks) for t in tables.values())
    col_total = sum(len(t.columns) for t in tables.values())
    lines = [
        "# Kiểm kê schema (sinh tự động)",
        "",
        "> Sinh bởi `scripts/analysis/schema_inventory.py` từ migration Flyway. Không sửa tay; chạy lại script khi thêm migration.",
        "",
        f"- **Bảng**: {len(tables)} · **Cột**: {col_total} · **Khóa ngoại**: {fk_total} · **View**: {len(views)} ({', '.join(sorted(views))})",
        "",
        "| Bảng | Tạo ở | Cột | PK | Khóa ngoại → bảng | UNIQUE | CHECK |",
        "|---|---|---|---|---|---|---|",
    ]
    for name in sorted(tables):
        t = tables[name]
        fks = "<br>".join(f"{', '.join(c)} → {ref}" for c, ref, _ in t.fks.values()) or "—"
        uqs = "<br>".join(", ".join(c) for c in t.uniques.values()) or "—"
        lines.append(f"| `{name}` | {t.version} | {len(t.columns)} | {', '.join(t.pk) or '—'} | {fks} | {uqs} | {t.checks} |")
    lines += ["", "## Cột theo bảng", ""]
    for name in sorted(tables):
        t = tables[name]
        lines.append(f"### `{name}`")
        lines.append("")
        lines.append(", ".join(f"`{c}`" for c in t.columns))
        lines.append("")
    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text("\n".join(lines) + "\n", encoding="utf-8")
    return len(tables), col_total, fk_total


def check(tables, directory):
    """Every table must be an entity and every FK a relationship in the Chen diagrams."""
    text = "\n".join(p.read_text(encoding="utf-8") for p in pathlib.Path(directory).glob("0[1-9]-*.puml"))
    entities = re.findall(r'entity\s+"[^"]*\((\w+)\)"', text)
    missing = sorted(set(tables) - set(entities))
    duplicated = sorted({e for e in entities if entities.count(e) > 1 and e in tables})
    # A FK is covered when a relationship note "fk: <table>.<col>" exists in some diagram.
    covered = set(re.findall(r"fk:\s*(\w+\.\w+)", text))
    uncovered = []
    for t in tables.values():
        for fk_cols, ref, _ in t.fks.values():
            key = f"{t.name}.{fk_cols[0]}"
            if key not in covered:
                uncovered.append(f"{key} → {ref}")
    print(f"entities: {len(set(entities) & set(tables))}/{len(tables)}")
    print("missing tables:", missing or "none")
    print("tables drawn more than once as full entities:", duplicated or "none")
    print(f"foreign keys covered: {sum(len(t.fks) for t in tables.values()) - len(uncovered)}/{sum(len(t.fks) for t in tables.values())}")
    for item in uncovered:
        print("  uncovered FK:", item)
    return not missing and not uncovered


if __name__ == "__main__":
    tables, views = load()
    n, c, f = write_markdown(tables, views)
    print(f"tables={n} columns={c} fks={f} views={len(views)} -> {OUT.relative_to(ROOT)}")
    if len(sys.argv) == 3 and sys.argv[1] == "--check":
        sys.exit(0 if check(tables, sys.argv[2]) else 1)
