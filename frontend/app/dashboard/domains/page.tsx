"use client";

import { useEffect, useState } from "react";
import { api, ApiError } from "@/lib/api-client";
import type { DomainItem } from "@/lib/types";

export default function DomainsPage() {
  const [domains, setDomains] = useState<DomainItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const [domain, setDomain] = useState("");
  const [creating, setCreating] = useState(false);

  const load = async () => {
    try {
      setDomains(await api.get<DomainItem[]>("/api/domains"));
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to load domains");
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
      await api.post("/api/domains", { domain });
      setDomain("");
      await load();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to add domain");
    } finally {
      setCreating(false);
    }
  };

  const handleVerify = async (id: string) => {
    try {
      await api.post(`/api/domains/${id}/verify`);
      await load();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to verify domain");
    }
  };

  const handleDelete = async (id: string) => {
    if (!confirm("Remove this domain?")) return;
    try {
      await api.del(`/api/domains/${id}`);
      setDomains((prev) => prev.filter((d) => d.id !== id));
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to delete domain");
    }
  };

  return (
    <div className="space-y-8">
      <div>
        <h1 className="text-2xl font-bold mb-2">Custom domains</h1>
        <p className="text-sm text-gray-500 mb-4">
          Note: verification here is a placeholder - it just flips the status, there&apos;s no real DNS check yet.
        </p>
        <form onSubmit={handleCreate} className="flex flex-wrap gap-3 items-end">
          <div className="min-w-[220px]">
            <label className="block text-sm font-medium mb-1">Domain</label>
            <input
              type="text"
              value={domain}
              onChange={(e) => setDomain(e.target.value)}
              placeholder="go.example.com"
              className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
              required
            />
          </div>
          <button
            type="submit"
            disabled={creating}
            className="px-4 py-2 bg-blue-600 text-white font-medium rounded-md disabled:opacity-50 hover:!bg-blue-700 transition-colors"
          >
            {creating ? "Adding..." : "Add domain"}
          </button>
        </form>
        {error && <p className="mt-2 text-sm text-red-600">{error}</p>}
      </div>

      {loading ? (
        <p className="text-sm text-gray-500">Loading...</p>
      ) : domains.length === 0 ? (
        <p className="text-sm text-gray-500">No domains yet.</p>
      ) : (
        <ul className="divide-y divide-gray-100">
          {domains.map((d) => (
            <li key={d.id} className="py-3 flex items-center justify-between">
              <div>
                <p className="font-medium">{d.domain}</p>
                <p className="text-xs text-gray-400">{d.verificationStatus}</p>
              </div>
              <div className="flex items-center gap-3">
                {d.verificationStatus !== "VERIFIED" && (
                  <button onClick={() => handleVerify(d.id)} className="text-sm text-blue-600 hover:underline">
                    Verify
                  </button>
                )}
                <button onClick={() => handleDelete(d.id)} className="text-sm text-red-600 hover:underline">
                  Delete
                </button>
              </div>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
