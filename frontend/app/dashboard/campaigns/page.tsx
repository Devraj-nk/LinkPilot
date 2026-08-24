"use client";

import { useEffect, useState } from "react";
import { api, ApiError } from "@/lib/api-client";
import type { Campaign } from "@/lib/types";

export default function CampaignsPage() {
  const [campaigns, setCampaigns] = useState<Campaign[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [creating, setCreating] = useState(false);

  const load = async () => {
    try {
      setCampaigns(await api.get<Campaign[]>("/api/campaigns"));
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to load campaigns");
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
      await api.post("/api/campaigns", { name, description: description || null });
      setName("");
      setDescription("");
      await load();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to create campaign");
    } finally {
      setCreating(false);
    }
  };

  const handleDelete = async (id: string) => {
    if (!confirm("Delete this campaign? Links stay, but lose their campaign.")) return;
    try {
      await api.del(`/api/campaigns/${id}`);
      setCampaigns((prev) => prev.filter((c) => c.id !== id));
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to delete campaign");
    }
  };

  return (
    <div className="space-y-8">
      <div>
        <h1 className="text-2xl font-bold mb-4">Campaigns</h1>
        <form onSubmit={handleCreate} className="flex flex-wrap gap-3 items-end">
          <div className="min-w-[200px]">
            <label className="block text-sm font-medium mb-1">Name</label>
            <input
              type="text"
              value={name}
              onChange={(e) => setName(e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
              required
            />
          </div>
          <div className="flex-1 min-w-[220px]">
            <label className="block text-sm font-medium mb-1">Description (optional)</label>
            <input
              type="text"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>
          <button
            type="submit"
            disabled={creating}
            className="px-4 py-2 bg-blue-600 text-white font-medium rounded-md disabled:opacity-50 hover:!bg-blue-700 transition-colors"
          >
            {creating ? "Creating..." : "Create"}
          </button>
        </form>
        {error && <p className="mt-2 text-sm text-red-600">{error}</p>}
      </div>

      {loading ? (
        <p className="text-sm text-gray-500">Loading...</p>
      ) : campaigns.length === 0 ? (
        <p className="text-sm text-gray-500">No campaigns yet.</p>
      ) : (
        <ul className="divide-y divide-gray-100">
          {campaigns.map((campaign) => (
            <li key={campaign.id} className="py-3 flex items-center justify-between">
              <div>
                <p className="font-medium">{campaign.name}</p>
                {campaign.description && <p className="text-sm text-gray-500">{campaign.description}</p>}
                <p className="text-xs text-gray-400">{campaign.status}</p>
              </div>
              <button onClick={() => handleDelete(campaign.id)} className="text-sm text-red-600 hover:underline">
                Delete
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
