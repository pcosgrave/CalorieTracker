import { NextRequest, NextResponse } from "next/server";
import { applyAuthSession, requireBearerAccessToken } from "@/lib/auth/session";
import { getCognitoConfig } from "@/lib/auth/config";

export async function POST(request: NextRequest) {
  try {
    const apiBaseUrl = request.headers.get("x-sync-api-base-url") || getCognitoConfig().apiBaseUrl;
    const body = await request.text();
    const { session, refreshed } = await requireBearerAccessToken();
    const upstream = await fetch(`${apiBaseUrl.replace(/\/$/, "")}/sync/push`, {
      method: "POST",
      headers: {
        "content-type": "application/json",
        authorization: `Bearer ${session.accessToken}`,
      },
      body,
      cache: "no-store",
    });

    const response = new NextResponse(await upstream.text(), {
      status: upstream.status,
      headers: {
        "content-type": upstream.headers.get("content-type") || "application/json",
      },
    });
    if (refreshed) {
      applyAuthSession(response, session);
    }
    return response;
  } catch (error) {
    return NextResponse.json(
      { message: error instanceof Error ? error.message : "Authentication required" },
      { status: 401 },
    );
  }
}
