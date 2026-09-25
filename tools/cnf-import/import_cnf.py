"""Build the offline CNF 2026 SQLite asset from the official relational ZIP."""
import argparse, csv, hashlib, io, json, math, re, sqlite3, zipfile
from pathlib import Path

# Stable CNF Nutrient_Code -> app field and verified source unit.
MAPPING = {
 208: ("calories", "kilocalorie"), 203: ("proteinGrams", "Gram"),
 204: ("fatGrams", "Gram"), 205: ("carbohydrateGrams", "Gram"),
 606: ("saturatedFat", "Gram"), 605: ("transFat", "Gram"),
 291: ("fiber", "Gram"), 269: ("sugar", "Gram"),
 307: ("sodium", "Milligram"), 601: ("cholesterol", "Milligram"),
 306: ("potassium", "Milligram"), 301: ("calcium", "Milligram"),
 303: ("iron", "Milligram"), 304: ("magnesium", "Milligram"),
 309: ("zinc", "Milligram"), 320: ("vitaminA", "Microgram"),
 401: ("vitaminC", "Milligram"), 328: ("vitaminD", "Microgram"),
 418: ("vitaminB12", "Microgram"), 417: ("folate", "Microgram"),
}
PRIMARY = {"calories", "proteinGrams", "fatGrams", "carbohydrateGrams"}
def normalize(text):
 return " ".join(re.sub(r"[^a-z0-9.%]+", " ", text.lower()).split())
def valid_number(text):
 try:
  value = float(text)
  return value if math.isfinite(value) and value >= 0 else None
 except (ValueError, TypeError): return None

def build(archive, output):
 z = zipfile.ZipFile(archive)
 def rows(name):
  return list(csv.DictReader(io.StringIO(z.read(name).decode("utf-8-sig"))))
 definitions = {int(r["Nutrient_Code"]): r for r in rows("Nutrient_Name.csv")}
 for code, (_, unit) in MAPPING.items():
  if definitions[code]["Nutrient_Unit"] != unit: raise ValueError("Unexpected nutrient unit: " + str(code))
 nutrients = {}
 for r in rows("Nutrient_Amount.csv"):
  code = int(r["Nutrient_Code"])
  value = valid_number(r["Nutrient_Amount"])
  if code in MAPPING and value is not None:
   nutrients.setdefault(r["Food_Code"], {})[MAPPING[code][0]] = value
 measures = {r["Measure_Code"]: r["Measure_Description_and_Unit_EN"] for r in rows("Measure_Name.csv")}
 servings = {}
 for r in rows("Measure_Weight_Conversion.csv"):
  # Type 6 is a household/user-defined serving. Type 3 refuse and 9 yield are NOT servings.
  grams = valid_number(r["Measure_Weight_Conversion"])
  if r["Measure_Type_Code"] == "6" and grams is not None and grams > 0 and r["Measure_Code"] in measures:
   servings.setdefault(r["Food_Code"], []).append({"id": r["Measure_Code"], "description": measures[r["Measure_Code"]], "grams": grams})
 groups = {r["CNF_Food_Group_Code"]: r["CNF_Food_Group_Description_EN"] for r in rows("CNF_Food_Group.csv")}
 output = Path(output); output.parent.mkdir(parents=True, exist_ok=True)
 temporary = output.with_suffix(".building.db")
 if temporary.exists(): temporary.unlink()
 db = sqlite3.connect(str(temporary))
 db.executescript("CREATE TABLE metadata(key TEXT PRIMARY KEY,value TEXT NOT NULL); CREATE TABLE foods(id TEXT PRIMARY KEY,name TEXT NOT NULL,normalized_name TEXT NOT NULL,food_group TEXT NOT NULL,nutrients TEXT NOT NULL,servings TEXT NOT NULL); CREATE INDEX food_name_index ON foods(normalized_name);")
 skipped = 0
 for r in sorted(rows("Food_Name.csv"), key=lambda r: int(r["Food_Code"])):
  values = nutrients.get(r["Food_Code"], {})
  # Never manufacture zeroes for missing required macros. Optional missing values stay absent.
  if not PRIMARY.issubset(values): skipped += 1; continue
  payload = {k: values[k] for k in sorted(PRIMARY)}
  payload["additional"] = {k: v for k, v in sorted(values.items()) if k not in PRIMARY}
  db.execute("INSERT INTO foods VALUES(?,?,?,?,?,?)", (r["Food_Code"], r["Food_Description_EN"], normalize(r["Food_Description_EN"] + " " + r["Alternate_Description_EN"]), groups.get(r["CNF_Food_Group_Code"], ""), json.dumps(payload, sort_keys=True), json.dumps(sorted(servings.get(r["Food_Code"], []), key=lambda s: s["id"]), sort_keys=True)))
 count = db.execute("SELECT count(*) FROM foods").fetchone()[0]
 metadata = {"source":"CNF", "version":"2026", "food_count":str(count), "skipped_incomplete":str(skipped), "archive_sha256":hashlib.sha256(Path(archive).read_bytes()).hexdigest(), "source_url":"https://open.canada.ca/data/en/dataset/1b6139bd-ed7e-4043-bc28-ff00e10f3109", "license":"Open Government Licence - Canada"}
 db.executemany("INSERT INTO metadata VALUES(?,?)", sorted(metadata.items()))
 db.commit(); assert db.execute("PRAGMA integrity_check").fetchone()[0] == "ok"
 db.execute("VACUUM"); db.close(); temporary.replace(output)
 return metadata
if __name__ == "__main__":
 parser = argparse.ArgumentParser(); parser.add_argument("archive"); parser.add_argument("output")
 args = parser.parse_args(); print(json.dumps(build(args.archive, args.output), indent=2))
