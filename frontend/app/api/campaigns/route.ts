import { NextRequest } from "next/server";
import { proxyToBackend } from "@/lib/backend";

export async function GET(request: NextRequest) {
  return proxyToBackend(request, "/api/campaigns");
}

export async function POST(request: NextRequest) {
  return proxyToBackend(request, "/api/campaigns", { method: "POST", body: await request.text() });
}
