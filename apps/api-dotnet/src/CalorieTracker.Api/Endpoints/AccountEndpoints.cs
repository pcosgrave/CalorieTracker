using CalorieTracker.Api.Handlers.Account;

namespace CalorieTracker.Api.Endpoints;

public static class AccountEndpoints
{
    public static IEndpointRouteBuilder MapAccountEndpoints(this IEndpointRouteBuilder app)
    {
        var account = app.MapGroup("/account").WithTags("Account");

        account.MapDelete("/", AccountHandlers.DeleteAsync);

        return app;
    }
}
