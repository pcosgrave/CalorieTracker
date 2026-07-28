using CalorieTracker.Api.Contracts;

namespace CalorieTracker.Api.Services.Diary;

public interface IDiaryRepository
{
    Task<DiaryEntry?> GetEntryAsync(string ownerUserId, string entryId, CancellationToken cancellationToken);
    Task SaveEntryAsync(DiaryEntry entry, CancellationToken cancellationToken);
    Task DeleteEntryAsync(string ownerUserId, string entryId, CancellationToken cancellationToken);
}
