variable "org_id" {
  description = "Atlas organization id where the project is created."
  type        = string
}

variable "project_name" {
  description = "Name of the Atlas project to create (a free M0 cluster is limited to one per project)."
  type        = string
}

variable "cluster_name" {
  description = "Name of the M0 cluster."
  type        = string
}

variable "backing_provider" {
  description = "Cloud provider hosting the shared M0 cluster: AWS, GCP or AZURE."
  type        = string
  default     = "AWS"

  validation {
    condition     = contains(["AWS", "GCP", "AZURE"], var.backing_provider)
    error_message = "backing_provider must be AWS, GCP or AZURE."
  }
}

variable "region" {
  description = "Atlas region name for the M0 cluster, e.g. US_EAST_1. It must offer the free tier."
  type        = string
  default     = "US_EAST_1"
}

variable "database_name" {
  description = "Database the application user can read and write."
  type        = string
  default     = "franquix"
}

variable "database_username" {
  description = "Username of the application database user."
  type        = string
  default     = "franquix-app"
}

variable "allowed_cidr_blocks" {
  description = "CIDR blocks allowed to reach the cluster (IP access list)."
  type = list(object({
    cidr_block = string
    comment    = string
  }))

  validation {
    condition     = length(var.allowed_cidr_blocks) > 0
    error_message = "At least one CIDR block is required, otherwise nothing can connect."
  }
}
