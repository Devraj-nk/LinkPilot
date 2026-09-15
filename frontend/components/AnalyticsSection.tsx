"use client";

import { useEffect, useState } from "react";
import { api, ApiError } from "@/lib/api-client";
import type { LinkAnalytics, NamedCount } from "@/lib/types";

function BarList({ items, emptyLabel }: { items: NamedCount[]; emptyLabel: string }) {
  if (items.length === 0) {
    return <p className="text-sm text-gray-500">{emptyLabel}</p>;
  }
  const max = Math.max(1, ...items.map((i) => i.count));
  return (
    <div className="space-y-2">
      {items.map((item) => (
        <div key={item.name} className="flex items-center gap-3 text-sm">
          <span className="w-28 shrink-0 truncate text-gray-600" title={item.name}>
            {item.name || "unknown"}
          </span>
          <div className="flex-1 h-4 bg-gray-100 rounded overflow-hidden">
            <div className="h-full bg-blue-500" style={{ width: `${(item.count / max) * 100}%` }} />
          </div>
          <span className="w-8 shrink-0 text-right text-gray-500">{item.count}</span>
        </div>
      ))}
    </div>
  );
}

function DailyBars({ points }: { points: LinkAnalytics["dailyClicks"] }) {
  if (points.length === 0) {
    return <p className="text-sm text-gray-500">No click data in the last 14 days.</p>;
  }
  const max = Math.max(1, ...points.map((p) => p.views));
  return (
    <div className="flex items-end gap-1 h-32">
      {points.map((p) => (
        <div
          key={p.date}
          className="flex-1 flex flex-col items-center justify-end h-full"
          title={`${p.date}: ${p.views} views, ${p.uniqueVisitors} unique`}
        >
          <div
            className="w-full bg-blue-500 rounded-t"
            style={{ height: `${Math.max(4, (p.views / max) * 100)}%` }}
          />
          <span className="mt-1 text-[10px] text-gray-400">{p.date.slice(5)}</span>
        </div>
      ))}
    </div>
  );
}

export default function AnalyticsSection({ linkId }: { linkId: string }) {
  const [analytics, setAnalytics] = useState<LinkAnalytics | null>(null);
  const [error, setError] = useState("");

  useEffect(() => {
    (async () => {
      try {
        setAnalytics(await api.get<LinkAnalytics>(`/api/links/${linkId}/analytics`));
      } catch (err) {
        setError(err instanceof ApiError ? err.message : "Failed to load analytics");
      }
    })();
  }, [linkId]);

  if (error) {
    return <p className="text-sm text-red-600">{error}</p>;
  }

  if (!analytics) {
    return <p className="text-sm text-gray-500">Loading analytics...</p>;
  }

  if (!analytics.analyticsAvailable) {
    return (
      <p className="text-sm text-gray-500">
        Detailed analytics are unavailable right now (ClickHouse isn&apos;t reachable). The
        total click count above is still accurate - it comes from the primary database,
        not the analytics store.
      </p>
    );
  }

  return (
    <div className="space-y-6">
      <div>
        <h3 className="text-sm font-medium text-gray-500 mb-2">Clicks, last 14 days</h3>
        <DailyBars points={analytics.dailyClicks} />
      </div>
      <div className="grid gap-6 sm:grid-cols-2">
        <div>
          <h3 className="text-sm font-medium text-gray-500 mb-2">Devices</h3>
          <BarList items={analytics.deviceBreakdown} emptyLabel="No device data yet." />
        </div>
        <div>
          <h3 className="text-sm font-medium text-gray-500 mb-2">Top referrers</h3>
          <BarList items={analytics.topReferrers} emptyLabel="No referrer data yet." />
        </div>
      </div>
    </div>
  );
}
