# Infrastructure: MongoDB Atlas with Terraform

This provisions the persistence layer of Franquix on the **MongoDB Atlas free tier (M0)**.

| Resource | Purpose |
|---|---|
| `mongodbatlas_project` | Dedicated project (Atlas allows one free M0 cluster per project) |
| `mongodbatlas_advanced_cluster` | M0 shared cluster (`provider_name = "TENANT"`, backed by AWS `US_EAST_1` by default) |
| `random_password` + `mongodbatlas_database_user` | Application user with `readWrite` on the `franquix` database only, scoped to the cluster |
| `mongodbatlas_project_ip_access_list` | IP access list entries |

The application runtime (Cloudflare Containers) is deployed with Wrangler, not Terraform. See the
root [README](../README.md#deploy-to-cloudflare-containers).

## Layout

```
infra/
├── modules/
│   └── mongodb-atlas/   # project, M0 cluster, database user, IP access list, connection URI
└── envs/
    └── prod/            # module instance, provider, outputs, tfvars example, backend notes
```

## Prerequisites

- Terraform >= 1.6
- A MongoDB Atlas account and organization (free)
- Atlas credentials with the **Organization Project Creator** role, exported as environment variables:

```bash
# Option A: service account (recommended)
export MONGODB_ATLAS_CLIENT_ID="mdb_sa_id_..."
export MONGODB_ATLAS_CLIENT_SECRET="mdb_sa_sk_..."

# Option B: programmatic API key
export MONGODB_ATLAS_PUBLIC_API_KEY="..."
export MONGODB_ATLAS_PRIVATE_API_KEY="..."
```

Create them in Atlas under **Organization > Access Manager > Applications**.

## Usage

```bash
cd infra/envs/prod
cp terraform.tfvars.example terraform.tfvars   # set atlas_org_id
terraform init
terraform plan
terraform apply                                # ~5-10 minutes for the cluster

terraform output                               # project, cluster and host
terraform output -raw mongodb_uri              # full connection string (sensitive)
```

Hand the connection string straight to the Cloudflare Worker as a secret, without writing it to disk:

```bash
terraform output -raw mongodb_uri | npx --prefix ../../../cloudflare wrangler secret put MONGODB_URI --config ../../../cloudflare/wrangler.jsonc
```

Destroy everything (this deletes the data):

```bash
terraform destroy
```

## Decisions and trade-offs

- **Open IP access list (`0.0.0.0/0`).** Cloudflare Containers have no static egress IP, and the
  M0 tier supports neither VPC peering nor private endpoints. So by default the cluster accepts
  connections from any address. Access still requires TLS and a 32-character random password
  for a user limited to one database. Override `allowed_cidr_blocks` if your runtime has a known
  egress IP.
- **Password in state.** The generated password lives in the Terraform state, which is why the
  state must stay private. `*.tfstate` is git-ignored, and `backend.tf` documents a remote
  backend for teams.
- **Credentials only from the environment.** The provider block is empty, and no credential is
  ever written to `*.tf` or `*.tfvars` files.
- **Indexes are not managed here.** The application creates its unique and top-stock indexes
  idempotently at startup (`MongoIndexInitializer`), so they also exist locally and in tests.

## Checks

```bash
terraform fmt -recursive -check infra
cd infra/envs/prod && terraform init -backend=false && terraform validate
```

Both run in CI on every push.
