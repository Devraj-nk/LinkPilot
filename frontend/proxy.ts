import { NextRequest, NextResponse } from "next/server";

// Presence-only check: a signed-out browser is bounced to /login immediately.
// Actual token validity is enforced API-side (a stale/invalid token still gets
// a 401 from the backend, which the dashboard pages send back to /login).
export function proxy(request: NextRequest) {
  const hasSession = request.cookies.has("accessToken") || request.cookies.has("refreshToken");
  if (!hasSession) {
    const loginUrl = new URL("/login", request.url);
    loginUrl.searchParams.set("next", request.nextUrl.pathname);
    return NextResponse.redirect(loginUrl);
  }
  return NextResponse.next();
}

export const config = {
  matcher: ["/dashboard/:path*"],
};
