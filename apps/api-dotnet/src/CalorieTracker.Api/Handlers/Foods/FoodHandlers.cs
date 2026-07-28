using CalorieTracker.Api.Contracts;
using CalorieTracker.Api.Endpoints;
using CalorieTracker.Api.Services.Foods;

namespace CalorieTracker.Api.Handlers.Foods;

internal static class FoodHandlers
{
    public static async Task<IResult> ListFoodsAsync(
        HttpContext context,
        FoodService service,
        CancellationToken cancellationToken)
    {
        if (!UserIdentity.TryRequireUserId(context, out var userId, out var unauthorizedResult))
        {
            return unauthorizedResult!;
        }

        var products = await service.ListFoodProductsAsync(userId, cancellationToken);
        return Results.Ok(new { products });
    }

    public static async Task<IResult> CreateFoodAsync(
        HttpContext context,
        CreateFoodProductRequest request,
        FoodService service,
        CancellationToken cancellationToken)
    {
        if (!UserIdentity.TryRequireUserId(context, out var userId, out var unauthorizedResult))
        {
            return unauthorizedResult!;
        }

        var validationError = FoodRequestValidator.Validate(request);
        if (validationError is not null)
        {
            return validationError;
        }

        var product = await service.CreateFoodProductAsync(userId, request, cancellationToken);
        return Results.Json(new { product }, statusCode: StatusCodes.Status201Created);
    }

    public static async Task<IResult> UpdateFoodAsync(
        HttpContext context,
        string productId,
        UpdateFoodProductRequest request,
        FoodService service,
        CancellationToken cancellationToken)
    {
        if (!UserIdentity.TryRequireUserId(context, out var userId, out var unauthorizedResult))
        {
            return unauthorizedResult!;
        }

        var productIdError = FoodRequestValidator.ValidateProductId(productId);
        if (productIdError is not null)
        {
            return productIdError;
        }

        var validationError = FoodRequestValidator.Validate(request);
        if (validationError is not null)
        {
            return validationError;
        }

        try
        {
            var product = await service.UpdateFoodProductAsync(userId, productId, request, cancellationToken);
            return Results.Ok(new { product });
        }
        catch (FoodProductNotFoundException)
        {
            return Results.Json(new { message = "Product not found" }, statusCode: StatusCodes.Status404NotFound);
        }
    }

    public static async Task<IResult> DeleteFoodAsync(
        HttpContext context,
        string productId,
        FoodService service,
        CancellationToken cancellationToken)
    {
        if (!UserIdentity.TryRequireUserId(context, out var userId, out var unauthorizedResult))
        {
            return unauthorizedResult!;
        }

        var productIdError = FoodRequestValidator.ValidateProductId(productId);
        if (productIdError is not null)
        {
            return productIdError;
        }

        await service.DeleteFoodProductAsync(userId, productId, cancellationToken);
        return Results.Json(new { }, statusCode: StatusCodes.Status204NoContent);
    }

    public static async Task<IResult> LookupBarcodeAsync(
        HttpContext context,
        string barcode,
        FoodService service,
        CancellationToken cancellationToken)
    {
        if (!UserIdentity.TryRequireUserId(context, out var userId, out var unauthorizedResult))
        {
            return unauthorizedResult!;
        }

        var barcodeError = FoodRequestValidator.ValidateBarcode(barcode);
        if (barcodeError is not null)
        {
            return barcodeError;
        }

        var result = await service.LookupBarcodeAsync(userId, barcode, cancellationToken);
        return Results.Ok(result);
    }

    public static async Task<IResult> SearchFoodsAsync(
        HttpContext context,
        string? query,
        FoodService service,
        CancellationToken cancellationToken)
    {
        if (!UserIdentity.TryRequireUserId(context, out var userId, out var unauthorizedResult))
        {
            return unauthorizedResult!;
        }

        var result = await service.SearchFoodProductsAsync(userId, query ?? string.Empty, cancellationToken);
        return Results.Ok(result);
    }

    public static async Task<IResult> LookupCommunityBarcodeAsync(
        string barcode,
        FoodService service,
        CancellationToken cancellationToken)
    {
        var barcodeError = FoodRequestValidator.ValidateBarcode(barcode);
        if (barcodeError is not null)
        {
            return barcodeError;
        }

        var result = await service.LookupCommunityBarcodeAsync(barcode, cancellationToken);
        return Results.Ok(result);
    }

    public static async Task<IResult> SearchCommunityFoodsAsync(
        string? query,
        FoodService service,
        CancellationToken cancellationToken)
    {
        var result = await service.SearchCommunityFoodProductsAsync(query ?? string.Empty, cancellationToken);
        return Results.Ok(result);
    }

    public static async Task<IResult> PublishCommunityFoodAsync(
        HttpContext context,
        PublishCommunityFoodRequest request,
        FoodService service,
        CancellationToken cancellationToken)
    {
        var validationError = FoodRequestValidator.Validate(request);
        if (validationError is not null)
        {
            return validationError;
        }

        var userId = UserIdentity.TryGetUserId(context);
        var result = await service.PublishCommunityFoodAsync(userId, request, cancellationToken);
        return Results.Json(result, statusCode: result.Existed ? StatusCodes.Status200OK : StatusCodes.Status201Created);
    }
}
