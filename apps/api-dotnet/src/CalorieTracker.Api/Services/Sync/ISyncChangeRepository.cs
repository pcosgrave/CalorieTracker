using CalorieTracker.Api.Contracts;

namespace CalorieTracker.Api.Services.Sync;

public interface ISyncChangeRepository
{
    Task AppendChangeAsync(string ownerUserId, string changeKey, SyncChange change, CancellationToken cancellationToken);
    Task<IReadOnlyList<SyncChange>> ListChangesAfterAsync(string ownerUserId, string changeKey, CancellationToken cancellationToken);
}
