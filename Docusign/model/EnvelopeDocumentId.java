package com.example.Docusign.model;

import java.io.Serializable;
import java.util.Objects;

public class EnvelopeDocumentId implements Serializable {
    private String envelopeId;
    private String documentId;

    public EnvelopeDocumentId() {}
    public EnvelopeDocumentId(String envelopeId, String documentId) {
        this.envelopeId = envelopeId;
        this.documentId = documentId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EnvelopeDocumentId that = (EnvelopeDocumentId) o;
        return Objects.equals(envelopeId, that.envelopeId) && Objects.equals(documentId, that.documentId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(envelopeId, documentId);
    }
}
