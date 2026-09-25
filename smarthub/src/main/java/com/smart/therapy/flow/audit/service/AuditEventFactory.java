package com.smart.therapy.flow.audit.service;

import org.springframework.stereotype.Component;

@Component
public class AuditEventFactory {

    public String documentUploadDetails(String fileName, long fileSize, String category, String documentType, String source) {
        return String.format(
                "document_upload fileName=%s fileSize=%d category=%s documentType=%s source=%s",
                fileName, fileSize, category, documentType, source);
    }
}
