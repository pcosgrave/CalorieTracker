namespace CalorieTracker.Api.Services.Diary;

public sealed class DiaryEntryNotFoundException(string entryId) : Exception($"Diary entry not found: {entryId}")
{
    public string EntryId { get; } = entryId;
}
