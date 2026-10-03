terraform {
  required_version = ">= 1.6.0"

  required_providers {
    mongodbatlas = {
      source  = "mongodb/mongodbatlas"
      version = "~> 2.19"
    }
    random = {
      source  = "hashicorp/random"
      version = "~> 3.7"
    }
  }
}

# Credentials come from the environment, never from files:
#   service account: MONGODB_ATLAS_CLIENT_ID / MONGODB_ATLAS_CLIENT_SECRET
#   or API key:      MONGODB_ATLAS_PUBLIC_API_KEY / MONGODB_ATLAS_PRIVATE_API_KEY
provider "mongodbatlas" {}
