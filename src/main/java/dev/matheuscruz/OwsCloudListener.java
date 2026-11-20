package dev.matheuscruz;

import io.serverlessworkflow.impl.lifecycle.TaskStartedEvent;
import io.serverlessworkflow.impl.lifecycle.WorkflowExecutionListener;
import io.serverlessworkflow.impl.lifecycle.WorkflowStartedEvent;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class OwsCloudListener implements WorkflowExecutionListener {

    @Override
    public void onWorkflowStarted(WorkflowStartedEvent ev) {
        System.out.println("workflow started: " + ev.workflowContext().definition().id());
    }

    @Override
    public void onTaskStarted(TaskStartedEvent ev) {
        System.out.println("task started: " + ev.taskContext().taskName());
    }
}
