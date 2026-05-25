import crypto from "node:crypto";
import { NextRequest, NextResponse } from "next/server";
import { cognitoHostedUiBase, getCognitoConfig } from "@/lib/auth/config";
import { authStateCookie, pkceVerifierCookie, postAuthRedirectCookie } from "@/lib/auth/cookies";

function toBase64Url(buffer: Buffer): string {
  return buffer
    .toString("base64")
    .replace(/\+/g, "-")
    .replace(/\//g, "_")
    .replace(/=+$/u, "");
}

export async function GET(request: NextRequest) {
  const config = getCognitoConfig();
  const state = toBase64Url(crypto.randomBytes(24));
  const verifier = toBase64Url(crypto.randomBytes(32));
  const challenge = toBase64Url(crypto.createHash("sha256").update(verifier).digest());
  const returnTo = request.nextUrl.searchParams.get("returnTo") || "/";
  const provider = request.nextUrl.searchParams.get("provider");

  const authorizeUrl = new URL(`${cognitoHostedUiBase(config)}/oauth2/authorize`);
  authorizeUrl.searchParams.set("response_type", "code");
  authorizeUrl.searchParams.set("client_id", config.webClientId);
  authorizeUrl.searchParams.set("redirect_uri", config.webRedirectUri);
  authorizeUrl.searchParams.set("scope", "openid email profile");
  authorizeUrl.searchParams.set("code_challenge_method", "S256");
  authorizeUrl.searchParams.set("code_challenge", challenge);
  authorizeUrl.searchParams.set("state", state);
  if (provider) {
    authorizeUrl.searchParams.set("identity_provider", provider);
  }

  const response = NextResponse.redirect(authorizeUrl);
  response.cookies.set(authStateCookie, state, {
    httpOnly: true,
    sameSite: "lax",
    secure: config.webRedirectUri.startsWith("https://"),
    path: "/",
  });
  response.cookies.set(pkceVerifierCookie, verifier, {
    httpOnly: true,
    sameSite: "lax",
    secure: config.webRedirectUri.startsWith("https://"),
    path: "/",
  });
  response.cookies.set(postAuthRedirectCookie, returnTo, {
    httpOnly: true,
    sameSite: "lax",
    secure: config.webRedirectUri.startsWith("https://"),
    path: "/",
  });
  return response;
}
