import { cookies } from "next/headers";
import { NextResponse } from "next/server";
import {
  accessTokenCookie,
  accessTokenExpiryCookie,
  idTokenCookie,
  refreshTokenCookie,
  userEmailCookie,
  userNameCookie,
  userSubCookie,
} from "./cookies";
import { cognitoHostedUiBase, getCognitoConfig } from "./config";

export type AuthSession = {
  userSub: string;
  email?: string | undefined;
  name?: string | undefined;
  accessToken: string;
  idToken: string;
  refreshToken?: string | undefined;
  expiresAt: number;
};

type JwtPayload = {
  sub?: string;
  email?: string;
  name?: string;
  exp?: number;
};

function decodeJwtPayload(token: string): JwtPayload {
  const [, payload = ""] = token.split(".");
  const normalized = payload.replace(/-/g, "+").replace(/_/g, "/");
  const padding = "=".repeat((4 - (normalized.length % 4 || 4)) % 4);
  const json = Buffer.from(`${normalized}${padding}`, "base64").toString("utf8");
  return JSON.parse(json) as JwtPayload;
}

export async function readAuthSession(): Promise<AuthSession | null> {
  const cookieStore = await cookies();
  const accessToken = cookieStore.get(accessTokenCookie)?.value;
  const idToken = cookieStore.get(idTokenCookie)?.value;
  if (!accessToken || !idToken) {
    return null;
  }

  const expiresAt = Number(cookieStore.get(accessTokenExpiryCookie)?.value || "0");
  const userSub = cookieStore.get(userSubCookie)?.value || decodeJwtPayload(idToken).sub;
  if (!userSub) {
    return null;
  }

  return {
    userSub,
    email: cookieStore.get(userEmailCookie)?.value || decodeJwtPayload(idToken).email,
    name: cookieStore.get(userNameCookie)?.value || decodeJwtPayload(idToken).name,
    accessToken,
    idToken,
    refreshToken: cookieStore.get(refreshTokenCookie)?.value,
    expiresAt,
  };
}

export async function refreshAuthSession(): Promise<AuthSession | null> {
  const cookieStore = await cookies();
  const refreshToken = cookieStore.get(refreshTokenCookie)?.value;
  if (!refreshToken) {
    return null;
  }

  const config = getCognitoConfig();
  const body = new URLSearchParams({
    grant_type: "refresh_token",
    client_id: config.webClientId,
    refresh_token: refreshToken,
  });

  const response = await fetch(`${cognitoHostedUiBase(config)}/oauth2/token`, {
    method: "POST",
    headers: {
      "content-type": "application/x-www-form-urlencoded",
    },
    body,
    cache: "no-store",
  });

  if (!response.ok) {
    return null;
  }

  const tokens = (await response.json()) as {
    access_token: string;
    id_token?: string;
    expires_in: number;
  };

  const nextIdToken = tokens.id_token ?? cookieStore.get(idTokenCookie)?.value;
  if (!nextIdToken) {
    return null;
  }

  const payload = decodeJwtPayload(nextIdToken);
  if (!payload.sub) {
    return null;
  }

  const expiresAt = Date.now() + tokens.expires_in * 1000;
  return {
    userSub: payload.sub,
    email: payload.email,
    name: payload.name,
    accessToken: tokens.access_token,
    idToken: nextIdToken,
    refreshToken,
    expiresAt,
  };
}

export function applyAuthSession(response: NextResponse, session: AuthSession): void {
  const isSecure = getCognitoConfig().webRedirectUri.startsWith("https://");
  const cookieOptions = {
    httpOnly: true as const,
    sameSite: "lax" as const,
    secure: isSecure,
    path: "/",
  };

  response.cookies.set(accessTokenCookie, session.accessToken, cookieOptions);
  response.cookies.set(idTokenCookie, session.idToken, cookieOptions);
  if (session.refreshToken) {
    response.cookies.set(refreshTokenCookie, session.refreshToken, cookieOptions);
  }
  response.cookies.set(accessTokenExpiryCookie, String(session.expiresAt), cookieOptions);

  response.cookies.set(userSubCookie, session.userSub, {
    httpOnly: false,
    sameSite: "lax",
    secure: isSecure,
    path: "/",
  });
  if (session.email) {
    response.cookies.set(userEmailCookie, session.email, {
      httpOnly: false,
      sameSite: "lax",
      secure: isSecure,
      path: "/",
    });
  }
  if (session.name) {
    response.cookies.set(userNameCookie, session.name, {
      httpOnly: false,
      sameSite: "lax",
      secure: isSecure,
      path: "/",
    });
  }
}

export function clearAuthSession(response: NextResponse): void {
  for (const name of [
    accessTokenCookie,
    idTokenCookie,
    refreshTokenCookie,
    accessTokenExpiryCookie,
    userSubCookie,
    userEmailCookie,
    userNameCookie,
  ]) {
    response.cookies.set(name, "", {
      expires: new Date(0),
      path: "/",
    });
  }
}

export async function requireBearerAccessToken(): Promise<{ session: AuthSession; refreshed: boolean }> {
  const current = await readAuthSession();
  if (current && current.expiresAt > Date.now() + 30_000) {
    return { session: current, refreshed: false };
  }

  const refreshed = await refreshAuthSession();
  if (!refreshed) {
    throw new Error("Authentication required");
  }

  return { session: refreshed, refreshed: true };
}
