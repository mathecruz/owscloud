variable "region" {
  description = "AWS region where the table is created."
  type        = string
  default     = "us-east-1"
}

variable "table_name" {
  description = "Name of the DynamoDB table used by the dynamodb-put-item workflow."
  type        = string
  default     = "orders"
}

variable "point_in_time_recovery" {
  description = "Enable point-in-time recovery (continuous backups) on the table."
  type        = bool
  default     = false
}

variable "deletion_protection" {
  description = "Prevent the table from being deleted (terraform destroy fails while enabled)."
  type        = bool
  default     = false
}

variable "app_trusted_principals" {
  description = "IAM principal ARNs allowed to assume the application role. Empty: the identity running terraform."
  type        = list(string)
  default     = []
}

variable "app_trusted_services" {
  description = "AWS services allowed to assume the application role where it runs, e.g. ecs-tasks.amazonaws.com or ec2.amazonaws.com."
  type        = list(string)
  default     = []
}
