import Link from "next/link";

const features = [
  {
    title: "Fast Link Shortening",
    body: "Transform lengthy URLs into short, memorable links in just seconds. Built for speed and reliability across social media and anywhere else you need to share.",
  },
  {
    title: "Powerful Analytics",
    body: "See detailed metrics on your shortened URLs. Track traffic by region and device type, and understand your users' journeys when they click your links.",
  },
  {
    title: "QR Code Generator",
    body: "Bridge the gap between the physical and digital world. Generate customizable QR codes for any shortened link, perfect for print materials and offline sources.",
  },
];

export default function Home() {
  return (
    <main className="flex min-h-screen flex-col items-center p-6">
      <div className="w-full max-w-3xl flex flex-col items-center text-center pt-24 pb-16">
        <h1 className="text-4xl font-bold mb-4">LinkPilot</h1>
        <p className="text-lg text-gray-600 max-w-xl">
          A short link is a powerful marketing tool when you use it carefully - it&apos;s the
          medium between you and your customer and their destination.
        </p>
        <div className="mt-8 flex gap-4">
          <Link
            href="/register"
            className="px-6 py-2.5 bg-blue-600 text-white font-medium rounded-md hover:!bg-blue-700 transition-colors"
          >
            Get started
          </Link>
          <Link
            href="/login"
            className="px-6 py-2.5 border border-gray-300 font-medium rounded-md hover:bg-gray-50 transition-colors"
          >
            Log in
          </Link>
        </div>
      </div>

      <div className="w-full max-w-4xl grid gap-8 sm:grid-cols-3 pb-24">
        {features.map((feature) => (
          <div key={feature.title}>
            <h2 className="font-semibold mb-2">{feature.title}</h2>
            <p className="text-sm text-gray-600">{feature.body}</p>
          </div>
        ))}
      </div>

      <footer className="mt-auto text-center text-sm text-gray-500 pb-8">
        <p>Built with Next.js, Spring Boot, SQLite &amp; Redis</p>
      </footer>
    </main>
  );
}
