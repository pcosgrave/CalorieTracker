namespace CalorieTracker.Api.Contracts;

public sealed record CreateHouseholdRequest(string Name);
public sealed record HouseholdResponse(Guid HouseholdId, string Name, string Role);
