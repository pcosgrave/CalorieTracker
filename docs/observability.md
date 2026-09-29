# Application observability

All API requests emit structured JSON logs suitable for CloudWatch, Application Insights, or another log aggregator. Use these fields when searching:

- `service`, `timestamp`, `level`, `event`
- `requestId` and `correlationId` (forward `X-Correlation-Id` between clients and services)
- `route`, `method`, `statusCode`, and `durationMs`
- `errorName`, `errorMessage`, and `stack` for server-side failures only

Clients receive the correlation ID in `X-Correlation-Id` and in error response bodies. They should show the user-safe `message` and include the request ID when asking support for help. Sensitive request bodies, tokens, and personal nutrition data must not be logged.

Recommended alerts:

1. Alert on sustained 5xx rate, grouped by `service` and `route`.
2. Alert on elevated request latency using `durationMs` (p95 and p99).
3. Alert on repeated `request.validation_failed` or `request.unauthorized` events as an abuse signal, not as an application outage.
4. Keep health checks separate from business traffic and exclude them from user-facing error-rate dashboards.

Logs are intentionally event-oriented so queries remain stable when human-readable messages change. Example CloudWatch Logs Insights query:

```text
fields @timestamp, level, event, route, statusCode, durationMs, correlationId, requestId
| filter level = "error" or statusCode >= 500
| sort @timestamp desc
```
