import { Container } from '@cloudflare/containers';
import { handleRequest, INSTANCE_NAME } from './edge';

const API_PORT = 8080;

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
      JWT_SECRET: env.JWT_SECRET,
      ADMIN_EMAIL: env.ADMIN_EMAIL,
      ADMIN_PASSWORD: env.ADMIN_PASSWORD,
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
  fetch(request: Request, env: Env): Promise<Response> {
    return handleRequest(request, env, () => env.FRANQUIX_CONTAINER.getByName(INSTANCE_NAME));
  },
} satisfies ExportedHandler<Env>;
