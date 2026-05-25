import csv
import sys
from pathlib import Path

import yaml

SCRIPT_DIR = Path(__file__).parent
INPUT_FILE = SCRIPT_DIR / "StockUniteLegale_utf8.csv"
OUTPUT_FILE = SCRIPT_DIR / "credit-bail.csv"
FILTRE_FILE = SCRIPT_DIR / "filtre.yml"
PREFIX_MATCH_FIELDS = {"activitePrincipaleNAF25UniteLegale"}


def load_filters(path: Path) -> dict[str, set[str]]:
    raw = yaml.safe_load(path.read_text(encoding="utf-8"))
    return {field: {str(v) for v in values} for field, values in raw.items()}


def matches(row: dict, filters: dict[str, set[str]]) -> bool:
    for field, values in filters.items():
        value = row.get(field, "")
        if field in PREFIX_MATCH_FIELDS:
            if not any(value.startswith(prefix) for prefix in values):
                return False
        elif value not in values:
            return False
    return True


def extract(input_path: Path, output_path: Path, filters: dict[str, set[str]]) -> int:
    matched = 0
    with (
        open(input_path, encoding="utf-8", newline="") as fin,
        open(output_path, "w", encoding="utf-8", newline="") as fout,
    ):
        reader = csv.DictReader(fin)
        writer = csv.DictWriter(fout, fieldnames=reader.fieldnames)
        writer.writeheader()

        for i, row in enumerate(reader):
            if i % 500_000 == 0:
                print(f"  {i:,} lignes traitées, {matched:,} trouvées...", file=sys.stderr)

            if matches(row, filters):
                writer.writerow(row)
                matched += 1

    return matched


def main() -> None:
    print(f"Chargement des filtres depuis {FILTRE_FILE.name}...")
    filters = load_filters(FILTRE_FILE)
    for field, values in filters.items():
        print(f"  {field}: {sorted(values)}")

    print(f"Lecture de {INPUT_FILE.name}...")
    matched = extract(INPUT_FILE, OUTPUT_FILE, filters)

    print(f"Terminé : {matched:,} lignes extraites → {OUTPUT_FILE.name}")


if __name__ == "__main__":
    main()