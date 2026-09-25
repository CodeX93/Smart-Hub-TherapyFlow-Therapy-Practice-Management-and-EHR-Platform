package com.smart.therapy.flow.document.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionNoteFieldEntriesResponse {

    private String fieldName;
    private List<LibraryEntryResponse> libraryEntries;
    private List<AiTemplateOption> aiTemplateOptions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AiTemplateOption {
        private String key;
        private String label;
        private String template;
    }
}