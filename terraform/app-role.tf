# Role the application assumes to sign the workflow requests, so it runs with temporary,
# least-privilege credentials instead of a long-lived access key.
data "aws_caller_identity" "current" {}

data "aws_iam_policy_document" "app_assume_role" {
  statement {
    sid     = "TrustedPrincipals"
    actions = ["sts:AssumeRole"]

    principals {
      type = "AWS"
      # Default: only the identity running terraform (e.g. for local development)
      identifiers = length(var.app_trusted_principals) > 0 ? var.app_trusted_principals : [data.aws_caller_identity.current.arn]
    }
  }

  dynamic "statement" {
    for_each = length(var.app_trusted_services) > 0 ? [1] : []
    content {
      sid     = "TrustedServices"
      actions = ["sts:AssumeRole"]

      principals {
        type        = "Service"
        identifiers = var.app_trusted_services
      }
    }
  }
}

resource "aws_iam_role" "app" {
  name                 = "${var.table_name}-app"
  description          = "Assumed by owscloud to write to the ${var.table_name} table."
  assume_role_policy   = data.aws_iam_policy_document.app_assume_role.json
  max_session_duration = 3600
}

resource "aws_iam_role_policy_attachment" "app_orders_put_item" {
  role       = aws_iam_role.app.name
  policy_arn = aws_iam_policy.orders_put_item.arn
}
