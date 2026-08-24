import { NextRequest, NextResponse } from "next/server";

export const BACKEND_URL = process.env.BACKEND_URL ?? "http://localhost:8080";

const ACCESS_TOKEN_MAX_AGE = 15 * 60; // seconds, matches jwt.access-token-expiration-minutes
const REFRESH_TOKEN_MAX_AGE = 30 * 24 * 60 * 60; // seconds, matches jwt.refresh-token-expiration-days

export interface AuthTokens {
  accessToken: string;
  refreshToken: string;
}

export function setAuthCookies(response: NextResponse, tokens: AuthTokens) {
  response.cookies.set("accessToken", tokens.accessToken, {
    httpOnly: true,
    sameSite: "lax",
    path: "/",
    maxAge: ACCESS_TOKEN_MAX_AGE,
  });
  response.cookies.set("refreshToken", tokens.refreshToken, {
    httpOnly: true,
    sameSite: "lax",
    path: "/",
    maxAge: REFRESH_TOKEN_MAX_AGE,
  });
}

export function clearAuthCookies(response: NextResponse) {
  response.cookies.delete("accessToken");
  response.cookies.delete("refreshToken");
}

/**
 * Proxies a request to the Spring backend, attaching the accessToken cookie as a
 * Bearer token. On a 401, attempts one silent refresh (via the refreshToken cookie)
 * and retries once before giving up - new tokens are written back onto the returned
 * NextResponse's cookies.
 */
export async function proxyToBackend(
  request: NextRequest,
  path: string,
  init: RequestInit = {}
): Promise<NextResponse> {
  const accessToken = request.cookies.get("accessToken")?.value;

  const callBackend = (token?: string) => {
    const headers = new Headers(init.headers);
    if (init.body && !headers.has("Content-Type")) {
      headers.set("Content-Type", "application/json");
    }
    if (token) {
      headers.set("Authorization", `Bearer ${token}`);
    }
    return fetch(`${BACKEND_URL}${path}`, { ...init, headers, cache: "no-store" });
  };

  let backendResponse = await callBackend(accessToken);
  let refreshedTokens: AuthTokens | null = null;

  if (backendResponse.status === 401) {
    const refreshToken = request.cookies.get("refreshToken")?.value;
    if (refreshToken) {
      const refreshResponse = await fetch(`${BACKEND_URL}/api/auth/refresh`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ refreshToken }),
        cache: "no-store",
      });
      if (refreshResponse.ok) {
        const data = await refreshResponse.json();
        refreshedTokens = { accessToken: data.accessToken, refreshToken: data.refreshToken };
        backendResponse = await callBackend(refreshedTokens.accessToken);
      }
    }
  }

  const contentType = backendResponse.headers.get("content-type");
  const body = backendResponse.status === 204 ? null : await backendResponse.arrayBuffer();
  const response = new NextResponse(body, {
    status: backendResponse.status,
    headers: contentType ? { "Content-Type": contentType } : undefined,
  });

  if (refreshedTokens) {
    setAuthCookies(response, refreshedTokens);
  }
  return response;
}
