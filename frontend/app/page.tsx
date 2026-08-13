"use client";
import Link from 'next/link';
import { useState } from 'react';

export default function Home() {
  const [url, setUrl] = useState('');
  const [shortUrl, setShortUrl] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    try {
      const response = await fetch('/api/links', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ url }),
      });

      if (response.ok) {
        const data = await response.json();
        // Assuming our backend returns the full URL like http://localhost:8080/api/links/{shortCode}
        setShortUrl(`${window.location.origin}/${data.shortCode}`);
      } else {
        alert('Failed to shorten URL');
      }
    } catch (error) {
      console.error('Error:', error);
      alert('An error occurred');
    } finally {
      setLoading(false);
    }
  };

  return (
    <main className="flex min-h-screen flex-col items-center justify-between p-24">
      <div className="w-full max-w-xl">
        <h1 className="mb-6 text-3xl font-bold text-center">
          LinkPilot - URL Shortener
        </h1>
        
        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label htmlFor="url" className="block text-sm font-medium mb-2">
              Enter URL to shorten
            </label>
            <input
              type="url"
              id="url"
              value={url}
              onChange={(e) => setUrl(e.target.value)}
              placeholder="https://example.com/very/long/url/that/needs/to/be/shortened"
              className="w-full px-4 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
              required
            />
          </div>
          
          <button
            type="submit"
            disabled={loading || !url}
            className="w-full px-4 py-2 bg-blue-600 text-white font-medium rounded-md disabled:opacity-50 hover:!bg-blue-700 transition-colors"
          >
            {loading ? 'Shortening...' : 'Shorten URL'}
          </button>
        </form>

        {shortUrl && (
          <div className="mt-6 p-4 bg-gray-50 rounded-md">
            <p className="mb-2 text-sm font-medium text-gray-700">
              Your shortened URL:
            </p>
            <div className="flex items-center space-x-3">
              <input
                type="text"
                value={shortUrl}
                readOnly
                className="flex-1 px-4 py-2 bg-white border border-gray-300 rounded-md focus:outline-none"
              />
              <button
                onClick={() => {
                  navigator.clipboard.writeText(shortUrl);
                  alert('Copied to clipboard!');
                }}
                className="px-4 py-2 bg-green-600 text-white font-medium rounded-md hover:!bg-green-700 transition-colors"
              >
                Copy
              </button>
            </div>
          </div>
        )}
      </div>
      
      <footer className="mt-auto text-center text-sm text-gray-500">
        <p>Built with Next.js, Spring Boot, PostgreSQL & Redis</p>
      </footer>
    </main>
  );}