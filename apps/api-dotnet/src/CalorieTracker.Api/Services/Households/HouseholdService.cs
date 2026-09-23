using Npgsql;
using NpgsqlTypes;

namespace CalorieTracker.Api.Services.Households;

public sealed class HouseholdService(NpgsqlDataSource dataSource)
{
    public async Task<HouseholdSummary> CreateAsync(string userId, string name, CancellationToken cancellationToken)
    {
        await using var connection = await dataSource.OpenConnectionAsync(cancellationToken);
        await using var transaction = await connection.BeginTransactionAsync(cancellationToken);
        var householdId = Guid.NewGuid();
        await using var command = new NpgsqlCommand("""
            INSERT INTO users (user_id) VALUES (@user_id) ON CONFLICT (user_id) DO NOTHING;
            INSERT INTO user_settings (user_id) VALUES (@user_id) ON CONFLICT (user_id) DO NOTHING;
            INSERT INTO households (household_id, name, created_by_user_id)
            VALUES (@id, @name, @user_id);
            INSERT INTO household_memberships (household_id, user_id, role)
            VALUES (@id, @user_id, 'owner');
            """, connection, transaction);
        command.Parameters.AddWithValue("id", householdId);
        command.Parameters.AddWithValue("name", NpgsqlDbType.Text, name.Trim());
        command.Parameters.AddWithValue("user_id", NpgsqlDbType.Text, userId);
        await command.ExecuteNonQueryAsync(cancellationToken);
        await transaction.CommitAsync(cancellationToken);
        return new HouseholdSummary(householdId, name.Trim(), "owner");
    }

    public async Task<IReadOnlyList<HouseholdSummary>> ListAsync(string userId, CancellationToken cancellationToken)
    {
        await using var command = dataSource.CreateCommand("""
            SELECT h.household_id, h.name, m.role
            FROM households h
            JOIN household_memberships m ON m.household_id = h.household_id
            WHERE m.user_id = $1 AND m.status = 'active'
            ORDER BY h.created_at DESC
            """);
        command.Parameters.AddWithValue(userId);
        await using var reader = await command.ExecuteReaderAsync(cancellationToken);
        var result = new List<HouseholdSummary>();
        while (await reader.ReadAsync(cancellationToken))
            result.Add(new HouseholdSummary(reader.GetGuid(0), reader.GetString(1), reader.GetString(2)));
        return result;
    }

    public async Task<HouseholdSummary?> GetAsync(string userId, Guid householdId, CancellationToken cancellationToken)
    {
        await using var command = dataSource.CreateCommand("""
            SELECT h.household_id, h.name, m.role
            FROM households h
            JOIN household_memberships m ON m.household_id = h.household_id
            WHERE h.household_id = $1 AND m.user_id = $2 AND m.status = 'active'
            """);
        command.Parameters.AddWithValue(householdId);
        command.Parameters.AddWithValue(userId);
        await using var reader = await command.ExecuteReaderAsync(cancellationToken);
        return await reader.ReadAsync(cancellationToken)
            ? new HouseholdSummary(reader.GetGuid(0), reader.GetString(1), reader.GetString(2))
            : null;
    }
}

public sealed record HouseholdSummary(Guid HouseholdId, string Name, string Role);
