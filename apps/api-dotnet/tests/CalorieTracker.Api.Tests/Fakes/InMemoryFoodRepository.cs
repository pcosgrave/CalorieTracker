using CalorieTracker.Api.Contracts;
using CalorieTracker.Api.Services.Foods;
using System.Collections.Concurrent;

namespace CalorieTracker.Api.Tests.Fakes;

public sealed class InMemoryFoodRepository : IFoodRepository
{
    private readonly ConcurrentDictionary<string, FoodProduct> _products = new(StringComparer.Ordinal);
    private readonly ConcurrentDictionary<string, BarcodeAlias> _aliases = new(StringComparer.Ordinal);

    public Task<FoodProduct?> GetProductAsync(string ownerUserId, string productId, CancellationToken cancellationToken)
    {
        _products.TryGetValue(ToProductKey(ownerUserId, productId), out var product);
        return Task.FromResult(product);
    }

    public Task<IReadOnlyList<FoodProduct>> ListProductsAsync(string ownerUserId, CancellationToken cancellationToken)
    {
        var products = _products.Values
            .Where(product => string.Equals(product.OwnerUserId, ownerUserId, StringComparison.Ordinal))
            .OrderByDescending(product => product.UpdatedAt)
            .Take(100)
            .ToArray();

        return Task.FromResult<IReadOnlyList<FoodProduct>>(products);
    }

    public Task SaveProductAsync(FoodProduct product, CancellationToken cancellationToken)
    {
        _products[ToProductKey(product.OwnerUserId, product.ProductId)] = product;
        return Task.CompletedTask;
    }

    public Task DeleteProductAsync(string ownerUserId, string productId, CancellationToken cancellationToken)
    {
        _products.TryRemove(ToProductKey(ownerUserId, productId), out _);
        return Task.CompletedTask;
    }

    public Task<BarcodeAlias?> GetBarcodeAliasAsync(string ownerUserId, string barcode, CancellationToken cancellationToken)
    {
        _aliases.TryGetValue(ToAliasKey(ownerUserId, barcode), out var alias);
        return Task.FromResult(alias);
    }

    public Task SaveBarcodeAliasAsync(BarcodeAlias alias, CancellationToken cancellationToken)
    {
        _aliases[ToAliasKey(alias.OwnerUserId, alias.Barcode)] = alias;
        return Task.CompletedTask;
    }

    public Task DeleteBarcodeAliasAsync(string ownerUserId, string barcode, CancellationToken cancellationToken)
    {
        _aliases.TryRemove(ToAliasKey(ownerUserId, barcode), out _);
        return Task.CompletedTask;
    }

    private static string ToProductKey(string ownerUserId, string productId) => $"{ownerUserId}::{productId}";

    private static string ToAliasKey(string ownerUserId, string barcode) => $"{ownerUserId}::{barcode}";
}
