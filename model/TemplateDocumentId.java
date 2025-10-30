package com.example.Docusign.model;

import java.io.Serializable;
import java.util.Objects;

public class TemplateDocumentId implements Serializable {
    private String templateId;
    private String documentId;

    public TemplateDocumentId() {}
    public TemplateDocumentId(String templateId, String documentId) {
        this.templateId = templateId;
        this.documentId = documentId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TemplateDocumentId that = (TemplateDocumentId) o;
        return Objects.equals(templateId, that.templateId) && Objects.equals(documentId, that.documentId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(templateId, documentId);
    }
}
