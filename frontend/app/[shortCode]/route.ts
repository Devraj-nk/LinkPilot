import { NextRequest, NextResponse } from "next/server";
import { BACKEND_URL } from "@/lib/backend";

// The backend does the actual click-tracking and final redirect to originalUrl; this
// just forwards the browser there so short links can be root-level (e.g. /abc123).
//
// This is also the real entry point for a custom domain (go.example.com/abc123), so
// it's this request's Host header - not the backend's - that reflects what the
// visitor actually typed. By the time the browser follows the redirect below, its
// Host header will be this server's own address, not the original domain, so that
// has to be carried forward explicitly as a query param for the backend's
// domain-scoped redirect check to see the right host.
export async function GET(request: NextRequest, { params }: { params: Promise<{ shortCode: string }> }) {
  const { shortCode } = await params;
  const host = request.headers.get("host");
  const target = new URL(`${BACKEND_URL}/r/${encodeURIComponent(shortCode)}`);
  if (host) {
    target.searchParams.set("host", host);
  }
  return NextResponse.redirect(target.toString(), 307);
}
