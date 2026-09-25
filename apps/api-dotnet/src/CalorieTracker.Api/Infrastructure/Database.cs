using Npgsql;
using CalorieTracker.Api.Configuration;

namespace CalorieTracker.Api.Infrastructure;

public sealed class DatabaseConnectionFactory(DatabaseOptions options)
{
    public NpgsqlConnection Create() => new(options.ConnectionString ?? throw new InvalidOperationException("Database connection is not configured."));
}
