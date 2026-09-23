# .NET API Foundation

This workspace contains the Phase 1 C#/.NET foundation for the CalorieTracker backend migration.

## Goal

Establish a parallel backend implementation that mirrors the current route surface without changing production behavior yet.

Current status:

- ASP.NET Core `net8.0` minimal API project
- route groups for `foods`, `diary`, `weights`, `sync`, `ai`, and `account`
- contract models shaped after the existing TypeScript API
- environment-backed configuration placeholders for DynamoDB, Cognito, and AI secret access

## Structure

- `CalorieTracker.Api.sln` - solution container for the .NET backend
- `src/CalorieTracker.Api` - API host project
- `tests/CalorieTracker.Api.Tests` - test project using `InMemoryFoodRepository` for fast service-level coverage
- `src/CalorieTracker.Api/Contracts` - request/response and entity contracts used for parity work
- `src/CalorieTracker.Api/Endpoints` - route mapping grouped by capability
- `src/CalorieTracker.Api/Configuration` - options and environment binding

## Repository Pattern Rule

- Production repositories live only under `src/CalorieTracker.Api`.
- In-memory and fake repository implementations live only under `tests/CalorieTracker.Api.Tests/Fakes`.
- As `diary`, `weights`, and `sync` migrate, they should follow the same split as `foods`.

## Phase 1 Exit Criteria

- the .NET backend builds locally
- the route surface mirrors the current TypeScript backend
- configuration seams exist for Cognito, DynamoDB, and AI integration
- no client cutover yet

## Local build

From `apps/api-dotnet`:

```powershell
dotnet restore .\CalorieTracker.Api.sln --configfile .\NuGet.Config
dotnet build .\CalorieTracker.Api.sln --no-restore
dotnet test .\CalorieTracker.Api.sln --no-build
```

## PostgreSQL and households

The first `/v1/households` slice requires `DATABASE_CONNECTION_STRING` for local development,
or `DATABASE_SECRET_ARN` in AWS. Apply `database/001_initial_schema.sql` before starting the
API. Cognito JWT validation is enabled for `/v1` routes; the legacy `X-User-Id` development
bypass is intentionally not supported.

Household reads are membership-scoped and household creation inserts the user profile,
settings row, household, and owner membership in one PostgreSQL transaction. Private user
records remain user-scoped and are not exposed by household membership.
