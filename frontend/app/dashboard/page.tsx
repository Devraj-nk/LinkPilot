"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { api, ApiError } from "@/lib/api-client";
import type { Campaign, DomainItem, LinkItem, PageResponse } from "@/lib/types";

const PAGE_SIZE = 20;
// Dropdowns want "all of a user's campaigns/domains" rather than one page of them -
// this is the API's own max page size (see PageRequestFactory on the backend), so it's
// still a bounded request, just a generous one for a list that's meant to be complete.
const DROPDOWN_SIZE = 100;

export default function DashboardPage() {
  const [links, setLinks] = useState<LinkItem[]>([]);
  const [linkPage, setLinkPage] = useState(0);
  const [linkTotalPages, setLinkTotalPages] = useState(0);
  const [linkTotalElements, setLinkTotalElements] = useState(0);
  const [campaigns, setCampaigns] = useState<Campaign[]>([]);
  const [domains, setDomains] = useState<DomainItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const [originalUrl, setOriginalUrl] = useState("");
  const [title, setTitle] = useState("");
  const [campaignId, setCampaignId] = useState("");
  const [domainId, setDomainId] = useState("");
  const [creating, setCreating] = useState(false);
  const [togglingId, setTogglingId] = useState<string | null>(null);

  const verifiedDomains = domains.filter((d) => d.verificationStatus === "VERIFIED");

  const load = async (page: number) => {
    try {
      const [linksData, campaignsData, domainsData] = await Promise.all([
        api.get<PageResponse<LinkItem>>(`/api/links?page=${page}&size=${PAGE_SIZE}`),
        api.get<PageResponse<Campaign>>(`/api/campaigns?size=${DROPDOWN_SIZE}`),
        api.get<PageResponse<DomainItem>>(`/api/domains?size=${DROPDOWN_SIZE}`),
      ]);
      setLinks(linksData.content);
      setLinkPage(linksData.page);
      setLinkTotalPages(linksData.totalPages);
      setLinkTotalElements(linksData.totalElements);
      setCampaigns(campaignsData.content);
      setDomains(domainsData.content);
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
    load(0);
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
        domainId: domainId || null,
      });
      setOriginalUrl("");
      setTitle("");
      setCampaignId("");
      setDomainId("");
      // A new link sorts first (newest-first), so jump back to page 0 to see it.
      await load(0);
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
      // Reload the current page rather than just filtering locally - deleting the
      // last row on a page should pull the next page's item up, not leave a gap.
      const nextPage = links.length === 1 && linkPage > 0 ? linkPage - 1 : linkPage;
      await load(nextPage);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to delete link");
    }
  };

  const handleToggleStatus = async (link: LinkItem) => {
    const nextStatus = link.status === "ACTIVE" ? "DISABLED" : "ACTIVE";
    setTogglingId(link.id);
    try {
      const updated = await api.patch<LinkItem>(`/api/links/${link.id}/status`, { status: nextStatus });
      setLinks((prev) => prev.map((l) => (l.id === link.id ? updated : l)));
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to update link status");
    } finally {
      setTogglingId(null);
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
          <div className="min-w-[160px]">
            <label className="block text-sm font-medium mb-1">Domain (optional)</label>
            <select
              value={domainId}
              onChange={(e) => setDomainId(e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              <option value="">Default</option>
              {verifiedDomains.map((d) => (
                <option key={d.id} value={d.id}>
                  {d.domain}
                </option>
              ))}
            </select>
            {domains.length > 0 && verifiedDomains.length === 0 && (
              <p className="mt-1 text-xs text-gray-500">
                No verified domains yet - see the Domains tab.
              </p>
            )}
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
                  <td className="py-2 pr-4 whitespace-nowrap">
                    <Link href={`/dashboard/links/${link.id}`} className="text-blue-600 hover:underline">
                      /{link.shortCode}
                    </Link>
                    <a
                      href={link.shortUrl}
                      target="_blank"
                      rel="noopener noreferrer"
                      title={`Open ${link.shortUrl}`}
                      className="ml-2 text-gray-400 hover:text-blue-600"
                    >
                      &#8599;
                    </a>
                  </td>
                  <td className="py-2 pr-4 max-w-xs truncate" title={link.originalUrl}>
                    {link.originalUrl}
                  </td>
                  <td className="py-2 pr-4">
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
                  </td>
                  <td className="py-2 pr-4">{link.clickCount}</td>
                  <td className="py-2 pr-4 space-x-3 whitespace-nowrap">
                    {link.status !== "EXPIRED" && (
                      <button
                        onClick={() => handleToggleStatus(link)}
                        disabled={togglingId === link.id}
                        className="text-blue-600 hover:underline disabled:opacity-50"
                      >
                        {link.status === "ACTIVE" ? "Disable" : "Enable"}
                      </button>
                    )}
                    <button onClick={() => handleDelete(link.id)} className="text-red-600 hover:underline">
                      Delete
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          <div className="flex items-center justify-between mt-4 text-sm text-gray-500">
            <span>
              Page {linkPage + 1} of {Math.max(linkTotalPages, 1)} - {linkTotalElements} link
              {linkTotalElements === 1 ? "" : "s"} total
            </span>
            <div className="space-x-2">
              <button
                onClick={() => load(linkPage - 1)}
                disabled={linkPage === 0}
                className="px-3 py-1 border border-gray-300 rounded-md disabled:opacity-50"
              >
                Previous
              </button>
              <button
                onClick={() => load(linkPage + 1)}
                disabled={linkPage + 1 >= linkTotalPages}
                className="px-3 py-1 border border-gray-300 rounded-md disabled:opacity-50"
              >
                Next
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
