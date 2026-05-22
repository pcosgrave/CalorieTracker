import { NextRequest, NextResponse } from "next/server";
import { clearAuthSession } from "@/lib/auth/session";
import { cognitoHostedUiBase, getCognitoConfig } from "@/lib/auth/config";

export async function GET(request: NextRequest) {
  const config = getCognitoConfig();
  const logoutUrl = new URL(`${cognitoHostedUiBase(config)}/logout`);
  logoutUrl.searchParams.set("client_id", config.webClientId);
  logoutUrl.searchParams.set("logout_uri", config.webLogoutUri);

  const response = NextResponse.redirect(logoutUrl);
  clearAuthSession(response);
  return response;
}
