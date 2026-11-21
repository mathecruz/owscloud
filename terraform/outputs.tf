output "table_name" {
  value = aws_dynamodb_table.orders.name
}

output "table_arn" {
  value = aws_dynamodb_table.orders.arn
}

output "dynamodb_endpoint" {
  description = "Endpoint to use in the workflow instead of http://localhost:4566/."
  value       = "https://dynamodb.${var.region}.amazonaws.com/"
}

output "put_item_policy_arn" {
  value = aws_iam_policy.orders_put_item.arn
}

output "app_role_arn" {
  description = "Role to run the application with (role_arn of an AWS profile, ECS task role, EC2 instance profile...)."
  value       = aws_iam_role.app.arn
}
