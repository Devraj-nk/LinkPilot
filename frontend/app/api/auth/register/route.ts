import { NextRequest, NextResponse } from "next/server";
import { BACKEND_URL, setAuthCookies } from "@/lib/backend";

export async function POST(request: NextRequest) {
  const body = await request.text();
  const backendResponse = await fetch(`${BACKEND_URL}/api/auth/register`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body,
    cache: "no-store",
  });

  const data = await backendResponse.json();
  if (!backendResponse.ok) {
    return NextResponse.json(data, { status: backendResponse.status });
  }

  const response = NextResponse.json({ user: data.user }, { status: backendResponse.status });
  setAuthCookies(response, { accessToken: data.accessToken, refreshToken: data.refreshToken });
  return response;
}
