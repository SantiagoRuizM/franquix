resource "mongodbatlas_project" "this" {
  name   = var.project_name
  org_id = var.org_id
}

# Free shared tier: M0 is expressed as a TENANT cluster backed by a cloud provider.
resource "mongodbatlas_advanced_cluster" "this" {
  project_id   = mongodbatlas_project.this.id
  name         = var.cluster_name
  cluster_type = "REPLICASET"

  replication_specs = [
    {
      region_configs = [
        {
          provider_name         = "TENANT"
          backing_provider_name = var.backing_provider
          region_name           = var.region
          priority              = 7
          electable_specs = {
            instance_size = "M0"
          }
        }
      ]
    }
  ]

  termination_protection_enabled = false
}

# Alphanumeric only, so it can be embedded in a connection string without escaping issues.
resource "random_password" "database_user" {
  length  = 32
  special = false
}

# Least privilege: the application only gets readWrite on its own database.
resource "mongodbatlas_database_user" "app" {
  project_id         = mongodbatlas_project.this.id
  username           = var.database_username
  password           = random_password.database_user.result
  auth_database_name = "admin"

  roles {
    role_name     = "readWrite"
    database_name = var.database_name
  }

  scopes {
    name = mongodbatlas_advanced_cluster.this.name
    type = "CLUSTER"
  }
}

resource "mongodbatlas_project_ip_access_list" "this" {
  for_each = { for entry in var.allowed_cidr_blocks : entry.cidr_block => entry }

  project_id = mongodbatlas_project.this.id
  cidr_block = each.value.cidr_block
  comment    = each.value.comment
}

locals {
  srv_host = trimprefix(mongodbatlas_advanced_cluster.this.connection_strings.standard_srv, "mongodb+srv://")
  connection_uri = format(
    "mongodb+srv://%s:%s@%s/%s?retryWrites=true&w=majority&appName=%s",
    urlencode(mongodbatlas_database_user.app.username),
    urlencode(random_password.database_user.result),
    local.srv_host,
    var.database_name,
    var.cluster_name,
  )
}
