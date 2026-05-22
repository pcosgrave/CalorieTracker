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

export type Handler<TBody = unknown> = (
  request: AuthedRequest<TBody>,
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
      return await handler({
        event,
        userId: getUserId(event),
        body: parseJson(event, schema),
      });
    } catch (error) {
      if (error instanceof UnauthorizedError) {
        return json(401, { message: error.message });
      }
      if (error instanceof SyntaxError) {
        return json(400, { message: "Request body must be valid JSON" });
      }
      if (error instanceof ZodError) {
        return json(400, { message: "Request validation failed", issues: error.issues });
      }
      console.error(error);
      return json(500, { message: "Internal server error" });
    }
  };
}
