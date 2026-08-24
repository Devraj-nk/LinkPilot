import { NextRequest } from "next/server";
import { proxyToBackend } from "@/lib/backend";

export async function POST(request: NextRequest, { params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  return proxyToBackend(request, `/api/domains/${id}/verify`, { method: "POST" });
}
