using CalorieTracker.Api.Contracts;

namespace CalorieTracker.Api.Handlers.Foods;

internal static class FoodRequestValidator
{
    public static IResult? Validate(CreateFoodProductRequest request) =>
        ValidateProductRequest(request.Barcode, request.Name, request.Brand, request.Serving, request.Nutrients);

    public static IResult? Validate(UpdateFoodProductRequest request) =>
        ValidateProductRequest(request.Barcode, request.Name, request.Brand, request.Serving, request.Nutrients);

    public static IResult? Validate(PublishCommunityFoodRequest request) =>
        ValidateProductRequest(request.Barcode, request.Name, request.Brand, request.Serving, request.Nutrients);

    public static IResult? ValidateProductId(string productId) =>
        string.IsNullOrWhiteSpace(productId)
            ? Results.Json(new { message = "Product ID is required" }, statusCode: StatusCodes.Status400BadRequest)
            : null;

    public static IResult? ValidateBarcode(string barcode) =>
        string.IsNullOrWhiteSpace(barcode)
            ? Results.Json(new { message = "Barcode is required" }, statusCode: StatusCodes.Status400BadRequest)
            : null;

    private static IResult? ValidateProductRequest(
        string? barcode,
        string name,
        string? brand,
        Serving serving,
        Nutrients nutrients)
    {
        if (!string.IsNullOrWhiteSpace(barcode) && (barcode.Length < 4 || barcode.Length > 32))
        {
            return Results.ValidationProblem(new Dictionary<string, string[]>
            {
                ["barcode"] = ["Barcode must be between 4 and 32 characters."],
            });
        }

        if (string.IsNullOrWhiteSpace(name))
        {
            return Results.ValidationProblem(new Dictionary<string, string[]>
            {
                ["name"] = ["Name is required."],
            });
        }

        if (brand is not null && string.IsNullOrWhiteSpace(brand))
        {
            return Results.ValidationProblem(new Dictionary<string, string[]>
            {
                ["brand"] = ["Brand must not be empty when provided."],
            });
        }

        if (string.IsNullOrWhiteSpace(serving.Label) ||
            string.IsNullOrWhiteSpace(serving.Unit) ||
            serving.Quantity <= 0 ||
            serving.Grams is <= 0)
        {
            return Results.ValidationProblem(new Dictionary<string, string[]>
            {
                ["serving"] = ["Serving requires a label, positive quantity, unit, and positive grams when provided."],
            });
        }

        if (nutrients.Calories < 0 ||
            nutrients.ProteinGrams < 0 ||
            nutrients.CarbohydrateGrams < 0 ||
            nutrients.FatGrams < 0 ||
            nutrients.FiberGrams < 0 ||
            nutrients.SugarGrams < 0 ||
            nutrients.SodiumMilligrams < 0)
        {
            return Results.ValidationProblem(new Dictionary<string, string[]>
            {
                ["nutrients"] = ["Nutrient values must be non-negative."],
            });
        }

        return null;
    }
}
