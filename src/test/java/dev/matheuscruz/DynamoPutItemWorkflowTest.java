package dev.matheuscruz;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runs the dynamodb-put-item workflow against LocalStack, started by Dev Services on port 4566
 * with the orders table created by src/test/resources/localstack-init.
 */
@QuarkusTest
class DynamoPutItemWorkflowTest {

    static final Map<String, AttributeValue> KEY = Map.of("id", AttributeValue.fromS("order-1001"));

    @Inject
    DynamoDbClient dynamoDb;

    @BeforeEach
    void deleteItem() {
        dynamoDb.deleteItem(r -> r.tableName("orders").key(KEY));
    }

    @Test
    void putsItemAndReturnsInput() {
        given()
                .contentType("application/json")
                .body("""
                        {"id": "order-1001", "total": "42.50"}
                        """)
                .when().post("/workflows/exec/dynamo-put-item")
                .then()
                .statusCode(200)
                .body("id", is("order-1001"))
                .body("total", is("42.50"))
                .body("size()", is(2));

        GetItemResponse item = dynamoDb.getItem(r -> r.tableName("orders").key(KEY).consistentRead(true));
        assertTrue(item.hasItem());
        assertEquals("order-1001", item.item().get("id").s());
        // DynamoDB normalizes numbers: "42.50" is stored as 42.5
        assertEquals("42.5", item.item().get("total").n());
    }
}
