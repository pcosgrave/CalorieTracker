import type {
  APIGatewayProxyEvent,
  APIGatewayProxyEventV2WithJWTAuthorizer,
  APIGatewayProxyResult,
  APIGatewayProxyResultV2,
} from "aws-lambda";
import { ZodError, type ZodSchema } from "zod";

type SupportedGatewayEvent = APIGatewayProxyEvent | APIGatewayProxyEventV2WithJWTAuthorizer;
type SupportedGatewayResult = APIGatewayProxyResult | APIGatewayProxyResultV2;

export interface AuthedRequest<TBody = unknown> {
  event: SupportedGatewayEvent;
  userId: string;
  body: TBody;
}

export interface PublicRequest<TBody = unknown> {
  event: SupportedGatewayEvent;
  body: TBody;
}

type LogLevel = "info" | "warn" | "error";

function requestId(event: SupportedGatewayEvent): string {
  const headers = event.headers ?? {};
  return headers["x-correlation-id"] ?? headers["X-Correlation-Id"] ?? crypto.randomUUID();
}

function log(level: LogLevel, eventName: string, event: SupportedGatewayEvent, fields: Record<string, unknown> = {}, error?: unknown) {
  const record = {
    timestamp: new Date().toISOString(), level, service: "calorie-tracker-api",
    event: eventName, requestId: requestId(event), route: ("rawPath" in event ? event.rawPath : event.path) ?? "unknown",
    ...fields,
    ...(error instanceof Error ? { errorName: error.name, errorMessage: error.message, stack: error.stack } : {}),
  };
  (level === "error" ? console.error : level === "warn" ? console.warn : console.info)(JSON.stringify(record));
}

export type Handler<TBody = unknown> = (
  request: AuthedRequest<TBody>,
) => Promise<SupportedGatewayResult>;

export type PublicHandler<TBody = unknown> = (
  request: PublicRequest<TBody>,
) => Promise<SupportedGatewayResult>;

export function json(statusCode: number, body: unknown): SupportedGatewayResult {
  return {
    statusCode,
    headers: {
      "content-type": "application/json",
    },
    body: JSON.stringify(body),
  };
}

function withObservabilityHeaders(result: SupportedGatewayResult, event: SupportedGatewayEvent): SupportedGatewayResult {
  if (typeof result !== "object" || result === null) return result;
  return { ...result, headers: { ...(typeof result.headers === "object" ? result.headers : {}), "x-correlation-id": requestId(event) } } as SupportedGatewayResult;
}

function response(statusCode: number, message: string, event: SupportedGatewayEvent, extra: Record<string, unknown> = {}) {
  return json(statusCode, { message, requestId: requestId(event), ...extra });
}

class UnauthorizedError extends Error {}

export function getUserId(event: SupportedGatewayEvent): string {
  const v1Claims = "requestContext" in event ? (event.requestContext as APIGatewayProxyEvent["requestContext"]).authorizer?.claims : undefined;
  const v2Claims = "requestContext" in event ? (event.requestContext as APIGatewayProxyEventV2WithJWTAuthorizer["requestContext"]).authorizer?.jwt?.claims : undefined;
  const subject = v2Claims?.sub ?? v1Claims?.sub;
  if (typeof subject !== "string" || subject.length === 0) {
    throw new UnauthorizedError("Missing authenticated Cognito subject");
  }
  return subject;
}

export function parseJson<TBody>(
  event: SupportedGatewayEvent,
  schema: ZodSchema<TBody>,
): TBody {
  const parsed = event.body ? JSON.parse(event.body) : {};
  return schema.parse(parsed);
}

export function route<TBody>(
  schema: ZodSchema<TBody>,
  handler: Handler<TBody>,
) {
  return async (event: SupportedGatewayEvent): Promise<SupportedGatewayResult> => {
    try {
      return withObservabilityHeaders(await handler({
        event,
        userId: getUserId(event),
        body: parseJson(event, schema),
      }), event);
    } catch (error) {
      if (error instanceof UnauthorizedError) {
        log("warn", "request.unauthorized", event, { statusCode: 401 });
        return response(401, error.message, event);
      }
      if (error instanceof SyntaxError) {
        log("warn", "request.invalid_json", event, { statusCode: 400 });
        return response(400, "Request body must be valid JSON", event);
      }
      if (error instanceof ZodError) {
        log("warn", "request.validation_failed", event, { statusCode: 400, issueCount: error.issues.length });
        return response(400, "Request validation failed", event, { issues: error.issues });
      }
      log("error", "request.failed", event, { statusCode: 500 }, error);
      return response(500, "Internal server error", event);
    }
  };
}

export function publicRoute<TBody>(
  schema: ZodSchema<TBody>,
  handler: PublicHandler<TBody>,
) {
  return async (event: SupportedGatewayEvent): Promise<SupportedGatewayResult> => {
    try {
      return withObservabilityHeaders(await handler({
        event,
        body: parseJson(event, schema),
      }), event);
    } catch (error) {
      if (error instanceof SyntaxError) {
        log("warn", "request.invalid_json", event, { statusCode: 400 });
        return response(400, "Request body must be valid JSON", event);
      }
      if (error instanceof ZodError) {
        log("warn", "request.validation_failed", event, { statusCode: 400, issueCount: error.issues.length });
        return response(400, "Request validation failed", event, { issues: error.issues });
      }
      log("error", "request.failed", event, { statusCode: 500 }, error);
      return response(500, "Internal server error", event);
    }
  };
}
