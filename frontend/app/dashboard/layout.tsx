import Link from "next/link";
import LogoutButton from "@/components/LogoutButton";

export default function DashboardLayout({ children }: { children: React.ReactNode }) {
  return (
    <div className="min-h-screen flex flex-col">
      <header className="border-b border-gray-200">
        <div className="max-w-5xl mx-auto px-6 py-4 flex items-center justify-between">
          <div className="flex items-center gap-6">
            <Link href="/dashboard" className="font-bold text-lg">
              LinkPilot
            </Link>
            <nav className="flex items-center gap-4 text-sm">
              <Link href="/dashboard" className="text-gray-600 hover:text-gray-900">
                Links
              </Link>
              <Link href="/dashboard/campaigns" className="text-gray-600 hover:text-gray-900">
                Campaigns
              </Link>
              <Link href="/dashboard/domains" className="text-gray-600 hover:text-gray-900">
                Domains
              </Link>
            </nav>
          </div>
          <LogoutButton />
        </div>
      </header>
      <main className="flex-1 max-w-5xl mx-auto w-full px-6 py-8">{children}</main>
    </div>
  );
}
