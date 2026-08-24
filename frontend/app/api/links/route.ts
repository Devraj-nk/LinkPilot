import { NextRequest } from "next/server";
import { proxyToBackend } from "@/lib/backend";

export async function GET(request: NextRequest) {
  const campaignId = request.nextUrl.searchParams.get("campaignId");
  const query = campaignId ? `?campaignId=${encodeURIComponent(campaignId)}` : "";
  return proxyToBackend(request, `/api/links${query}`);
}

export async function POST(request: NextRequest) {
  return proxyToBackend(request, "/api/links", { method: "POST", body: await request.text() });
}
