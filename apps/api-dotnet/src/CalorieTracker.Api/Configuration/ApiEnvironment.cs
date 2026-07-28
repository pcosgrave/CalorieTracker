namespace CalorieTracker.Api.Configuration;

public static class ApiEnvironment
{
    public static readonly string[] RequiredVariableNames =
    [
        "PRODUCTS_TABLE_NAME",
        "BARCODE_ALIASES_TABLE_NAME",
        "DIARY_ENTRIES_TABLE_NAME",
        "WEIGHT_ENTRIES_TABLE_NAME",
        "SYNC_CHANGES_TABLE_NAME",
        "USER_POOL_ID",
    ];
}
