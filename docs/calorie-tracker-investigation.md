# Calorie Tracker Investigation

Date: 2026-05-20

## Product Goal

Build a calorie tracking product with:

- Web app and Android app
- Accounts with Google sign-in
- Barcode scanning
- Manual food entry tied to barcode/GTIN/UPC values
- A first-party food catalog created from user-entered nutrition facts
- User food diary and calorie/macro tracking
- AWS hosting, provisioned with Terraform
- Data lake or analytical history for long-term product insight

## Key Recommendation

Use your own application backend and food-log database. Do not build as a thin wrapper around MyFitnessPal or Fitbit.

For MVP, make nutrition entry fully manual and use barcode scanning only as a fast lookup key into your own catalog:

1. User scans or types a barcode.
2. If your database already knows that barcode, show the saved product and serving options.
3. If the barcode is new, ask the user to enter product name, serving size, calories, and macros from the label.
4. Save the product and link it to that barcode for future logging.

External nutrition providers should move out of MVP. Open Food Facts, USDA FoodData Central, FatSecret, Edamam, and Nutritionix remain useful later for suggestions, prefill, validation, or enrichment, but they are not required to ship the first version.

For authentication, use Amazon Cognito User Pools federated to Google. For Android barcode scanning, use Google ML Kit. For analytics, put scan, product-created, product-updated, and diary events in S3 and query with Glue/Athena, with Lake Formation if governance becomes important.

## Integration Options

### MyFitnessPal

MyFitnessPal should be treated as a competitor and maybe an eventual partner, not as a core dependency. Publicly available information indicates its API was private and partner-selected. Its current help docs emphasize partner categories and app integrations rather than a public developer food database API.

Verdict: do not depend on MyFitnessPal for MVP.

### Fitbit

Fitbit has nutrition Web API endpoints for querying and modifying food and water data, including custom/private food creation, food logs, food search, and food database lookup. However, the Fitbit developer docs state the legacy Fitbit Web API is being deprecated in September 2026 and apps need to migrate to Google Health API.

Verdict: useful as a future import/export or wearable-calories integration, but risky as a primary nutrition backend.

### Open Food Facts

Open Food Facts is open data and supports product/barcode lookup, nutrition values, ingredients, product images, and write/contribution flows. It is very attractive for bootstrapping because read operations do not require auth, but the docs warn that data is voluntary and not guaranteed accurate or complete. The API also has low read limits for direct product queries, so production use should cache responses and consider periodic data exports.

Verdict: defer until after MVP. It can later prefill manual entries, but user-entered label data should remain authoritative for MVP.

### USDA FoodData Central

FoodData Central is official US nutrition data with a REST API and branded foods. It requires a data.gov API key and has a default rate limit of 1,000 requests/hour/IP. It is strong for canonical nutrient data and US packaged foods, but product coverage and barcode UX may still be incomplete compared with commercial APIs.

Verdict: defer until after MVP. Strong future source for validation, generic foods, and nutrition normalization.

### FatSecret

FatSecret positions itself as a commercial/global nutrition API with verified foods, restaurant foods, localization, food images, and very high barcode coverage. Its marketing claims more than 2.3 million unique foods/products and barcode success above 90% for Premier.

Verdict: likely best paid option later if automatic barcode lookup becomes central to the product.

### Edamam

Edamam Food Database API supports keyword, food-name, and UPC/barcode search, with macro/micro nutrients, allergy labels, diet labels, brand search, and NLP food logging.

Verdict: strong later option, especially if natural-language food entry or prefill matters.

### Nutritionix

Nutritionix Track API supports natural-language food parsing, autocomplete, UPC/item lookup for branded foods, and exercise parsing. It is practical for a polished food search/logging experience, but pricing and data licensing need validation before committing.

Verdict: good later candidate for text entry plus barcode lookup.

## AWS Architecture

Recommended MVP architecture:

- Web: React/Next.js hosted on Amplify Hosting, S3/CloudFront, or ECS depending on framework needs.
- Android: native Kotlin/Jetpack Compose or React Native if code sharing is important.
- Auth: Cognito User Pool with Google as an identity provider.
- API: API Gateway + Lambda for serverless MVP, or ECS Fargate if you prefer a long-running backend.
- OLTP database: DynamoDB for simple scale and user-owned diary records, or PostgreSQL/RDS/Aurora Serverless if relational querying and reporting are more important.
- Food catalog search: simple barcode and name indexes are enough for MVP; OpenSearch can wait.
- Storage: S3 for optional nutrition-label photos, product images, and event archives.
- Analytics/data lake: S3 raw/curated buckets, Glue Data Catalog, Athena queries, optional Lake Formation permissions.
- IaC: Terraform modules for networking, Cognito, API, database, S3, Glue/Lake Formation, IAM, and observability.

Data lake guidance:

- Use the data lake for raw scans, manual product creation/update events, diary events, and aggregate analytics.
- Do not use the data lake as the application database.
- Keep user PII separated from food/event telemetry where possible.
- Use lifecycle policies to control storage cost.

## Core Data Model

Application tables/entities:

- User: Cognito subject, email, display name, preferences, units, timezone.
- FoodProduct: product record, owner/scope, barcode/GTIN/UPC, brand, name, serving definitions, nutrients, source.
- ProductRevision: immutable history of user-entered nutrition changes.
- DiaryEntry: user, date, meal, food product, serving amount, calories/macros snapshot.
- BarcodeAlias: barcode to product mapping, owner/scope, status.
- ScanEvent: barcode, user, matched/missed, selected product, latency.

Important design choice: diary entries should snapshot nutrition values at the time of logging. If a product record changes later, historical logs should not silently change.

## Barcode Flow

1. Android scans barcode using ML Kit.
2. Client sends barcode to backend.
3. Backend checks the first-party catalog for that user and any shared/global curated products.
4. If found, the app shows the saved product and serving options.
5. If not found, the app opens a manual product form.
6. User enters product name, brand, serving size, calories, protein, carbs, fat, and optional micronutrients.
7. Backend saves the product and links it to the scanned barcode.
8. User logs a serving to the diary.
9. Scan, product-created, and diary events are written to S3/data lake.

## Risks

- Manual entry is slower on first scan, so the form must be fast and label-oriented.
- Barcode reuse determines perceived product quality after the first few sessions.
- User-entered data can be wrong; preserve revision history and allow corrections.
- Fitbit Web API migration creates integration risk through September 2026.
- Health-adjacent apps must be careful with privacy, consent, exports, deletion, and security even if not HIPAA-covered.
- User-entered nutrition must be marked as user-provided and ideally confidence-scored.

## MVP Scope

Phase 1:

- Web app and Android app skeleton
- Google sign-in via Cognito
- Food diary CRUD
- Manual food entry
- Barcode scan on Android
- Barcode-to-manual-food association
- Basic product search by name and barcode
- Terraform for dev AWS environment

Phase 2:

- Product correction workflow
- Search/autocomplete
- Data lake ingestion and Athena reporting
- Export/delete account flows

Phase 3:

- Open Food Facts + USDA prefill with backend caching
- Paid API bakeoff with FatSecret, Edamam, and Nutritionix
- Fitbit or Google Health integration
- Meal templates and favorites
- Nutrition goals
- Image/label capture to improve missing products
- Admin review tools for food catalog quality

## Sources

- Open Food Facts API: https://openfoodfacts.github.io/documentation/docs/Product-Opener/api/
- USDA FoodData Central API: https://fdc.nal.usda.gov/api-guide
- Fitbit Nutrition Web API: https://dev.fitbit.com/build/reference/web-api/nutrition/
- MyFitnessPal partner/API background: https://techcrunch.com/2012/10/16/myfitnesspal-api/
- MyFitnessPal current app integration categories: https://support.myfitnesspal.com/hc/en-us/articles/360032274232-What-kind-of-products-and-apps-work-with-MyFitnessPal
- FatSecret Platform API: https://platform.fatsecret.com/platform-api
- Edamam Food Database API: https://developer.edamam.com/food-database-api-docs
- Nutritionix v2 API: https://developer.nutritionix.com/docs/v2
- Amazon Cognito federated sign-in: https://docs.aws.amazon.com/cognito/latest/developerguide/cognito-user-pools-identity-federation.html
- AWS Lake Formation: https://docs.aws.amazon.com/lake-formation/latest/dg/what-is-lake-formation.html
- Google ML Kit barcode scanning: https://developers.google.cn/ml-kit/vision/barcode-scanning/android?hl=en
- Android Sign in with Google: https://developer.android.com/identity/sign-in/credential-manager-siwg?hl=en
