using System.ComponentModel.DataAnnotations;

namespace CalorieTracker.Api.Configuration;

public sealed class StorageOptions
{
    public const string SectionName = "Storage";

    [Required]
    public string? ProductsTableName { get; set; }

    [Required]
    public string? BarcodeAliasesTableName { get; set; }

    [Required]
    public string? DiaryEntriesTableName { get; set; }

    [Required]
    public string? WeightEntriesTableName { get; set; }

    [Required]
    public string? SyncChangesTableName { get; set; }
}
