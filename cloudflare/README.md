# Cloudflare edge: Worker + Durable Object + Container

The Franquix API runs on **Cloudflare Containers**. Cloudflare does not expose a container
directly, so three small pieces sit in front of the Docker image:

```
client ──HTTPS──▶ Worker (src/index.ts)
                    │  /edge-health answered here (does not wake the container)
                    ▼
                 Durable Object FranquixContainer ("franquix-primary")
                    │  starts the container on demand, passes MONGODB_URI via envVars,
                    │  keeps it warm for sleepAfter = 2h after the last request
                    ▼
                 Container (../Dockerfile, instance "basic": 1 GiB, 1/4 vCPU) :8080
                    │
                    ▼
                 MongoDB Atlas M0 (provisioned by ../infra)
```

| File | Purpose |
|---|---|
| `wrangler.jsonc` | Worker, container (image `../Dockerfile`), Durable Object binding, SQLite export, required secret |
| `src/index.ts` | Worker routing and the `FranquixContainer` class (extended start-up timeouts for the JVM) |
| `.dev.vars.example` | Template for local secrets (`.dev.vars` is git-ignored) |

## Cost

Containers are **not available on the Workers Free plan**. They need **Workers Paid ($5/month)**,
which includes 25 GiB-hours of memory, 375 vCPU-minutes and 200 GB-hours of disk per month.
A `basic` instance uses 1 GiB, so the included memory covers about 25 awake hours a month.
`sleepAfter = '2h'` avoids repeated JVM cold starts during a review session and still scales to
zero when idle. Lower it in `src/index.ts` to reduce billed time. MongoDB Atlas M0 is free.

## Deploy

Requirements: Node 20+, Docker running (wrangler builds the `linux/amd64` image), and a
Cloudflare account on Workers Paid.

```bash
cd cloudflare
npm ci
npx wrangler login                      # or export CLOUDFLARE_API_TOKEN / CLOUDFLARE_ACCOUNT_ID

# MONGODB_URI comes from Terraform (see ../infra/README.md)
terraform -chdir=../infra/envs/prod output -raw mongodb_uri | npx wrangler secret put MONGODB_URI

npx wrangler deploy                     # builds ../Dockerfile, pushes it, deploys the Worker
```

The first deploy provisions the container application, which can take a few minutes. During that
time requests return `503`. The first request after an idle period starts the JVM (tens of
seconds on a `basic` instance). Requests made while it is warm are proxied directly.

```bash
curl https://franquix-api.<your-subdomain>.workers.dev/edge-health
curl https://franquix-api.<your-subdomain>.workers.dev/actuator/health
```

## Checks without deploying

```bash
npm run typegen     # generates worker-configuration.d.ts from wrangler.jsonc
npm run typecheck
npm run check       # wrangler deploy --dry-run: validates config and builds the image
```

## Local run

`wrangler dev` runs the container locally with Docker on **Linux, macOS or WSL**. Wrangler does
not support local containers on native Windows.

```bash
cp .dev.vars.example .dev.vars          # points at the MongoDB from `docker compose up mongo`
npx wrangler dev
```
