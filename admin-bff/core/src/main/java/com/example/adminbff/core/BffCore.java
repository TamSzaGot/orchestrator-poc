package com.example.adminbff.core;

public final class BffCore {

    private BffCore() {
    }

    public static boolean isValidTrigger(String submissionId, String partnerId) {
        return submissionId != null && !submissionId.isBlank()
                && partnerId != null && !partnerId.isBlank();
    }
}
