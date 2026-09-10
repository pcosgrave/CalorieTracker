import csv, io, json, sqlite3, tempfile, unittest, zipfile
from contextlib import closing
from pathlib import Path
from import_cnf import build, MAPPING

class ImportTest(unittest.TestCase):
 def fixture(self, path, missing=False, bad_unit=False):
  def table(z, name, fields, rows):
   stream=io.StringIO(); writer=csv.writer(stream); writer.writerow(fields); writer.writerows(rows); z.writestr(name,stream.getvalue())
  with zipfile.ZipFile(path,"w") as z:
   table(z,"Nutrient_Name.csv",["Nutrient_Code","Nutrient_Unit"],[(k,"wrong" if bad_unit and k==208 else v[1]) for k,v in MAPPING.items()])
   table(z,"Food_Name.csv",["Food_Code","Food_Description_EN","Alternate_Description_EN","CNF_Food_Group_Code"],[["1","Vinegar","","1"],["2","Apple","","1"]])
   values=[(f,c,v) for f in ["1","2"] for c,v in [(208,0),(203,0),(204,0),(205,0),(307,2)] if not (missing and f=="2" and c==208)]
   values.append(("1",303,""))
   table(z,"Nutrient_Amount.csv",["Food_Code","Nutrient_Code","Nutrient_Amount"],values)
   table(z,"Measure_Name.csv",["Measure_Code","Measure_Description_and_Unit_EN"],[["m","1 medium"]])
   table(z,"Measure_Weight_Conversion.csv",["Food_Code","Measure_Type_Code","Measure_Code","Measure_Weight_Conversion"],[["1","6","m","167"],["1","3","m","55"],["1","9","m","50"],["2","6","m","-1"]])
   table(z,"CNF_Food_Group.csv",["CNF_Food_Group_Code","CNF_Food_Group_Description_EN"],[["1","Food"]])
 def test_serving_units_missing_values_zero_calories_and_repeatability(self):
  with tempfile.TemporaryDirectory() as d:
   d=Path(d); self.fixture(d/"source.zip")
   build(d/"source.zip",d/"one.db"); build(d/"source.zip",d/"two.db")
   self.assertEqual((d/"one.db").read_bytes(),(d/"two.db").read_bytes())
   with closing(sqlite3.connect(d/"one.db")) as db:
    n,s=db.execute("SELECT nutrients,servings FROM foods WHERE id='1'").fetchone()
    self.assertEqual(0,json.loads(n)["calories"])
    self.assertNotIn("iron",json.loads(n)["additional"])
    self.assertEqual([167], [r["grams"] for r in json.loads(s)])
    self.assertEqual("[]", db.execute("SELECT servings FROM foods WHERE id='2'").fetchone()[0])
 def test_missing_primary_excluded_and_unit_change_rejected(self):
  with tempfile.TemporaryDirectory() as d:
   d=Path(d); self.fixture(d/"missing.zip",missing=True)
   self.assertEqual("1",build(d/"missing.zip",d/"out.db")["skipped_incomplete"])
   self.fixture(d/"bad.zip",bad_unit=True)
   with self.assertRaises(ValueError):build(d/"bad.zip",d/"bad.db")
if __name__=="__main__":unittest.main()
