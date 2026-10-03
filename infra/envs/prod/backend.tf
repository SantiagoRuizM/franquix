# State is local by default so the stack can be evaluated without extra accounts.
# The state contains the generated database password: keep it private and out of git
# (*.tfstate is ignored). For team use, switch to a remote backend, for example the
# free tier of HCP Terraform:
#
# terraform {
#   cloud {
#     organization = "your-org"
#     workspaces {
#       name = "franquix-prod"
#     }
#   }
# }
