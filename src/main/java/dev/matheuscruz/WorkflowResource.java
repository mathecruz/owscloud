package dev.matheuscruz;

import io.quarkiverse.flow.Flow;
import io.serverlessworkflow.impl.WorkflowModel;
import io.smallrye.common.annotation.Identifier;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

@Path("/workflows/exec")
public class WorkflowResource {

    @Inject
    @Identifier("default:pass-and-data-flow:0.1.0")
    Flow passAndData;

    @POST
    public Uni<WorkflowModel> execute(PassAndDataFlowRequest request) {
        return passAndData.startInstance(request);
    }

    @Inject
    @Identifier("default:dynamodb-put-item:0.1.0")
    Flow dynamoPutItem;

    @POST
    @Path("/dynamo-put-item")
    public Uni<WorkflowModel> executeDynamoPutItem(DynamoPutItemRequest request) {
        return dynamoPutItem.startInstance(request);
    }
}
