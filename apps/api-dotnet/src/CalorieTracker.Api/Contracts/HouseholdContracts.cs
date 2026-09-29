namespace CalorieTracker.Api.Contracts;

public sealed record CreateHouseholdRequest(string Name);
public sealed record HouseholdResponse(Guid HouseholdId, string Name, string Role);
public sealed record HouseholdMemberResponse(string MemberId, string Name, string Role);
public sealed record AddHouseholdMemberRequest(string Email, string? Name);
public sealed record JoinHouseholdRequest(Guid HouseholdId, string? Name);
