# BiteWise local UI refactor

Implementation map before edits:
- Reuse FoodItem, DiaryEntry, RecipeComponent and serving conversion functions as the shared food result. No duplicate food model or schema migration.
- Keep Room repositories, leftovers transactions, sync outbox and Health Connect exporter intact.
- Replace presentation navigation with the existing AppScreen stack plus a saved acquisition destination (meal date/meal or recipe). Save selected food and recipe draft using existing JSON serializers.
- Reuse SearchFoodScreen, AddIngredientScreen, LogFoodScreen and PhotoFoodScreen for both destinations. Recipe ingredient search is removed in favor of the same acquisition sheet. Camera and voice share review/save routing.
- Modify HomeScreen, DiaryScreen, CommonComponents and theme; introduce shared navigation, acquisition sheet and nutrition summary components.
- Keep date selector and local goals. Optional nutrient values are preserved when supplied; favorites, subscriptions and grocery scanning remain deferred.
- The current application has no screen ViewModels: it uses Compose-scoped state with callbacks into an app coordinator. This refactor preserves that architecture, moves service access out of form components, and uses saved state for navigation context.
- REST integration is deferred. Local acquisition uses saved foods and manual/local capture results. Existing backend implementation remains available for later hookup.

Reference decisions: Home has no meals; Log owns meals. Scan is a grocery placeholder. No gallery. Food Details ends with Add. Images use a neutral placeholder when no memory thumbnail exists.

## Implemented local behavior

- Dark green-accent theme, shared vector icons, neutral food imagery, compact nutrition metrics and five primary tabs.
- Home displays the selected day's calories, latest weight on/before that date, and today's actual Health Connect metrics only. Other dates show unavailable health metrics rather than today's data. Health details expand on demand.
- Log retains its date control, editing/deleting/copying and leftovers. Daily calories/progress expand into macros; each meal launches the acquisition sheet.
- The same Search, Manual, Camera and Voice review path routes portion results to a meal or recipe. Date/meal, selected food, edited record and recipe draft use saveable state. Recipe editor identity survives ingredient detours.
- Camera reads a local UPC first, then classifies label versus meal with existing on-device generation. Labels prefill the manual form through review; unknown barcodes require local/manual nutrition. No image files or gallery entry are introduced.
- Voice requests offline recognition from the existing Android provider and also permits text correction. Local phrase parsing creates shared review drafts, including unresolved foods. Recognition availability depends on installed speech support; no AI nutrition is silently saved.
- Online search controls and automatic backend sync are deferred in local mode. Existing account/sync implementation is retained under expandable account settings; repositories, schema and Health Connect logic are reused.
- Settings retain calorie ranges, goal weight, units, actual Health Connect status and existing account behavior. No invented subscription, macro-goal values, favorites data or restriction models.

## Verification

The emulator completed Log → Sep 8 → Lunch → Search → Banana → Add, showing one 105 kcal Banana under Lunch on Sep 8. Recipe → Add Ingredient opened the same sheet with recipe context; Search → Banana → Add returned one editable recipe component rather than navigating to the diary. Final Home was visually inspected for dark system bars, metric rings and navigation. Screenshots are in `ui-checks`.

Regression tests cover destination save/restore, date/meal routing, serving-scaled nutrition, recipe component results, selected-food/recipe-draft restoration, and unresolved multi-item voice review. Existing tests remain intact. Build and lint are run after each meaningful stage.

Physical-device checks still required: AICore image classification/recognition, real barcode/label capture, offline speech provider, Health Connect permission/import/export and full process-death capture recovery. Captured photos intentionally are not restored after process death; extracted fields are.

TODO for later backend work: connect remote food sources to acquisition callbacks, enable favorites after storage exists, and implement Scan grocery suitability only once its requirements/models are defined. Additional nutrient fields now use stable keys and units in the shared model and JSON payload.

Final configuration-change check: opened Breakfast on September 8, rotated the emulator to landscape and back, and confirmed the same sheet destination and visible Cancel action. Final build, unit tests and lint pass.


## September 2026 detail and CNF flows

- Existing ingredients and recipes open read-only details with top-right Edit. Recipe ingredients and cooking instructions can be read without entering logging; Add to Meal opens separate meal/portion/override controls.
- Create Food has manual barcode entry and a UPC/EAN camera scan action. Optional nutrition expansion contains only extra nutrients, with unknown values left absent. Recipe optional nutrient totals are displayed only when all ingredients supply that nutrient.
- More no longer contains Create Recipe. Creation remains in the food/recipe collection.
- CNF 2026 is bundled offline with 5,993 records. Search and unknown ingredients present local foods first and CNF choices with source attribution, then capture/manual alternatives. AICore assists names only; it never supplies CNF nutrition or serving weights. See tools/cnf-import/README.md.
- Ingredient photos and recipe photos are saved privately only on explicit attachment; meal-analysis photos remain temporary. Serving definitions are optional for older and new foods. Recipes support optional prep/total minutes.

Validation: 81 Android unit tests and two importer fixture tests; dev debug build and lint. Device-only checks remain real UPC/EAN recognition, nutrition-label capture, speech language download and AICore normalization availability.
