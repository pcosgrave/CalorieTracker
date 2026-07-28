using CalorieTracker.Api.Contracts;
using CalorieTracker.Api.Services.Foods;
using CalorieTracker.Api.Tests.Fakes.Foods;
using Xunit;

namespace CalorieTracker.Api.Tests.Services.Foods;

public sealed class FoodServiceTests
{
    [Fact]
    public async Task CreateFoodProductAsync_SavesProductAndBarcodeAlias()
    {
        var repository = new InMemoryFoodRepository();
        var service = new FoodService(repository);
        var request = CreateFoodRequest(barcode: "0123456789012", name: "Greek Yogurt", brand: "Test Brand");

        var product = await service.CreateFoodProductAsync("user-1", request, CancellationToken.None);
        var lookup = await service.LookupBarcodeAsync("user-1", "0123456789012", CancellationToken.None);

        Assert.Equal("user-1", product.OwnerUserId);
        Assert.Equal("Greek Yogurt", product.Name);
        Assert.True(lookup.Found);
        Assert.NotNull(lookup.Product);
        Assert.Equal(product.ProductId, lookup.Product!.ProductId);
    }

    [Fact]
    public async Task UpdateFoodProductAsync_ReplacesBarcodeAliasAndUpdatesFields()
    {
        var repository = new InMemoryFoodRepository();
        var service = new FoodService(repository);
        var created = await service.CreateFoodProductAsync(
            "user-1",
            CreateFoodRequest(barcode: "1111", name: "Oats", brand: "Original"),
            CancellationToken.None);

        var updated = await service.UpdateFoodProductAsync(
            "user-1",
            created.ProductId,
            new UpdateFoodProductRequest(
                Barcode: "2222",
                Name: "Steel Cut Oats",
                Brand: "Updated",
                Serving: new Serving("bowl", 1, "bowl", 80),
                Nutrients: new Nutrients(250, 10, 40, 4, 7, 1, 90)),
            CancellationToken.None);

        var oldLookup = await service.LookupBarcodeAsync("user-1", "1111", CancellationToken.None);
        var newLookup = await service.LookupBarcodeAsync("user-1", "2222", CancellationToken.None);

        Assert.Equal("Steel Cut Oats", updated.Name);
        Assert.Equal("Updated", updated.Brand);
        Assert.False(oldLookup.Found);
        Assert.True(newLookup.Found);
        Assert.Equal(updated.ProductId, newLookup.Product!.ProductId);
    }

    [Fact]
    public async Task SearchFoodProductsAsync_ReturnsMatchingProductsOnly()
    {
        var repository = new InMemoryFoodRepository();
        var service = new FoodService(repository);

        await service.CreateFoodProductAsync("user-1", CreateFoodRequest(name: "Chicken Breast"), CancellationToken.None);
        await service.CreateFoodProductAsync("user-1", CreateFoodRequest(name: "Chicken Wrap"), CancellationToken.None);
        await service.CreateFoodProductAsync("user-1", CreateFoodRequest(name: "Blueberry Muffin"), CancellationToken.None);

        var result = await service.SearchFoodProductsAsync("user-1", "chicken", CancellationToken.None);

        Assert.Equal(2, result.Products.Count);
        Assert.All(result.Products, product => Assert.Contains("Chicken", product.Name, StringComparison.OrdinalIgnoreCase));
    }

    [Fact]
    public async Task PublishCommunityFoodAsync_IsIdempotentForNormalizedNameAndBrand()
    {
        var repository = new InMemoryFoodRepository();
        var service = new FoodService(repository);
        var request = new PublishCommunityFoodRequest(
            ProductId: null,
            Barcode: "9999",
            Name: "  Pizza Slice  ",
            Brand: "Corner Shop",
            Serving: new Serving("slice", 1, "slice", 120),
            Nutrients: new Nutrients(285, 11, 31, 12, 2, 3, 540));

        var created = await service.PublishCommunityFoodAsync("user-1", request, CancellationToken.None);
        var second = await service.PublishCommunityFoodAsync("user-2", request with { Name = "Pizza Slice" }, CancellationToken.None);

        Assert.False(created.Existed);
        Assert.True(second.Existed);
        Assert.Equal(created.Product.ProductId, second.Product.ProductId);
    }

    [Fact]
    public async Task DeleteFoodProductAsync_RemovesStoredProductAndAlias()
    {
        var repository = new InMemoryFoodRepository();
        var service = new FoodService(repository);
        var created = await service.CreateFoodProductAsync(
            "user-1",
            CreateFoodRequest(barcode: "3333", name: "Protein Bar"),
            CancellationToken.None);

        await service.DeleteFoodProductAsync("user-1", created.ProductId, CancellationToken.None);

        var products = await service.ListFoodProductsAsync("user-1", CancellationToken.None);
        var lookup = await service.LookupBarcodeAsync("user-1", "3333", CancellationToken.None);

        Assert.Empty(products);
        Assert.False(lookup.Found);
    }

    private static CreateFoodProductRequest CreateFoodRequest(
        string? barcode = null,
        string name = "Food",
        string? brand = null) =>
        new(
            Barcode: barcode,
            Name: name,
            Brand: brand,
            Serving: new Serving("serving", 1, "serving", 100),
            Nutrients: new Nutrients(150, 10, 20, 5, 3, 4, 120));
}
