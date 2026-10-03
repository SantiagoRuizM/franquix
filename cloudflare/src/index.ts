import { Container } from '@cloudflare/containers';

const API_PORT = 8080;

/** Name of the single container instance every request is routed to. */
const INSTANCE_NAME = 'franquix-primary';

/**
 * Durable Object that owns the Franquix API container.
 *
 * The JVM needs far longer than the library defaults to boot on a `basic` instance, so the
 * start-up waits are extended, and the container stays warm for hours after the last request to
 * avoid paying the cold start on every visit. It still scales to zero when idle.
 */
export class FranquixContainer extends Container<Env> {
  override defaultPort = API_PORT;
  override sleepAfter = '2h';
  // Any HTTP answer means the port is open; Spring only binds it once fully started
  override pingEndpoint = 'container/actuator/health';

  constructor(ctx: DurableObjectState<Env>, env: Env) {
    super(ctx, env);
    // Worker secrets are not visible to the container; hand them over explicitly
    this.envVars = {
      MONGODB_URI: env.MONGODB_URI,
      MONGODB_DATABASE: 'franquix',
      SPRING_PROFILES_ACTIVE: 'prod',
      PORT: String(API_PORT),
    };
  }

  override async fetch(request: Request): Promise<Response> {
    const state = await this.getState();
    if (state.status !== 'healthy') {
      await this.startAndWaitForPorts({
        ports: API_PORT,
        cancellationOptions: {
          abort: request.signal,
          instanceGetTimeoutMS: 60_000,
          portReadyTimeoutMS: 180_000,
        },
      });
    }
    return this.containerFetch(request, API_PORT);
  }

  override onStart(): void {
    console.log('franquix container started');
  }

  override onStop(): void {
    console.log('franquix container stopped');
  }

  override onError(error: unknown): never {
    console.error('franquix container error', error);
    throw error;
  }
}

export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    const url = new URL(request.url);

    // Answered by the Worker itself, so checking the edge does not wake the container
    if (url.pathname === '/edge-health') {
      return Response.json({ status: 'UP', component: 'worker' });
    }

    if (!env.MONGODB_URI) {
      return Response.json(
        {
          type: 'about:blank',
          title: 'Service Unavailable',
          status: 503,
          detail: 'MONGODB_URI secret is not configured',
          instance: url.pathname,
        },
        { status: 503, headers: { 'content-type': 'application/problem+json' } },
      );
    }

    // Tell Spring the public scheme and host, so generated Location headers are correct
    const forwarded = new Request(request);
    forwarded.headers.set('X-Forwarded-Proto', url.protocol.replace(':', ''));
    forwarded.headers.set('X-Forwarded-Host', url.host);

    const container = env.FRANQUIX_CONTAINER.getByName(INSTANCE_NAME);
    return container.fetch(forwarded);
  },
} satisfies ExportedHandler<Env>;
