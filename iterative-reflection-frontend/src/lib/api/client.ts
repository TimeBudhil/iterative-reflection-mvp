import createClient from "openapi-fetch";
import type { paths } from "./schema";

/**
 * The only way the app talks to the backend.
 *
 * Callers use typed operations (`api.GET("/api/me")`, ...) built from the generated OpenAPI schema and get
 * back `{ data }` or a thrown {@link ApiError}. Everything transport-related is hidden in here:
 * same-origin cookies, the CSRF dance (see DECISIONS.md "CSRF behaviour"), problem+json parsing and the
 * 401/403 hooks.
 */

export type ApiErrorInit = {
  status: number;
  detail: string;
  fieldErrors?: Record<string, string>;
  requestId?: string | null;
};

export class ApiError extends Error {
  readonly status: number;
  readonly detail: string;
  /** Server-side validation messages keyed by request field (first message per field). */
  readonly fieldErrors: Record<string, string>;
  readonly requestId: string | null;

  constructor({ status, detail, fieldErrors = {}, requestId = null }: ApiErrorInit) {
    super(detail);
    this.name = "ApiError";
    this.status = status;
    this.detail = detail;
    this.fieldErrors = fieldErrors;
    this.requestId = requestId;
  }
}

export type ApiHooks = {
  /** A 401 on anything other than sign-in/sign-up (session expired). */
  onUnauthorized?: (error: ApiError) => void;
  /** A 403 that survived the single CSRF retry (role/ownership changed, R2.7). */
  onForbidden?: (error: ApiError) => void;
};

export type ApiClientOptions = ApiHooks & {
  fetch?: typeof fetch;
  baseUrl?: string;
};

const CSRF_COOKIE = "XSRF-TOKEN";
const CSRF_HEADER = "X-XSRF-TOKEN";
const PRIME_PATH = "/api/me";
const MUTATING = new Set(["POST", "PUT", "PATCH", "DELETE"]);
/** 401 here means "wrong credentials", not "session expired". */
const AUTH_ENTRY_PATHS = new Set(["/api/auth/signin", "/api/auth/signup"]);

function readCsrfCookie(): string | null {
  if (typeof document === "undefined") return null;
  for (const part of document.cookie.split(";")) {
    const eq = part.indexOf("=");
    if (eq > -1 && part.slice(0, eq).trim() === CSRF_COOKIE) {
      try {
        return decodeURIComponent(part.slice(eq + 1).trim());
      } catch {
        return part.slice(eq + 1).trim();
      }
    }
  }
  return null;
}

async function toApiError(res: Response): Promise<ApiError> {
  const requestId = res.headers.get("X-Request-Id");
  let detail = res.statusText || `Request failed (${res.status})`;
  const fieldErrors: Record<string, string> = {};
  try {
    const body: unknown = await res.json();
    if (body && typeof body === "object") {
      const b = body as { detail?: unknown; errors?: unknown };
      if (typeof b.detail === "string" && b.detail) detail = b.detail;
      if (Array.isArray(b.errors)) {
        for (const e of b.errors) {
          if (e && typeof e.field === "string" && typeof e.message === "string" && !(e.field in fieldErrors)) {
            fieldErrors[e.field] = e.message;
          }
        }
      }
    }
  } catch {
    // Not JSON (e.g. an HTML 502 from a proxy): keep the generic detail.
  }
  return new ApiError({ status: res.status, detail, fieldErrors, requestId });
}

export function createApiClient(options: ApiClientOptions = {}) {
  const hooks: ApiHooks = { onUnauthorized: options.onUnauthorized, onForbidden: options.onForbidden };
  const baseUrl = options.baseUrl ?? (typeof window !== "undefined" ? window.location.origin : "");
  const doFetch: typeof fetch = (...args) => (options.fetch ?? globalThis.fetch)(...args);

  let priming: Promise<void> | null = null;
  /** GET /api/me makes the backend (re)issue XSRF-TOKEN, even when it answers 401. Concurrent callers share one call. */
  function prime(): Promise<void> {
    priming ??= doFetch(`${baseUrl}${PRIME_PATH}`, { credentials: "include" })
      .then(() => undefined)
      .catch(() => undefined)
      .finally(() => {
        priming = null;
      });
    return priming;
  }

  async function send(request: Request): Promise<Response> {
    const mutating = MUTATING.has(request.method);
    const path = new URL(request.url).pathname;

    const attempt = (req: Request) => {
      const headers = new Headers(req.headers);
      const token = mutating ? readCsrfCookie() : null;
      if (token) headers.set(CSRF_HEADER, token);
      return doFetch(new Request(req, { headers, credentials: "include" }));
    };

    let res: Response;
    try {
      if (mutating && !readCsrfCookie()) await prime();
      const retryCopy = mutating ? request.clone() : null;
      const sentToken = mutating ? readCsrfCookie() : null;
      res = await attempt(request);
      if (mutating && res.status === 403 && retryCopy) {
        // A CSRF 403 looks exactly like a role-denial 403, so re-prime and retry (once) only if the
        // token actually changed. If it didn't, this is a genuine denial and goes to onForbidden.
        await prime();
        const fresh = readCsrfCookie();
        if (fresh && fresh !== sentToken) res = await attempt(retryCopy);
      }
    } catch {
      throw new ApiError({ status: 0, detail: "Network error" });
    }

    if (res.ok) return res;
    const error = await toApiError(res);
    if (error.status === 401 && !AUTH_ENTRY_PATHS.has(path)) hooks.onUnauthorized?.(error);
    if (error.status === 403) hooks.onForbidden?.(error);
    throw error;
  }

  const client = createClient<paths>({
    baseUrl,
    // openapi-fetch hands us a fully built Request.
    fetch: (input) => send(input as Request),
  });

  return Object.assign(client, {
    /** Plug in the app-level reactions to 401/403 (set once, at startup). */
    setHooks(next: ApiHooks) {
      Object.assign(hooks, next);
    },
  });
}

export type ApiClient = ReturnType<typeof createApiClient>;

/** The app-wide client. */
export const api: ApiClient = createApiClient();
