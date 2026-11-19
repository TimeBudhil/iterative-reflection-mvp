import type { NextConfig } from "next";

// Must match `career.embed-hosts` in mvp/src/main/resources/application.properties.
const EMBED_HOSTS = [
  "youtube.com",
  "www.youtube.com",
  "www.youtube-nocookie.com",
  "player.vimeo.com",
  "vimeo.com",
  "w.soundcloud.com",
  "soundcloud.com",
  "open.spotify.com",
];

const csp = [
  `frame-src ${EMBED_HOSTS.map((h) => `https://${h}`).join(" ")}`,
  "object-src 'none'",
  "base-uri 'self'",
  "form-action 'self'",
  "frame-ancestors 'none'",
].join("; ");

const nextConfig: NextConfig = {
  reactCompiler: true,
  output: "standalone",
  async rewrites() {
    // Dev only: in prod nginx routes /api/* to the backend.
    const backend = process.env.BACKEND_URL ?? "http://localhost:8080";
    return [{ source: "/api/:path*", destination: `${backend}/api/:path*` }];
  },
  async headers() {
    return [{ source: "/:path*", headers: [{ key: "Content-Security-Policy", value: csp }] }];
  },
};

export default nextConfig;
