import { beforeEach, describe, expect, it, vi } from "vitest";
import { ApiError, createApiClient } from "./client";

type Handler = (req: Request) => Response | Promise<Response>;

function setup(handler: Handler) {
  const calls: Request[] = [];
  const fetchStub = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
    const req = input instanceof Request ? input : new Request(new URL(String(input), "http://localhost"), init);
    calls.push(req.clone());
    return handler(req);
  });
  const onUnauthorized = vi.fn();
  const onForbidden = vi.fn();
  const api = createApiClient({ fetch: fetchStub as unknown as typeof fetch, onUnauthorized, onForbidden });
  return { api, calls, onUnauthorized, onForbidden };
}

const json = (body: unknown, status = 200, headers: Record<string, string> = {}) =>
  new Response(JSON.stringify(body), { status, headers: { "Content-Type": "application/json", ...headers } });

const clearCookies = () =>
  document.cookie.split(";").forEach((c) => {
    document.cookie = `${c.split("=")[0].trim()}=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/`;
  });

const label = (c: Request) => `${c.method} ${new URL(c.url).pathname}`;
const ME = { id: 1, email: "a@b.c", displayName: "A", role: "ADMIN", mustChangePassword: false, intakePromptDue: false };
const pwBody = { currentPassword: "a", newPassword: "b" } as never;

describe("api client", () => {
  beforeEach(() => {
    clearCookies();
    localStorage.clear();
    sessionStorage.clear();
  });

  it("sends credentials: include and no CSRF header on GET", async () => {
    document.cookie = "XSRF-TOKEN=tok; path=/";
    const { api, calls } = setup(() => json(ME));
    const { data } = await api.GET("/api/me");
    expect(data?.email).toBe("a@b.c");
    expect(calls[0].credentials).toBe("include");
    expect(calls[0].headers.get("X-XSRF-TOKEN")).toBeNull();
  });

  it.each(["POST", "PUT", "PATCH", "DELETE"] as const)("%s carries X-XSRF-TOKEN from the cookie", async (method) => {
    document.cookie = "XSRF-TOKEN=tok%20en; path=/";
    const { api, calls } = setup(() => new Response(null, { status: 204 }));
    // The typed path is irrelevant to the transport; cast to exercise every verb.
    const loose = api as unknown as Record<string, (p: string, o?: unknown) => Promise<unknown>>;
    await loose[method]("/api/me/password", { body: {} });
    expect(calls).toHaveLength(1);
    expect(calls[0].method).toBe(method);
    expect(calls[0].headers.get("X-XSRF-TOKEN")).toBe("tok en");
  });

  it("primes the cookie with GET /api/me before the first mutation when it is missing", async () => {
    const { api, calls } = setup((req) => {
      if (new URL(req.url).pathname === "/api/me") {
        document.cookie = "XSRF-TOKEN=fresh; path=/";
        return json({ detail: "Authentication is required.", status: 401 }, 401);
      }
      return new Response(null, { status: 204 });
    });
    await api.PUT("/api/me/password", { body: pwBody });
    expect(calls.map(label)).toEqual(["GET /api/me", "PUT /api/me/password"]);
    expect(calls[1].headers.get("X-XSRF-TOKEN")).toBe("fresh");
  });

  it("a failed priming call (401) does not fire onUnauthorized", async () => {
    const { api, onUnauthorized } = setup((req) => {
      if (new URL(req.url).pathname === "/api/me") {
        document.cookie = "XSRF-TOKEN=fresh; path=/";
        return json({ status: 401 }, 401);
      }
      return new Response(null, { status: 204 });
    });
    await api.PUT("/api/me/password", { body: pwBody });
    expect(onUnauthorized).not.toHaveBeenCalled();
  });

  it("retries exactly once after a CSRF 403, re-priming first", async () => {
    document.cookie = "XSRF-TOKEN=stale; path=/";
    let puts = 0;
    const { api, calls } = setup((req) => {
      if (new URL(req.url).pathname === "/api/me") {
        document.cookie = "XSRF-TOKEN=rotated; path=/";
        return json(ME);
      }
      puts++;
      return puts === 1 ? json({ detail: "Access is denied.", status: 403 }, 403) : new Response(null, { status: 204 });
    });
    await api.PUT("/api/me/password", { body: pwBody });
    expect(calls.map(label)).toEqual(["PUT /api/me/password", "GET /api/me", "PUT /api/me/password"]);
    expect(calls[2].headers.get("X-XSRF-TOKEN")).toBe("rotated");
  });

  it("a 403 that persists after the single retry fires onForbidden", async () => {
    document.cookie = "XSRF-TOKEN=stale; path=/";
    const { api, calls, onForbidden } = setup((req) => {
      if (new URL(req.url).pathname === "/api/me") {
        document.cookie = "XSRF-TOKEN=rotated; path=/";
        return json(ME);
      }
      return json({ detail: "Access is denied.", status: 403 }, 403);
    });
    await expect(api.PUT("/api/me/password", { body: pwBody })).rejects.toMatchObject({ status: 403 });
    expect(calls.filter((c) => c.method === "PUT")).toHaveLength(2);
    expect(onForbidden).toHaveBeenCalledTimes(1);
  });

  it("re-primes before the next mutation when a previous call cleared the cookie (sign-in/out, password change)", async () => {
    document.cookie = "XSRF-TOKEN=old; path=/";
    const { api, calls } = setup((req) => {
      const path = new URL(req.url).pathname;
      if (path === "/api/me") {
        document.cookie = "XSRF-TOKEN=new; path=/";
        return json(ME);
      }
      document.cookie = "XSRF-TOKEN=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/";
      return new Response(null, { status: 204 });
    });
    await api.PUT("/api/me/password", { body: pwBody });
    await api.PUT("/api/me/password", { body: pwBody });
    expect(calls.map(label)).toEqual(["PUT /api/me/password", "GET /api/me", "PUT /api/me/password"]);
    expect(calls[2].headers.get("X-XSRF-TOKEN")).toBe("new");
  });

  it("a 403 with an unchanged token is a real denial: no retry, onForbidden fires", async () => {
    document.cookie = "XSRF-TOKEN=t; path=/";
    const { api, calls, onForbidden } = setup((req) =>
      new URL(req.url).pathname === "/api/me" ? json(ME) : json({ detail: "Access is denied.", status: 403 }, 403),
    );
    await expect(api.PUT("/api/me/password", { body: pwBody })).rejects.toMatchObject({ status: 403 });
    expect(calls.filter((c) => c.method === "PUT")).toHaveLength(1);
    expect(onForbidden).toHaveBeenCalledTimes(1);
  });

  it("turns a 400 with errors[] into fieldErrors and reads X-Request-Id", async () => {
    document.cookie = "XSRF-TOKEN=t; path=/";
    const { api } = setup(() =>
      json(
        {
          detail: "Validation failed.",
          status: 400,
          errors: [
            { field: "email", message: "must not be blank" },
            { field: "password", message: "too short" },
          ],
        },
        400,
        { "X-Request-Id": "req-1" },
      ),
    );
    const err = await api.POST("/api/auth/signup", { body: {} as never }).catch((e) => e);
    expect(err).toBeInstanceOf(ApiError);
    expect(err).toMatchObject({
      status: 400,
      detail: "Validation failed.",
      fieldErrors: { email: "must not be blank", password: "too short" },
      requestId: "req-1",
    });
  });

  it("still yields an ApiError for a non-JSON 5xx", async () => {
    const { api } = setup(
      () => new Response("<html>boom</html>", { status: 502, headers: { "Content-Type": "text/html", "X-Request-Id": "r9" } }),
    );
    const err = await api.GET("/api/me").catch((e) => e);
    expect(err).toBeInstanceOf(ApiError);
    expect(err).toMatchObject({ status: 502, fieldErrors: {}, requestId: "r9" });
    expect(typeof err.detail).toBe("string");
  });

  it("yields an ApiError with status 0 when the network fails", async () => {
    const failing = (async () => {
      throw new TypeError("failed to fetch");
    }) as unknown as typeof fetch;
    const api = createApiClient({ fetch: failing });
    await expect(api.GET("/api/me")).rejects.toMatchObject({ status: 0 });
  });

  it("401 fires onUnauthorized, except for sign-in", async () => {
    document.cookie = "XSRF-TOKEN=t; path=/";
    const { api, onUnauthorized } = setup(() => json({ detail: "Authentication is required.", status: 401 }, 401));
    await expect(api.GET("/api/me")).rejects.toMatchObject({ status: 401 });
    expect(onUnauthorized).toHaveBeenCalledTimes(1);
    onUnauthorized.mockClear();
    await expect(api.POST("/api/auth/signin", { body: { email: "a", password: "b" } as never })).rejects.toMatchObject({
      status: 401,
    });
    expect(onUnauthorized).not.toHaveBeenCalled();
  });

  it("a 403 on a GET fires onForbidden without retrying", async () => {
    const { api, calls, onForbidden } = setup(() => json({ detail: "Access is denied.", status: 403 }, 403));
    await expect(api.GET("/api/me")).rejects.toMatchObject({ status: 403 });
    expect(calls).toHaveLength(1);
    expect(onForbidden).toHaveBeenCalledTimes(1);
  });

  it("writes nothing to web storage", async () => {
    document.cookie = "XSRF-TOKEN=t; path=/";
    const { api } = setup(() => json(ME));
    await api.GET("/api/me");
    await api.PUT("/api/me/password", { body: pwBody }).catch(() => {});
    expect(localStorage.length).toBe(0);
    expect(sessionStorage.length).toBe(0);
  });

  it("flattens a Spring pageable into page/size and keeps other params", async () => {
    const { api, calls } = setup(() => json({ content: [] }));
    await api.GET("/api/admin/audit", { params: { query: { studentId: 7, pageable: { page: 2, size: 20 } } } });
    const url = new URL(calls[0].url);
    expect(url.searchParams.get("page")).toBe("2");
    expect(url.searchParams.get("size")).toBe("20");
    expect(url.searchParams.get("studentId")).toBe("7");
    expect(url.search).not.toContain("pageable");
  });

  it("omits undefined query params", async () => {
    const { api, calls } = setup(() => json({ content: [] }));
    await api.GET("/api/admin/audit", { params: { query: { studentId: undefined, pageable: { page: 0, size: 20 } } } });
    expect(new URL(calls[0].url).search).toBe("?page=0&size=20");
  });
});
