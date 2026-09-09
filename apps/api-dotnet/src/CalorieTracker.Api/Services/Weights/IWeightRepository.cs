using CalorieTracker.Api.Contracts;

namespace CalorieTracker.Api.Services.Weights;

public interface IWeightRepository
{
    Task<WeightEntry?> GetEntryAsync(string ownerUserId, string entryId, CancellationToken cancellationToken);
    Task<IReadOnlyList<WeightEntry>> ListEntriesAsync(string ownerUserId, CancellationToken cancellationToken);
    Task SaveEntryAsync(WeightEntry entry, CancellationToken cancellationToken);
    Task DeleteEntryAsync(string ownerUserId, string entryId, CancellationToken cancellationToken);
}
