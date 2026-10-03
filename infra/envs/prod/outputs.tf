output "atlas_project_id" {
  description = "Atlas project id."
  value       = module.mongodb_atlas.project_id
}

output "atlas_cluster_name" {
  description = "Name of the M0 cluster."
  value       = module.mongodb_atlas.cluster_name
}

output "mongodb_srv_host" {
  description = "SRV connection string without credentials."
  value       = module.mongodb_atlas.standard_srv
}

output "mongodb_uri" {
  description = "Value for the MONGODB_URI secret. Read it with: terraform output -raw mongodb_uri"
  value       = module.mongodb_atlas.connection_uri
  sensitive   = true
}
