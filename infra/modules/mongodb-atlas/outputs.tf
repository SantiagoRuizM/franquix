output "project_id" {
  description = "Atlas project id."
  value       = mongodbatlas_project.this.id
}

output "cluster_name" {
  description = "Name of the M0 cluster."
  value       = mongodbatlas_advanced_cluster.this.name
}

output "standard_srv" {
  description = "SRV connection string without credentials."
  value       = mongodbatlas_advanced_cluster.this.connection_strings.standard_srv
}

output "database_name" {
  description = "Application database."
  value       = var.database_name
}

output "connection_uri" {
  description = "Full MONGODB_URI for the application, including credentials."
  value       = local.connection_uri
  sensitive   = true
}
