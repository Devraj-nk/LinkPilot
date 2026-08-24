"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { api, ApiError } from "@/lib/api-client";
import type { Campaign, LinkItem } from "@/lib/types";

export default function DashboardPage() {
  const [links, setLinks] = useState<LinkItem[]>([]);
  const [campaigns, setCampaigns] = useState<Campaign[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const [originalUrl, setOriginalUrl] = useState("");
  const [title, setTitle] = useState("");
  const [campaignId, setCampaignId] = useState("");
  const [creating, setCreating] = useState(false);

  const load = async () => {
    try {
      const [linksData, campaignsData] = await Promise.all([
        api.get<LinkItem[]>("/api/links"),
        api.get<Campaign[]>("/api/campaigns"),
      ]);
      setLinks(linksData);
      setCampaigns(campaignsData);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to load links");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    // load()'s setState calls all happen after its `await`, in a microtask - not
    // synchronously during this render - but the rule can't see across the call boundary.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    load();
  }, []);

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");
    setCreating(true);
    try {
      await api.post("/api/links", {
        originalUrl,
        title: title || null,
        campaignId: campaignId || null,
      });
      setOriginalUrl("");
      setTitle("");
      setCampaignId("");
      await load();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to create link");
    } finally {
      setCreating(false);
    }
  };

  const handleDelete = async (id: string) => {
    if (!confirm("Delete this link?")) return;
    try {
      await api.del(`/api/links/${id}`);
      setLinks((prev) => prev.filter((l) => l.id !== id));
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to delete link");
    }
  };

  return (
    <div className="space-y-8">
      <div>
        <h1 className="text-2xl font-bold mb-4">Your links</h1>
        <form onSubmit={handleCreate} className="flex flex-wrap gap-3 items-end">
          <div className="flex-1 min-w-[220px]">
            <label className="block text-sm font-medium mb-1">URL to shorten</label>
            <input
              type="url"
              value={originalUrl}
              onChange={(e) => setOriginalUrl(e.target.value)}
              placeholder="https://example.com/very/long/url"
              className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
              required
            />
          </div>
          <div className="min-w-[160px]">
            <label className="block text-sm font-medium mb-1">Title (optional)</label>
            <input
              type="text"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>
          <div className="min-w-[160px]">
            <label className="block text-sm font-medium mb-1">Campaign (optional)</label>
            <select
              value={campaignId}
              onChange={(e) => setCampaignId(e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              <option value="">None</option>
              {campaigns.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.name}
                </option>
              ))}
            </select>
          </div>
          <button
            type="submit"
            disabled={creating}
            className="px-4 py-2 bg-blue-600 text-white font-medium rounded-md disabled:opacity-50 hover:!bg-blue-700 transition-colors"
          >
            {creating ? "Shortening..." : "Shorten"}
          </button>
        </form>
        {error && <p className="mt-2 text-sm text-red-600">{error}</p>}
      </div>

      {loading ? (
        <p className="text-sm text-gray-500">Loading...</p>
      ) : links.length === 0 ? (
        <p className="text-sm text-gray-500">No links yet - create your first one above.</p>
      ) : (
        <div className="overflow-x-auto">
          <table className="w-full text-sm border-collapse">
            <thead>
              <tr className="text-left border-b border-gray-200 text-gray-500">
                <th className="py-2 pr-4">Short link</th>
                <th className="py-2 pr-4">Original URL</th>
                <th className="py-2 pr-4">Status</th>
                <th className="py-2 pr-4">Clicks</th>
                <th className="py-2 pr-4"></th>
              </tr>
            </thead>
            <tbody>
              {links.map((link) => (
                <tr key={link.id} className="border-b border-gray-100">
                  <td className="py-2 pr-4">
                    <Link href={`/dashboard/links/${link.id}`} className="text-blue-600 hover:underline">
                      /{link.shortCode}
                    </Link>
                  </td>
                  <td className="py-2 pr-4 max-w-xs truncate" title={link.originalUrl}>
                    {link.originalUrl}
                  </td>
                  <td className="py-2 pr-4">{link.status}</td>
                  <td className="py-2 pr-4">{link.clickCount}</td>
                  <td className="py-2 pr-4">
                    <button onClick={() => handleDelete(link.id)} className="text-red-600 hover:underline">
                      Delete
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
