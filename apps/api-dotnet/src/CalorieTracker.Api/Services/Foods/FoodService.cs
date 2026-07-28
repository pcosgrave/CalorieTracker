using CalorieTracker.Api.Contracts;

namespace CalorieTracker.Api.Services.Foods;

public sealed class FoodService(IFoodRepository repository)
{
    private const string CommunityOwnerUserId = "__community__";

    public async Task<FoodProduct> CreateFoodProductAsync(
        string userId,
        CreateFoodProductRequest request,
        CancellationToken cancellationToken)
    {
        var now = DateTimeOffset.UtcNow;
        var product = new FoodProduct(
            ProductId: Guid.NewGuid().ToString(),
            OwnerUserId: userId,
            Visibility: Visibility.Private,
            Barcode: request.Barcode,
            Name: request.Name.Trim(),
            Brand: NormalizeOptional(request.Brand),
            Serving: request.Serving,
            Nutrients: request.Nutrients,
            Frequency: null,
            BreakfastFrequency: null,
            LunchFrequency: null,
            DinnerFrequency: null,
            SnackFrequency: null,
            LastUsedAt: null,
            CreatedAt: now,
            UpdatedAt: now);

        await repository.SaveProductAsync(product, cancellationToken);

        if (!string.IsNullOrWhiteSpace(request.Barcode))
        {
            await repository.SaveBarcodeAliasAsync(
                new BarcodeAlias(
                    Barcode: request.Barcode.Trim(),
                    OwnerUserId: userId,
                    ProductId: product.ProductId,
                    Visibility: Visibility.Private,
                    CreatedAt: now),
                cancellationToken);
        }

        return product;
    }

    public async Task<FoodProduct> UpdateFoodProductAsync(
        string userId,
        string productId,
        UpdateFoodProductRequest request,
        CancellationToken cancellationToken)
    {
        var existing = await repository.GetProductAsync(userId, productId, cancellationToken);
        if (existing is null)
        {
            throw new FoodProductNotFoundException(productId);
        }

        var product = existing with
        {
            Barcode = request.Barcode,
            Name = request.Name.Trim(),
            Brand = NormalizeOptional(request.Brand),
            Serving = request.Serving,
            Nutrients = request.Nutrients,
            UpdatedAt = DateTimeOffset.UtcNow,
        };

        await repository.SaveProductAsync(product, cancellationToken);

        var existingBarcode = NormalizeOptional(existing.Barcode);
        var requestedBarcode = NormalizeOptional(request.Barcode);

        if (!string.IsNullOrWhiteSpace(existingBarcode) &&
            !string.Equals(existingBarcode, requestedBarcode, StringComparison.Ordinal))
        {
            await repository.DeleteBarcodeAliasAsync(userId, existingBarcode, cancellationToken);
        }

        if (!string.IsNullOrWhiteSpace(requestedBarcode))
        {
            await repository.SaveBarcodeAliasAsync(
                new BarcodeAlias(
                    Barcode: requestedBarcode,
                    OwnerUserId: userId,
                    ProductId: product.ProductId,
                    Visibility: existing.Visibility,
                    CreatedAt: existing.CreatedAt),
                cancellationToken);
        }

        return product;
    }

    public async Task DeleteFoodProductAsync(
        string userId,
        string productId,
        CancellationToken cancellationToken)
    {
        var existing = await repository.GetProductAsync(userId, productId, cancellationToken);
        if (existing is null)
        {
            return;
        }

        await repository.DeleteProductAsync(userId, productId, cancellationToken);

        var barcode = NormalizeOptional(existing.Barcode);
        if (!string.IsNullOrWhiteSpace(barcode))
        {
            await repository.DeleteBarcodeAliasAsync(userId, barcode, cancellationToken);
        }
    }

    public Task<IReadOnlyList<FoodProduct>> ListFoodProductsAsync(
        string userId,
        CancellationToken cancellationToken) =>
        repository.ListProductsAsync(userId, cancellationToken);

    public async Task<BarcodeLookupResponse> LookupBarcodeAsync(
        string userId,
        string barcode,
        CancellationToken cancellationToken)
    {
        var alias = await repository.GetBarcodeAliasAsync(userId, barcode.Trim(), cancellationToken);
        if (alias is null)
        {
            return new BarcodeLookupResponse(false, null);
        }

        var product = await repository.GetProductAsync(userId, alias.ProductId, cancellationToken);
        return product is null
            ? new BarcodeLookupResponse(false, null)
            : new BarcodeLookupResponse(true, product);
    }

    public async Task<FoodSearchResponse> SearchFoodProductsAsync(
        string userId,
        string query,
        CancellationToken cancellationToken)
    {
        var normalized = NormalizeSearchQuery(query);
        if (string.IsNullOrWhiteSpace(normalized))
        {
            return new FoodSearchResponse([]);
        }

        var products = await repository.ListProductsAsync(userId, cancellationToken);
        return new FoodSearchResponse(
            products
                .Where(product => MatchesProductQuery(product, normalized))
                .Take(25)
                .ToArray());
    }

    public Task<BarcodeLookupResponse> LookupCommunityBarcodeAsync(
        string barcode,
        CancellationToken cancellationToken) =>
        LookupBarcodeAsync(CommunityOwnerUserId, barcode, cancellationToken);

    public Task<FoodSearchResponse> SearchCommunityFoodProductsAsync(
        string query,
        CancellationToken cancellationToken) =>
        SearchFoodProductsAsync(CommunityOwnerUserId, query, cancellationToken);

    public async Task<PublishCommunityFoodResponse> PublishCommunityFoodAsync(
        string? uploadedByUserId,
        PublishCommunityFoodRequest request,
        CancellationToken cancellationToken)
    {
        var now = DateTimeOffset.UtcNow;
        var normalizedName = NormalizeCommunityKeyPart(request.Name);
        var normalizedBrand = NormalizeCommunityKeyPart(request.Brand ?? string.Empty);
        var productId = $"community:{normalizedName}:{(string.IsNullOrWhiteSpace(normalizedBrand) ? "unbranded" : normalizedBrand)}";
        var existing = await repository.GetProductAsync(CommunityOwnerUserId, productId, cancellationToken);

        if (existing is not null)
        {
            return new PublishCommunityFoodResponse(existing, true);
        }

        var product = new FoodProduct(
            ProductId: productId,
            OwnerUserId: CommunityOwnerUserId,
            Visibility: Visibility.Shared,
            Barcode: NormalizeOptional(request.Barcode),
            Name: request.Name.Trim(),
            Brand: NormalizeOptional(request.Brand),
            Serving: request.Serving,
            Nutrients: request.Nutrients,
            Frequency: null,
            BreakfastFrequency: null,
            LunchFrequency: null,
            DinnerFrequency: null,
            SnackFrequency: null,
            LastUsedAt: null,
            CreatedAt: now,
            UpdatedAt: now);

        await repository.SaveProductAsync(product, cancellationToken);

        if (!string.IsNullOrWhiteSpace(product.Barcode))
        {
            var existingAlias = await repository.GetBarcodeAliasAsync(CommunityOwnerUserId, product.Barcode, cancellationToken);
            if (existingAlias is null)
            {
                await repository.SaveBarcodeAliasAsync(
                    new BarcodeAlias(
                        Barcode: product.Barcode,
                        OwnerUserId: CommunityOwnerUserId,
                        ProductId: productId,
                        Visibility: Visibility.Shared,
                        CreatedAt: now),
                    cancellationToken);
            }
        }

        _ = uploadedByUserId;
        return new PublishCommunityFoodResponse(product, false);
    }

    private static string NormalizeSearchQuery(string query) =>
        query.Trim().ToLowerInvariant();

    private static bool MatchesProductQuery(FoodProduct product, string normalizedQuery)
    {
        var name = product.Name.ToLowerInvariant();
        var brand = (product.Brand ?? string.Empty).ToLowerInvariant();
        return name.Contains(normalizedQuery, StringComparison.Ordinal)
            || brand.Contains(normalizedQuery, StringComparison.Ordinal)
            || $"{name} {brand}".Contains(normalizedQuery, StringComparison.Ordinal);
    }

    private static string NormalizeCommunityKeyPart(string value) =>
        NormalizeCommunityKeyPartCore(value) is var normalized && normalized.Length > 80
            ? normalized[..80]
            : normalized;

    private static string NormalizeCommunityKeyPartCore(string value) =>
        string.Join(
            "-",
            value.Trim()
                .ToLowerInvariant()
                .Split(new[] { ' ', '\t', '\r', '\n', '-', '_' }, StringSplitOptions.RemoveEmptyEntries)
                .Select(part => new string(part.Where(char.IsLetterOrDigit).ToArray()))
                .Where(part => part.Length > 0))
        .Trim('-');

    private static string? NormalizeOptional(string? value)
    {
        if (string.IsNullOrWhiteSpace(value))
        {
            return null;
        }

        return value.Trim();
    }
}
