import { cookies } from "next/headers";
import { NextRequest, NextResponse } from "next/server";
import { cognitoHostedUiBase, getCognitoConfig } from "@/lib/auth/config";
import { applyAuthSession } from "@/lib/auth/session";
import {
  authStateCookie,
  pkceVerifierCookie,
  postAuthRedirectCookie,
} from "@/lib/auth/cookies";

type TokenResponse = {
  access_token: string;
  id_token: string;
  refresh_token?: string;
  expires_in: number;
};

function decodeJwtPayload(token: string): { sub?: string; email?: string; name?: string } {
  const [, payload = ""] = token.split(".");
  const normalized = payload.replace(/-/g, "+").replace(/_/g, "/");
  const padding = "=".repeat((4 - (normalized.length % 4 || 4)) % 4);
  const json = Buffer.from(`${normalized}${padding}`, "base64").toString("utf8");
  return JSON.parse(json) as { sub?: string; email?: string; name?: string };
}

export async function GET(request: NextRequest) {
  const code = request.nextUrl.searchParams.get("code");
  const state = request.nextUrl.searchParams.get("state");
  const config = getCognitoConfig();
  const cookieStore = await cookies();
  const expectedState = cookieStore.get(authStateCookie)?.value;
  const verifier = cookieStore.get(pkceVerifierCookie)?.value;
  const returnTo = cookieStore.get(postAuthRedirectCookie)?.value || "/";

  if (!code || !state || !expectedState || state !== expectedState || !verifier) {
    return NextResponse.redirect(new URL("/settings?auth=failed", request.url));
  }

  const body = new URLSearchParams({
    grant_type: "authorization_code",
    client_id: config.webClientId,
    code,
    code_verifier: verifier,
    redirect_uri: config.webRedirectUri,
  });

  const tokenResponse = await fetch(`${cognitoHostedUiBase(config)}/oauth2/token`, {
    method: "POST",
    headers: {
      "content-type": "application/x-www-form-urlencoded",
    },
    body,
    cache: "no-store",
  });

  if (!tokenResponse.ok) {
    return NextResponse.redirect(new URL("/settings?auth=failed", request.url));
  }

  const tokens = (await tokenResponse.json()) as TokenResponse;
  const payload = decodeJwtPayload(tokens.id_token);
  if (!payload.sub) {
    return NextResponse.redirect(new URL("/settings?auth=failed", request.url));
  }

  const response = NextResponse.redirect(new URL(returnTo, request.url));
  applyAuthSession(response, {
    userSub: payload.sub,
    email: payload.email,
    name: payload.name,
    accessToken: tokens.access_token,
    idToken: tokens.id_token,
    refreshToken: tokens.refresh_token,
    expiresAt: Date.now() + tokens.expires_in * 1000,
  });

  for (const name of [authStateCookie, pkceVerifierCookie, postAuthRedirectCookie]) {
    response.cookies.set(name, "", { expires: new Date(0), path: "/" });
  }

  return response;
}
