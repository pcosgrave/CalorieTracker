# Phase One Decisions

Date: 2026-05-20

## Confirmed

- AWS region: `us-east-1`
- Primary cloud database: DynamoDB
- Food entries: manually entered by the user
- Barcode mappings: private per user for MVP
- Cloud sync: build now so the owner can iterate as a paid user
- Product direction: keep as much user data on-device as possible for free users; paid users may unlock sync/sharing later

## Stack Recommendation

Use TypeScript for the web app and backend.

Recommended layout:

- Web app: Next.js / React / TypeScript
- Backend API: TypeScript Lambda handlers behind API Gateway
- Android app: Kotlin / Jetpack Compose
- Infrastructure: Terraform
- Cloud auth: Amazon Cognito with Google sign-in
- Cloud storage: DynamoDB
- Local Android storage: Room or SQLDelight later, with a sync boundary around user-owned data

## Python FastAPI vs TypeScript Backend

### TypeScript Backend

Benefits:

- Same language as the web app.
- Easier to share request/response types between web and API.
- Fits AWS Lambda very naturally for a serverless MVP.
- Smaller team/context load because frontend and backend use one ecosystem.
- Good fit for API Gateway, DynamoDB, Cognito JWT validation, and Terraform-driven AWS apps.

Risks:

- TypeScript runtime validation still needs discipline; static types do not validate incoming JSON by themselves.
- Complex data processing can feel less ergonomic than Python.
- Dependency churn in the JavaScript ecosystem can be noisy.

Mitigation:

- Use a schema library such as Zod for API input/output validation.
- Keep Lambda handlers thin and domain logic testable.

### Python FastAPI

Benefits:

- Very productive API framework.
- Strong request validation with Pydantic.
- Excellent if future features include data science, AI, OCR pipelines, recommendation models, or nutrition analytics.
- Developer ergonomics are clean and readable.

Risks:

- Web app and backend types are separate unless you generate clients/types.
- Running FastAPI usually points toward containers, Lambda adapters, or a long-running service.
- Slightly more moving parts for an AWS serverless MVP.

Mitigation:

- Use OpenAPI generation and typed clients if choosing Python.
- Deploy on ECS/Fargate or Lambda with a clear adapter strategy.

Recommendation:

Choose TypeScript for Phase 1. It keeps the MVP simpler and makes shared API contracts easier. Python can still be introduced later for analytics, enrichment, OCR, or recommendation services.

## Local-First Product Direction

The app should be designed with three data modes:

1. Anonymous/local-only mode: foods, barcodes, and diary entries stay on the device.
2. Signed-in private sync mode: data syncs privately to the user's cloud account.
3. Future paid/shared mode: users can choose to publish selected food/barcode records to a shared catalog.

For now, implement mode 2 first so development can exercise the cloud path. Keep the data model compatible with mode 1 and mode 3:

- Every food product has an `ownerUserId`.
- Every barcode alias has an `ownerUserId`.
- Add a `visibility` field with initial value `private`.
- Future values can be `shared` or `global`.
- Diary entries are always private.

## Working App Identity

Use temporary placeholders until the final name is chosen:

- Project name: `CalorieTracker`
- Android package: `com.philipcosgrave.calorietracker`
- AWS environment: `dev`

## Naming Criteria

A good name should be:

- Easy to say out loud.
- Not too diet-culture heavy.
- Comfortable for a daily utility.
- Broad enough to support meals, macros, barcodes, and sync later.
- Distinct enough to search for.

## Name Ideas

Product names:

- LabelLog
- MacroLedger
- Bitebook
- PantryCal
- ScanMeal
- NourishLog
- CalorieKit
- Mealmark
- DailyPlate
- Intake
- Foodprint
- Platewise

Company names:

- Labelwise Labs
- Platewise Software
- Daily Intake Labs
- Quiet Plate Software
- Foodprint Labs
- Macro Ledger Labs
- Nourish Tools
- Small Plate Software

Current favorite pairing:

- Product: Platewise
- Company: Labelwise Labs

Rationale: `Platewise` feels like a practical food app without sounding clinical, and `Labelwise Labs` points at the manual nutrition-label/barcode workflow without boxing the product into calories only.
