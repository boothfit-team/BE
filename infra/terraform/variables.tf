variable "aws_region" {
  description = "AWS Region for the backend deployment"
  type        = string
  default     = "ap-northeast-2"
}

variable "vpc_id" {
  description = "Existing VPC ID; null selects the account's default VPC"
  type        = string
  default     = null
}

variable "subnet_id" {
  description = "Existing public subnet ID; null selects a subnet in the selected VPC"
  type        = string
  default     = null
}

variable "instance_type" {
  description = "EC2 instance type for the single Docker host"
  type        = string
  default     = "t3.small"
}

variable "github_oidc_provider_arn" {
  description = "Existing GitHub Actions OIDC provider ARN, if the AWS account already has one"
  type        = string
  default     = null
}

variable "github_oidc_subject" {
  description = "Exact OIDC sub claim for boothfit-team/BE main; update if repository OIDC settings differ"
  type        = string
  default     = "repo:boothfit-team@347638161/BE@1432841847:ref:refs/heads/main"
}
