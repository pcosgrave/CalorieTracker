using CalorieTracker.Api.Contracts;

namespace CalorieTracker.Api.Services.Foods;

public interface IFoodRepository
{
    Task<FoodProduct?> GetProductAsync(string ownerUserId, string productId, CancellationToken cancellationToken);
    Task<IReadOnlyList<FoodProduct>> ListProductsAsync(string ownerUserId, CancellationToken cancellationToken);
    Task SaveProductAsync(FoodProduct product, CancellationToken cancellationToken);
    Task DeleteProductAsync(string ownerUserId, string productId, CancellationToken cancellationToken);
    Task<BarcodeAlias?> GetBarcodeAliasAsync(string ownerUserId, string barcode, CancellationToken cancellationToken);
    Task SaveBarcodeAliasAsync(BarcodeAlias alias, CancellationToken cancellationToken);
    Task DeleteBarcodeAliasAsync(string ownerUserId, string barcode, CancellationToken cancellationToken);
}
