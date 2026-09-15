"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { api, ApiError } from "@/lib/api-client";
import type { LinkItem, QRCodeItem } from "@/lib/types";
import AnalyticsSection from "@/components/AnalyticsSection";

const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

export default function LinkDetailClient({ linkId }: { linkId: string }) {
  const [link, setLink] = useState<LinkItem | null>(null);
  const [qrCodes, setQrCodes] = useState<QRCodeItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [togglingStatus, setTogglingStatus] = useState(false);

  const [size, setSize] = useState(256);
  const [foregroundColor, setForegroundColor] = useState("#000000");
  const [backgroundColor, setBackgroundColor] = useState("#FFFFFF");
  const [generating, setGenerating] = useState(false);

  const load = async () => {
    try {
      const [linkData, qrData] = await Promise.all([
        api.get<LinkItem>(`/api/links/${linkId}`),
        api.get<QRCodeItem[]>(`/api/links/${linkId}/qrcodes`),
      ]);
      setLink(linkData);
      setQrCodes(qrData);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to load link");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    // load()'s setState calls all happen after its `await`, in a microtask - not
    // synchronously during this render - but the rule can't see across the call boundary.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [linkId]);

  const handleGenerate = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");
    setGenerating(true);
    try {
      await api.post(`/api/links/${linkId}/qrcodes`, {
        format: "PNG",
        foregroundColor,
        backgroundColor,
        size,
      });
      await load();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to generate QR code");
    } finally {
      setGenerating(false);
    }
  };

  const handleToggleStatus = async () => {
    if (!link) return;
    const nextStatus = link.status === "ACTIVE" ? "DISABLED" : "ACTIVE";
    setTogglingStatus(true);
    try {
      setLink(await api.patch<LinkItem>(`/api/links/${linkId}/status`, { status: nextStatus }));
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to update link status");
    } finally {
      setTogglingStatus(false);
    }
  };

  if (loading) {
    return <p className="text-sm text-gray-500">Loading...</p>;
  }

  if (!link) {
    return <p className="text-sm text-red-600">{error || "Link not found"}</p>;
  }

  return (
    <div className="space-y-8">
      <div>
        <Link href="/dashboard" className="text-sm text-blue-600 hover:underline">
          &larr; Back to links
        </Link>
        <h1 className="text-2xl font-bold mt-2">{link.shortUrl}</h1>
        <p className="text-gray-600 break-all">{link.originalUrl}</p>
        <dl className="mt-4 grid grid-cols-2 gap-4 text-sm max-w-md">
          <div>
            <dt className="text-gray-500">Status</dt>
            <dd className="flex items-center gap-2">
              <span
                className={
                  link.status === "ACTIVE"
                    ? "text-green-700"
                    : link.status === "DISABLED"
                      ? "text-gray-500"
                      : "text-red-600"
                }
              >
                {link.status}
              </span>
              {link.status !== "EXPIRED" && (
                <button
                  onClick={handleToggleStatus}
                  disabled={togglingStatus}
                  className="text-xs text-blue-600 hover:underline disabled:opacity-50"
                >
                  {link.status === "ACTIVE" ? "Disable" : "Enable"}
                </button>
              )}
            </dd>
          </div>
          <div>
            <dt className="text-gray-500">Clicks</dt>
            <dd>{link.clickCount}</dd>
          </div>
          <div>
            <dt className="text-gray-500">Created</dt>
            <dd>{new Date(link.createdAt).toLocaleString()}</dd>
          </div>
          <div>
            <dt className="text-gray-500">Expires</dt>
            <dd>{link.expiresAt ? new Date(link.expiresAt).toLocaleString() : "Never"}</dd>
          </div>
        </dl>
        {error && <p className="mt-2 text-sm text-red-600">{error}</p>}
      </div>

      <div>
        <h2 className="text-xl font-semibold mb-4">Analytics</h2>
        <AnalyticsSection linkId={linkId} />
      </div>

      <div>
        <h2 className="text-xl font-semibold mb-4">QR codes</h2>
        <form onSubmit={handleGenerate} className="flex flex-wrap gap-3 items-end mb-6">
          <div>
            <label className="block text-sm font-medium mb-1">Size (px)</label>
            <input
              type="number"
              min={64}
              max={1024}
              value={size}
              onChange={(e) => setSize(Number(e.target.value))}
              className="w-24 px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>
          <div>
            <label className="block text-sm font-medium mb-1">Foreground</label>
            <input
              type="color"
              value={foregroundColor}
              onChange={(e) => setForegroundColor(e.target.value)}
              className="w-16 h-10 border border-gray-300 rounded-md"
            />
          </div>
          <div>
            <label className="block text-sm font-medium mb-1">Background</label>
            <input
              type="color"
              value={backgroundColor}
              onChange={(e) => setBackgroundColor(e.target.value)}
              className="w-16 h-10 border border-gray-300 rounded-md"
            />
          </div>
          <button
            type="submit"
            disabled={generating}
            className="px-4 py-2 bg-blue-600 text-white font-medium rounded-md disabled:opacity-50 hover:!bg-blue-700 transition-colors"
          >
            {generating ? "Generating..." : "Generate QR code"}
          </button>
        </form>

        {qrCodes.length === 0 ? (
          <p className="text-sm text-gray-500">No QR codes yet.</p>
        ) : (
          <div className="flex flex-wrap gap-6">
            {qrCodes.map((qr) => (
              <div key={qr.id} className="text-center">
                {/* eslint-disable-next-line @next/next/no-img-element */}
                <img
                  src={`${API_BASE_URL}${qr.imageUrl}`}
                  alt={`QR code for ${link.shortUrl}`}
                  width={160}
                  height={160}
                  className="border border-gray-200 rounded-md"
                />
                <a
                  href={`${API_BASE_URL}${qr.imageUrl}`}
                  download={`${link.shortCode}-qr.png`}
                  className="mt-2 block text-sm text-blue-600 hover:underline"
                >
                  Download
                </a>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
