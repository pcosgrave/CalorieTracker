using Amazon.DynamoDBv2;
using Microsoft.Extensions.Options;
using CalorieTracker.Api.Services.Foods;

namespace CalorieTracker.Api.Configuration;

public static class ApiConfiguration
{
    public static IServiceCollection AddApiConfiguration(
        this IServiceCollection services,
        IConfiguration configuration)
    {
        services
            .AddOptions<StorageOptions>()
            .BindConfiguration(StorageOptions.SectionName)
            .PostConfigure(options =>
            {
                options.ProductsTableName ??= configuration["PRODUCTS_TABLE_NAME"];
                options.BarcodeAliasesTableName ??= configuration["BARCODE_ALIASES_TABLE_NAME"];
                options.DiaryEntriesTableName ??= configuration["DIARY_ENTRIES_TABLE_NAME"];
                options.WeightEntriesTableName ??= configuration["WEIGHT_ENTRIES_TABLE_NAME"];
                options.SyncChangesTableName ??= configuration["SYNC_CHANGES_TABLE_NAME"];
            })
            .ValidateDataAnnotations()
            .ValidateOnStart();

        services
            .AddOptions<CognitoOptions>()
            .BindConfiguration(CognitoOptions.SectionName)
            .PostConfigure(options =>
            {
                options.UserPoolId ??= configuration["USER_POOL_ID"];
            })
            .ValidateDataAnnotations()
            .ValidateOnStart();

        services
            .AddOptions<AiOptions>()
            .BindConfiguration(AiOptions.SectionName)
            .PostConfigure(options =>
            {
                options.GeminiApiSecretArn ??= configuration["GEMINI_API_SECRET_ARN"];
            });

        services.AddSingleton(sp => sp.GetRequiredService<IOptions<StorageOptions>>().Value);
        services.AddSingleton(sp => sp.GetRequiredService<IOptions<CognitoOptions>>().Value);
        services.AddSingleton(sp => sp.GetRequiredService<IOptions<AiOptions>>().Value);
        services.AddSingleton<IAmazonDynamoDB>(_ => new AmazonDynamoDBClient());
        services.AddSingleton<IFoodRepository, DynamoDbFoodRepository>();
        services.AddSingleton<FoodService>();

        return services;
    }
}
