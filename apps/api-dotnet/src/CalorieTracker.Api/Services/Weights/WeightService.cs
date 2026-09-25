using CalorieTracker.Api.Contracts;

namespace CalorieTracker.Api.Services.Weights;

public sealed class WeightService(IWeightRepository repository)
{
    public async Task<WeightEntry> CreateWeightEntryAsync(
        string userId,
        CreateWeightEntryRequest request,
        CancellationToken cancellationToken)
    {
        var now = DateTimeOffset.UtcNow;
        var entry = new WeightEntry(
            EntryId: Guid.NewGuid().ToString(),
            OwnerUserId: userId,
            LoggedAt: request.LoggedAt,
            WeightKg: request.WeightKg,
            Source: request.Source ?? "manual",
            CreatedAt: now,
            UpdatedAt: now);

        await repository.SaveEntryAsync(entry, cancellationToken);
        return entry;
    }

    public Task<IReadOnlyList<WeightEntry>> ListWeightEntriesAsync(string userId, CancellationToken cancellationToken) =>
        repository.ListEntriesAsync(userId, cancellationToken);

    public async Task<WeightEntry> UpdateWeightEntryAsync(
        string userId,
        string entryId,
        UpdateWeightEntryRequest request,
        CancellationToken cancellationToken)
    {
        var existing = await repository.GetEntryAsync(userId, entryId, cancellationToken);
        if (existing is null)
        {
            throw new WeightEntryNotFoundException(entryId);
        }

        var updated = existing with
        {
            LoggedAt = request.LoggedAt,
            WeightKg = request.WeightKg,
            Source = request.Source ?? existing.Source,
            UpdatedAt = DateTimeOffset.UtcNow,
        };

        await repository.SaveEntryAsync(updated, cancellationToken);
        return updated;
    }

    public Task DeleteWeightEntryAsync(string userId, string entryId, CancellationToken cancellationToken) =>
        repository.DeleteEntryAsync(userId, entryId, cancellationToken);
}
