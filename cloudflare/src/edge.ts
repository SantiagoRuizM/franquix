/**
 * Edge routing, kept free of `@cloudflare/containers` so it can be unit tested in plain Node.
 */

/** Secrets the container needs; the Worker refuses traffic until all of them are configured. */
export const REQUIRED_SECRETS = ['MONGODB_URI', 'JWT_SECRET', 'ADMIN_EMAIL', 'ADMIN_PASSWORD'] as const;

export type EdgeSecrets = Partial<Record<(typeof REQUIRED_SECRETS)[number], string>>;

/** Anything that can forward a request to the API container. */
export interface ContainerStub {
  fetch(request: Request): Promise<Response>;
}

/** Name of the single container instance every request is routed to. */
export const INSTANCE_NAME = 'franquix-primary';

export async function handleRequest(
  request: Request,
  secrets: EdgeSecrets,
  container: () => ContainerStub,
): Promise<Response> {
  const url = new URL(request.url);

  // Answered by the Worker itself, so checking the edge does not wake the container
  if (url.pathname === '/edge-health') {
    return Response.json({ status: 'UP', component: 'worker' });
  }

  const missing = REQUIRED_SECRETS.filter((name) => !secrets[name]);
  if (missing.length > 0) {
    return problem(503, 'Service Unavailable', `Missing secrets: ${missing.join(', ')}`, url.pathname);
  }

  return container().fetch(forwarded(request, url));
}

/**
 * Tells Spring the public scheme, host and client IP. X-Forwarded-For is overwritten, never appended:
 * a client-supplied value would let callers spoof their IP and dodge the per-IP rate limits.
 */
export function forwarded(request: Request, url: URL = new URL(request.url)): Request {
  const copy = new Request(request);
  copy.headers.set('X-Forwarded-Proto', url.protocol.replace(':', ''));
  copy.headers.set('X-Forwarded-Host', url.host);
  const clientIp = request.headers.get('CF-Connecting-IP');
  if (clientIp) {
    copy.headers.set('X-Forwarded-For', clientIp);
  } else {
    copy.headers.delete('X-Forwarded-For');
  }
  return copy;
}

function problem(status: number, title: string, detail: string, instance: string): Response {
  return Response.json(
    { type: 'about:blank', title, status, detail, instance },
    { status, headers: { 'content-type': 'application/problem+json' } },
  );
}
