import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  // A self-contained server.js + minimal node_modules, instead of needing the full
  // project + node_modules in the runtime image - keeps the Docker image lean.
  output: "standalone",
};

export default nextConfig;
