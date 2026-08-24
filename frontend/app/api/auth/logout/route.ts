import { NextRequest, NextResponse } from "next/server";
import { BACKEND_URL, clearAuthCookies } from "@/lib/backend";

export async function POST(request: NextRequest) {
  const refreshToken = request.cookies.get("refreshToken")?.value;
  if (refreshToken) {
    await fetch(`${BACKEND_URL}/api/auth/logout`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ refreshToken }),
      cache: "no-store",
    }).catch(() => {
      // best-effort revoke; the cookies are cleared client-side regardless
    });
  }

  const response = NextResponse.json({ success: true });
  clearAuthCookies(response);
  return response;
}
