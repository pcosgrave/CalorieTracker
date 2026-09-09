using CalorieTracker.Api.Contracts;
using CalorieTracker.Api.Services.Foods;

namespace CalorieTracker.Api.Services.Diary;

public sealed class DiaryService(
    IDiaryRepository diaryRepository,
    IFoodRepository foodRepository)
{
    public async Task<DiaryEntry> CreateDiaryEntryAsync(
        string userId,
        CreateDiaryEntryRequest request,
        CancellationToken cancellationToken)
    {
        var product = await foodRepository.GetProductAsync(userId, request.ProductId, cancellationToken);
        if (product is null)
        {
            throw new FoodProductNotFoundException(request.ProductId);
        }

        var now = DateTimeOffset.UtcNow;
        var entry = new DiaryEntry(
            EntryId: Guid.NewGuid().ToString(),
            OwnerUserId: userId,
            ProductId: request.ProductId,
            LoggedAt: request.LoggedAt,
            Meal: request.Meal,
            ServingMultiplier: request.ServingMultiplier,
            LoggedAmount: request.LoggedAmount,
            LoggedUnit: request.LoggedUnit,
            ProductSnapshot: product,
            CreatedAt: now,
            UpdatedAt: now);

        await diaryRepository.SaveEntryAsync(entry, cancellationToken);
        return entry;
    }

    public async Task<DiaryEntry> UpdateDiaryEntryAsync(
        string userId,
        string entryId,
        UpdateDiaryEntryRequest request,
        CancellationToken cancellationToken)
    {
        var existing = await diaryRepository.GetEntryAsync(userId, entryId, cancellationToken);
        if (existing is null)
        {
            throw new DiaryEntryNotFoundException(entryId);
        }

        var product = await foodRepository.GetProductAsync(userId, request.ProductId, cancellationToken);
        if (product is null)
        {
            throw new FoodProductNotFoundException(request.ProductId);
        }

        var updated = existing with
        {
            ProductId = request.ProductId,
            LoggedAt = request.LoggedAt,
            Meal = request.Meal,
            ServingMultiplier = request.ServingMultiplier,
            LoggedAmount = request.LoggedAmount,
            LoggedUnit = request.LoggedUnit,
            ProductSnapshot = product,
            UpdatedAt = DateTimeOffset.UtcNow,
        };

        await diaryRepository.SaveEntryAsync(updated, cancellationToken);
        return updated;
    }

    public Task DeleteDiaryEntryAsync(string userId, string entryId, CancellationToken cancellationToken) =>
        diaryRepository.DeleteEntryAsync(userId, entryId, cancellationToken);
}
