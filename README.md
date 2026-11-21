# owscloud

This project uses Quarkus, the Supersonic Subatomic Java Framework.

If you want to learn more about Quarkus, please visit its website: <https://quarkus.io/>.

## Running the application in dev mode

You can run your application in dev mode that enables live coding using:

```shell script
./mvnw quarkus:dev
```

> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at <http://localhost:8080/q/dev/>.

## Executing the workflow

With the application running, start an instance of the workflow with:

```shell script
curl -X POST http://localhost:8080/workflows/exec \
  -H 'Content-Type: application/json' \
  -d '{
    "order": {
      "id": "o-1001",
      "lines": [
        { "sku": "BOOK-1", "qty": 1 },
        { "sku": "PEN-7", "qty": 3 }
      ]
    }
  }'
```

### DynamoDB `PutItem` workflow on LocalStack

`src/main/resources/flow/dynamo-put-item.yaml` reads the DynamoDB endpoint from the configuration with
`${ $secret.aws.dynamodb.endpoint }` (secret `aws` declared in `use.secrets`): Quarkus Flow resolves `$secret.aws` from
the config properties prefixed with `aws.`. The endpoint is `https://dynamodb.us-east-1.amazonaws.com/` by default,
`http://localhost:4566/` (LocalStack) in dev and test, and can be overridden with `AWS_DYNAMODB_ENDPOINT`.

The workflow DSL has no SigV4 authentication, so `AwsSigV4ClientFilter` signs every request that carries the
`X-Aws-SigV4-Service` header (removed before sending). `@Provider` filters are not applied to the Quarkus Flow HTTP
clients and their configuration cannot register providers, so the workflow is routed to the named client `aws`
(`quarkus.flow.http.client.workflow.dynamodb-put-item.name=aws`) and the filter is registered on that client at
startup. Configuration (`application.properties`):

- `owscloud.aws.workflows`: workflows (`namespace:name:version`) whose client gets the filter;

- `owscloud.aws.region`: signing region (`us-east-1`);
- `owscloud.aws.access-key-id` / `owscloud.aws.secret-access-key`: static keys (`test`/`test` in dev and test);
  when unset, the AWS default credentials chain is used (env vars, `~/.aws`, IAM role).

The real table, the `orders-put-item` IAM policy and the `orders-app` IAM role (with only that policy) are created by
the Terraform in `terraform/`. Run the application with the role, not with a long-lived access key: on AWS, use it as
the task role / instance profile (`app_trusted_services`, e.g. `ecs-tasks.amazonaws.com`); locally, add a profile that
assumes it with your credentials (only the identity that ran `terraform apply` is trusted by default, see
`app_trusted_principals`):

```ini
# ~/.aws/config
[profile owscloud-app]
role_arn = <terraform output app_role_arn>
source_profile = default
region = us-east-1
```

```shell script
AWS_PROFILE=owscloud-app java -jar target/quarkus-app/quarkus-run.jar
```

Run the packaged application (prod profile) or dev mode against the real table with:

```shell script
./mvnw quarkus:dev -Dquarkus.profile=dev,real-aws
```

### Running on LocalStack

LocalStack is started by Dev Services for Amazon Services (`quarkus-amazon-dynamodb`) in dev and test modes:

- image `localstack/localstack:4.13` (the latest free community image, no `LOCALSTACK_AUTH_TOKEN` needed);
- fixed port `4566` (`quarkus.aws.devservices.localstack.port`), because the endpoint is literal in the workflow,
  so only one dev/test instance can run at a time;
- table `orders` (partition key `id`, type `S`) created by `src/test/resources/localstack-init/create-orders-table.sh`.

`AmzJsonMessageBodyReader` lets the Quarkus Flow HTTP client read `application/x-amz-json-1.0` responses; without it
the item is written but the workflow fails with
`Response could not be mapped to type class com.fasterxml.jackson.databind.JsonNode for response with media type application/x-amz-json-1.0`.
The HTTP calls of the workflow, including response bodies, are logged (`quarkus.flow.http.client.logging.scope=all`).

Run the integration test (needs Docker or Podman, and port 4566 free):

```shell script
./mvnw test -Dtest=DynamoPutItemWorkflowTest
```

Or start `./mvnw quarkus:dev` and run the workflow; the output is the input (`ResultPath: null`):

```shell script
curl -X POST http://localhost:8080/workflows/exec/dynamo-put-item \
  -H 'Content-Type: application/json' \
  -d '{
    "id": "order-1001",
    "total": "42.50"
  }'
```

Check the item (any credentials work with LocalStack):

```shell script
AWS_ACCESS_KEY_ID=test AWS_SECRET_ACCESS_KEY=test AWS_DEFAULT_REGION=us-east-1 \
  aws dynamodb get-item --endpoint-url http://localhost:4566 \
  --table-name orders --key '{"id": {"S": "order-1001"}}'
```

## Packaging and running the application

The application can be packaged using:

```shell script
./mvnw package
```

It produces the `quarkus-run.jar` file in the `target/quarkus-app/` directory.
Be aware that it’s not an _über-jar_ as the dependencies are copied into the `target/quarkus-app/lib/` directory.

The application is now runnable using `java -jar target/quarkus-app/quarkus-run.jar`.

If you want to build an _über-jar_, execute the following command:

```shell script
./mvnw package -Dquarkus.package.jar.type=uber-jar
```

The application, packaged as an _über-jar_, is now runnable using `java -jar target/*-runner.jar`.

## Creating a native executable

You can create a native executable using:

```shell script
./mvnw package -Dnative
```

Or, if you don't have GraalVM installed, you can run the native executable build in a container using:

```shell script
./mvnw package -Dnative -Dquarkus.native.container-build=true
```

You can then execute your native executable with: `./target/owscloud-1.0.0-SNAPSHOT-runner`

If you want to learn more about building native executables, please consult <https://quarkus.io/guides/maven-tooling>.

## Provided Code

### REST

Easily start your REST Web Services

[Related guide section...](https://quarkus.io/guides/getting-started-reactive#reactive-jax-rs-resources)
