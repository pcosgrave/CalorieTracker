import type {
  APIGatewayProxyEventV2WithJWTAuthorizer,
  APIGatewayProxyResultV2,
} from "aws-lambda";
import { ZodError, type ZodSchema } from "zod";

export interface AuthedRequest<TBody = unknown> {
  event: APIGatewayProxyEventV2WithJWTAuthorizer;
  userId: string;
  body: TBody;
}

export type Handler<TBody = unknown> = (
  request: AuthedRequest<TBody>,
) => Promise<APIGatewayProxyResultV2>;

export function json(statusCode: number, body: unknown): APIGatewayProxyResultV2 {
  return {
    statusCode,
    headers: {
      "content-type": "application/json",
    },
    body: JSON.stringify(body),
  };
}

export function getUserId(event: APIGatewayProxyEventV2WithJWTAuthorizer): string {
  const subject = event.requestContext.authorizer.jwt.claims.sub;
  if (typeof subject !== "string" || subject.length === 0) {
    throw new Error("Missing authenticated Cognito subject");
  }
  return subject;
}

export function parseJson<TBody>(
  event: APIGatewayProxyEventV2WithJWTAuthorizer,
  schema: ZodSchema<TBody>,
): TBody {
  const parsed = event.body ? JSON.parse(event.body) : {};
  return schema.parse(parsed);
}

export function route<TBody>(
  schema: ZodSchema<TBody>,
  handler: Handler<TBody>,
) {
  return async (event: APIGatewayProxyEventV2WithJWTAuthorizer): Promise<APIGatewayProxyResultV2> => {
    try {
      return await handler({
        event,
        userId: getUserId(event),
        body: parseJson(event, schema),
      });
    } catch (error) {
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
