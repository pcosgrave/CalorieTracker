namespace CalorieTracker.Api.Services.Weights;

public sealed class WeightEntryNotFoundException(string entryId) : Exception($"Weight entry not found: {entryId}")
{
    public string EntryId { get; } = entryId;
}
