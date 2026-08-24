import { NextRequest, NextResponse } from "next/server";
import { BACKEND_URL } from "@/lib/backend";

// The backend does the actual click-tracking and final redirect to originalUrl;
// this just forwards the browser there so short links can be root-level (e.g. /abc123).
export async function GET(request: NextRequest, { params }: { params: Promise<{ shortCode: string }> }) {
  const { shortCode } = await params;
  return NextResponse.redirect(`${BACKEND_URL}/r/${encodeURIComponent(shortCode)}`, 307);
}
