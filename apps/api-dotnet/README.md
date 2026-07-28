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
- `src/CalorieTracker.Api/Contracts` - request/response and entity contracts used for parity work
- `src/CalorieTracker.Api/Endpoints` - route mapping grouped by capability
- `src/CalorieTracker.Api/Configuration` - options and environment binding

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
```
