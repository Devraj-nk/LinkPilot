import { NextRequest } from "next/server";
import { proxyToBackend } from "@/lib/backend";

export async function GET(request: NextRequest) {
  const params = request.nextUrl.searchParams;
  const forwarded = new URLSearchParams();
  for (const key of ["page", "size"]) {
    const value = params.get(key);
    if (value) forwarded.set(key, value);
  }
  const query = forwarded.size > 0 ? `?${forwarded.toString()}` : "";
  return proxyToBackend(request, `/api/domains${query}`);
}

export async function POST(request: NextRequest) {
  return proxyToBackend(request, "/api/domains", { method: "POST", body: await request.text() });
}
