module "mongodb_atlas" {
  source = "../../modules/mongodb-atlas"

  org_id              = var.atlas_org_id
  project_name        = var.project_name
  cluster_name        = var.cluster_name
  region              = var.region
  database_name       = "franquix"
  database_username   = "franquix-app"
  allowed_cidr_blocks = var.allowed_cidr_blocks
}
