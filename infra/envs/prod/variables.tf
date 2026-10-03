variable "atlas_org_id" {
  description = "Atlas organization id (Organization Settings > Organization ID)."
  type        = string
}

variable "project_name" {
  description = "Atlas project to create."
  type        = string
  default     = "franquix"
}

variable "cluster_name" {
  description = "Name of the free M0 cluster."
  type        = string
  default     = "franquix-prod"
}

variable "region" {
  description = "Atlas region of the M0 cluster (must offer the free tier)."
  type        = string
  default     = "US_EAST_1"
}

variable "allowed_cidr_blocks" {
  description = <<-EOT
    IP access list of the cluster. Cloudflare Containers have no static egress IP and the free
    M0 tier supports neither VPC peering nor private endpoints, so the default opens the cluster
    to any address; access is still protected by TLS and the generated 32-character password.
    Narrow it (e.g. to your own IP) when the runtime has a known egress address.
  EOT
  type = list(object({
    cidr_block = string
    comment    = string
  }))
  default = [
    {
      cidr_block = "0.0.0.0/0"
      comment    = "Cloudflare Containers: no static egress IP"
    }
  ]
}
