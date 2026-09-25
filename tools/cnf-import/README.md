# CNF 2026 offline reference foods

Source: Health Canada, [Canadian Nutrient File 2026](https://open.canada.ca/data/en/dataset/1b6139bd-ed7e-4043-bc28-ff00e10f3109).
Licence: [Open Government Licence - Canada](https://open.canada.ca/en/open-government-licence-canada). Contains information licensed under the Open Government Licence – Canada. This is an adapted subset of nutrient fields, not an official Health Canada product.

Download the **All Resource Data** ZIP from that record as `cnf-2026.zip`. Do not use the legacy 2015 endpoint.

From the repository root:

```powershell
python tools/cnf-import/import_cnf.py tools/cnf-import/cnf-2026.zip apps/android/app/src/main/assets/cnf.db
python -m unittest discover -s tools/cnf-import -p test_import_cnf.py
```

The checked-in SQLite asset contains 5,993 foods and is approximately 4.9 MiB. The source archive SHA-256 is `f5faad8977ee6bbdd9d69c8649077cacd87d8658ad200509a4047db1e29edcdd`. Metadata includes version, source URL, licence, food count and skipped incomplete records. The importer joins using 2026 Food_Code and Nutrient_Code, validates explicit nutrient units, preserves zeroes, omits missing optional values, and rejects foods missing primary macros. No foods were rejected in this release.

2026 measure weights are grams directly, NOT the 2015 multipliers. Only Measure_Type_Code 6 becomes a serving; refuse (3) and yield (9) are excluded. Vitamin D uses code 328 in micrograms, not code 324 in IU. Nutrition is per 100 g edible portion. See the official 2026 database structure guide linked on the dataset page.

Build output is deterministic; fixture tests verify byte-identical rebuilds, invalid weights, missing nutrition, zero calories, units and exclusion of refuse/yield records. Raw downloads are ignored; the app never parses CSV or downloads reference foods at runtime.

Runtime: ReferenceFoodRepository is separate from user foods. Search debounces 250 ms and runs SQLite loading/ranking off the UI thread. Local foods precede CNF. Plurals, tokens, prefixes and curated synonyms are tried first; weak queries can use AICore only for name normalization. Strong matches skip AI, failures retain deterministic results, and normalization results are cached for the search session. Nutrition is always copied from CNF. Users review a candidate explicitly; no ambiguous choice is silently accepted. Imported local foods retain CNF source/code/version and serving options and may be edited independently.

To update: download a verified new release, review its schema/unit definitions, update the mapping/version and asset filename, run fixtures and Android tests, inspect sample values and rebuild the asset. Never replace weights with inferred values or blank nutrients with zero.
