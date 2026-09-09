# Local photo food logging

Tap the **camera icon beside the mic on the diary page**, frame the meal, then tap **Analyze photo**. There is no gallery picker. Android AICore / Gemini Nano analyzes it locally using ML Kit Prompt API (`1.0.0-beta2`). The first use may download the model. Unsupported or unprepared devices display an error; there is no cloud image fallback.

Review each food, its weight in grams, and any scale note. A readable scale value is converted from g/kg/oz/lb and assigned only to the identified food being weighed. A whole-plate reading is shown as context; individual weights remain estimates. The user must confirm tare/container weight. Visual estimates and scale digit recognition are not measurements guaranteed by the app.

All on-device requests use a maximum of 256 output tokens. Meal analysis first discovers short food names, then reads each food separately using the same in-memory photo. This avoids requesting a whole meal's detailed JSON in one short response. Groups larger than 12 foods ask for a smaller photo instead of silently dropping entries. Multiple foods require multiple sequential local requests and may take longer.

Detail prompts request only one food in a compact object. If the model instead returns a list, the parser selects an unambiguous name match and preserves scale attribution. A failed detail request gets one bounded retry; a second failure creates an editable row for that discovered food with missing values left blank. Other foods still reach review. Missing weights/nutrition must be completed or the item removed before saving. Cancellation continues to stop the analysis.

Known foods with unambiguous names reuse saved nutrition. Foods saved in serving or volume units require an amount in compatible units; a pictured gram estimate is not converted into servings or volume. The model supplies food identification and weight only; model nutrition is neither requested nor used. Select an unknown review item to open Add Ingredient with its name prefilled and blank nutrition. Enter values or scan a nutrition label, then save to persist and link the food and return to the same review. Cancel returns without changing the item. The photo remains in memory throughout. If the new food's serving unit cannot convert from grams, enter its portion amount in that unit on return. Unresolved items must be added, matched to a saved food, or removed before saving the meal. Saving the meal creates individual diary entries in one Room transaction, including sync outbox changes. Photos do not enter the sync payload.

CameraX captures directly into memory using `OnImageCapturedCallback`. The image is rotated upright and resized in memory. ImageProxy buffers are always closed. After successful analysis, a separate copy of at most 640 pixels on its longest edge supplies the captured-meal thumbnails in the condensed review rows. It remains only in memory during review, and its references are dropped on save, exit, retake, or removal of the last item. Rendered thumbnails use normal garbage collection rather than manual recycling while Compose might still draw them. The analysis bitmap is released separately. There is no file output, FileProvider, MediaStore write, external camera intent, or photo in saved instance state. Activity recreation preserves the extracted review fields but not the photo. Retrying requires a fresh capture.

Barcode scanning now automatically imports successful catalog/Open Food Facts lookups and opens food logging. Open Food Facts values use a consistent serving basis, preserve zero nutrients, and convert kJ to kcal. Missing/incomplete nutrition falls back to manual creation. Lookup failures can be retried.

## Nutrition labels

Use **Scan nutrition label** in Add/Edit Ingredient. From a UPC result's logging screen, choose **Edit food / scan nutrition label** first. The same memory-only camera captures the label for local transcription, without a network food lookup or gallery option. The result fills serving size/unit, calories, protein, total carbohydrate, and total fat. The compact label request omits name and brand to prioritize nutrition within the output limit; existing product identity and UPC remain unchanged.

The prompt selects one printed nutrition column and its serving basis, preferring per-serving as sold. The parser rejects an unreadable serving basis, converts kJ to kcal when necessary, preserves zeros/decimals, and leaves unreadable nutrient fields blank. Successful scans replace the entire nutrition section together so previous UPC values cannot be mixed with a new serving basis. Check the populated values and complete blanks before saving. Failed or canceled scans leave the form unchanged. Zero-calorie foods can be saved.

## Review and food management

Review rows start collapsed. Swipe left to delete or right to edit, using the same 35% threshold and colored edit/delete backgrounds as food rows elsewhere. Accessible edit/delete actions are also available. **Add missed food** opens an editor with the entire captured photo fitted above the fields, without cropping it to the thumbnail. The photo remains in memory only.

Matched voice food commands now open this review before saving, without a thumbnail column. The dictated amount, original unit, and inferred/explicit meal are prefilled. Volume and serving units remain volume and serving units. Existing voice meal-copy commands retain their copy behavior; unresolved voice matches still use food search.

Food names default to capitalized word starts in review and saved food/recipe forms, preserving existing acronyms. **Settings → Manage foods & recipes** opens a searchable list of locally known foods and recipes with add/edit/delete actions. Deleting a built-in food hides it; deleting a saved food uses the existing sync deletion path. Diary entries retain their saved food snapshots.

## Verification

Run from `apps/android`:

```powershell
.\gradlew.bat :app:testDevDebugUnitTest :app:assembleDevDebug :app:lintDevDebug
```

Pixel acceptance checks (require a physical supported device and prepared AICore):

- First-use model download, unavailable model, cancel, retry, and airplane-mode analysis after setup.
- Camera icon placement beside mic; camera permission denied; capture; portrait/landscape images; confirm there is no gallery option.
- Verify capture creates no app-cache or gallery photo, including failure/cancel/retake paths.
- One food on a tared scale, with g, kg, oz and lb readings; blurry/absent scale digits.
- Several foods on one plate: confirm the plate total is never assigned to each food.
- Edit weights, remove foods, add a missed food, select a saved food, and save to each meal/date.
- Confirm new foods exist in search, saved nutrition scales with the edited grams, and a repeat barcode scan reuses the imported food.
- Recreate the activity during capture/review and verify review fields are retained. In-flight analysis requires a new capture after recreation.
- Unknown barcode with complete nutrition auto-imports; missing nutrition and offline lookup allow manual entry.
- Scan a label for a new food and correct an imported UPC food; verify UPC stays unchanged and the corrected food is used on return to logging.
- Labels with per-serving/per-100 columns, g/ml/serving units, kcal/kJ, zeros, small decimals, missing fields, and blurry/non-label images; verify column consistency and editable results.
- Open Add missed food and verify the whole photo is visible; swipe left/right in collapsed and expanded reviews; cancel a voice review and confirm nothing was logged.
- Dictate amounts in grams, ml and servings; edit before saving; verify meal/date and nutrition totals.
- Settings food management: add/edit/delete a food and a recipe, search/filter, and return to Settings.

The deterministic tests cover scale attribution and conversion, malformed/empty results, invalid weights/nutrition, saved-food matching, meal batch generation, gram aliases, and Open Food Facts serving normalization. They do not establish recognition accuracy on real photos.

References: [ML Kit Prompt API setup](https://developers.google.com/ml-kit/genai/prompt/android/get-started), [Open Food Facts nutrition schema](https://openfoodfacts.github.io/documentation/docs/Product-Opener/schemas/schemas/product_nutrition/).

## Nutrition search for unknown foods

Opening Add Ingredient from an unknown review item automatically searches the Canadian Nutrient File by food name. A unique exact name match fills blank nutrition fields; other matches are shown for selection so preparation variants are not silently substituted. **Find nutrition for 100 g** is also available in the ingredient form for new searches and retries. Selecting a result fills serving size 100 g, calories, protein, carbs, and fat from CNF's per-100-g edible-portion data, with the source and food description visible. Values remain editable. Only the name is used for online lookup; the meal photo stays on the phone.

Missing calorie or macro data is rejected rather than converted to zeros. Explicit zero calories and zero macros are valid. Late search responses do not overwrite edits or scanned label/UPC values. Network failures or no matches leave manual entry and label scanning available. Save and cancel still return to the preserved review.

Validation includes CNF field names (including live API `Total Fat`), word-order/plural matching, missing versus zero nutrition, and a zero-calorie ingredient saved into a review meal. On-device checks: open unknown Apple, select the correct preparation, confirm 100 g and all macros; edit fields during lookup; retry offline; save a zero-calorie item; cancel back to review.
