# Same schema as src/test/resources/localstack-init/create-orders-table.sh
resource "aws_dynamodb_table" "orders" {
  name                        = var.table_name
  billing_mode                = "PAY_PER_REQUEST"
  hash_key                    = "id"
  deletion_protection_enabled = var.deletion_protection

  attribute {
    name = "id"
    type = "S"
  }

  point_in_time_recovery {
    enabled = var.point_in_time_recovery
  }

  server_side_encryption {
    enabled = true
  }
}

# Least-privilege policy for whoever signs the workflow requests (SigV4).
# Attach it to the role/user the application runs with.
data "aws_iam_policy_document" "orders_put_item" {
  statement {
    sid       = "OrdersPutItem"
    actions   = ["dynamodb:PutItem"]
    resources = [aws_dynamodb_table.orders.arn]
  }
}

resource "aws_iam_policy" "orders_put_item" {
  name        = "${var.table_name}-put-item"
  description = "Allows the owscloud dynamodb-put-item workflow to write to the ${var.table_name} table."
  policy      = data.aws_iam_policy_document.orders_put_item.json
}
