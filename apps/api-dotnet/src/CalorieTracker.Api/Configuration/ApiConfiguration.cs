using Amazon.DynamoDBv2;
using Amazon.CognitoIdentityProvider;
using Amazon.SecretsManager;
using CalorieTracker.Api.Services.Account;
using CalorieTracker.Api.Services.Ai;
using CalorieTracker.Api.Services.Diary;
using Microsoft.Extensions.Options;
using Npgsql;
using Microsoft.AspNetCore.Authentication.JwtBearer;
using System.Text.Json;
using CalorieTracker.Api.Services.Foods;
using CalorieTracker.Api.Services.Sync;
using CalorieTracker.Api.Services.Weights;

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
        services.AddSingleton<IAmazonCognitoIdentityProvider>(_ => new AmazonCognitoIdentityProviderClient());
        services.AddSingleton<IAmazonSecretsManager>(_ => new AmazonSecretsManagerClient());
        services.AddHttpClient<AiFoodLogService>();
        services.AddSingleton<IFoodRepository, DynamoDbFoodRepository>();
        services.AddSingleton<IDiaryRepository, DynamoDbDiaryRepository>();
        services.AddSingleton<IWeightRepository, DynamoDbWeightRepository>();
        services.AddSingleton<ISyncChangeRepository, DynamoDbSyncChangeRepository>();
        services.AddSingleton<FoodService>();
        services.AddSingleton<DiaryService>();
        services.AddSingleton<WeightService>();
        services.AddSingleton<SyncService>();
        services.AddSingleton<AccountService>();
        services.AddOptions<DatabaseOptions>()
            .BindConfiguration(DatabaseOptions.SectionName)
            .PostConfigure(options =>
            {
                options.ConnectionString ??= configuration["DATABASE_CONNECTION_STRING"];
                options.SecretArn ??= configuration["DATABASE_SECRET_ARN"];
            })
            .ValidateDataAnnotations()
            .Validate(options => !string.IsNullOrWhiteSpace(options.ConnectionString) || !string.IsNullOrWhiteSpace(options.SecretArn), "Database connection string or secret ARN is required.")
            .ValidateOnStart();

        services.AddAuthentication(JwtBearerDefaults.AuthenticationScheme)
            .AddJwtBearer(options =>
            {
                var region = configuration["AWS_REGION"] ?? configuration["AWS_DEFAULT_REGION"] ?? "us-east-1";
                var userPoolId = configuration["USER_POOL_ID"];
                options.Authority = $"https://cognito-idp.{region}.amazonaws.com/{userPoolId}";
                options.TokenValidationParameters.ValidateIssuer = true;
                options.TokenValidationParameters.ValidateLifetime = true;
                options.TokenValidationParameters.ValidateAudience = false;
            });
        services.AddAuthorization();
        services.AddSingleton(sp =>
        {
            var options = sp.GetRequiredService<IOptions<DatabaseOptions>>().Value;
            var connectionString = options.ConnectionString;
            if (string.IsNullOrWhiteSpace(connectionString) && !string.IsNullOrWhiteSpace(options.SecretArn))
            {
                var secret = sp.GetRequiredService<IAmazonSecretsManager>()
                    .GetSecretValueAsync(new Amazon.SecretsManager.Model.GetSecretValueRequest { SecretId = options.SecretArn })
                    .GetAwaiter().GetResult();
                connectionString = secret.SecretString;
                if (!string.IsNullOrWhiteSpace(connectionString) && connectionString.TrimStart().StartsWith("{"))
                    connectionString = JsonDocument.Parse(connectionString).RootElement.GetProperty("connectionString").GetString();
            }
            return NpgsqlDataSource.Create(connectionString!);
        });
        services.AddSingleton<Services.Households.HouseholdService>();

        return services;
    }
}
