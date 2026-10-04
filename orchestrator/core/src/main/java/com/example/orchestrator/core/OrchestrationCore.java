package com.example.orchestrator.core;

public final class OrchestrationCore {

    private OrchestrationCore() {
    }

    public static TriggerDecision validateTriggerCommand(TriggerCommand command, boolean leader) {
        if (!leader) {
            return TriggerDecision.rejected("not_leader");
        }
        if (command.submissionId() == null || command.submissionId().isBlank()) {
            return TriggerDecision.rejected("submission_id_missing");
        }
        if (command.partnerId() == null || command.partnerId().isBlank()) {
            return TriggerDecision.rejected("partner_id_missing");
        }
        return TriggerDecision.accepted();
    }
}
