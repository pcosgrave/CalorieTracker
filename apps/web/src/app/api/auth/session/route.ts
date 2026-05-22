import { NextResponse } from "next/server";
import { applyAuthSession, requireBearerAccessToken } from "@/lib/auth/session";

export async function GET() {
  try {
    const { session, refreshed } = await requireBearerAccessToken();
    const response = NextResponse.json({
      authenticated: true,
      user: {
        userSub: session.userSub,
        email: session.email,
        name: session.name,
      },
    });
    if (refreshed) {
      applyAuthSession(response, session);
    }
    return response;
  } catch {
    return NextResponse.json({
      authenticated: false,
      user: null,
    });
  }
}
