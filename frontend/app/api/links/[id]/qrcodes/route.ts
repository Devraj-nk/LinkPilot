import { NextRequest } from "next/server";
import { proxyToBackend } from "@/lib/backend";

export async function GET(request: NextRequest, { params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  return proxyToBackend(request, `/api/links/${id}/qrcodes`);
}

export async function POST(request: NextRequest, { params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  return proxyToBackend(request, `/api/links/${id}/qrcodes`, { method: "POST", body: await request.text() });
}
