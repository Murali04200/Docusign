package com.example.Docusign.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@IdClass(EnvelopeDocumentId.class)
@Table(name = "envelope_documents", indexes = {
    @Index(name = "idx_env_docs_envelope", columnList = "envelope_id"),
    @Index(name = "idx_env_docs_document", columnList = "document_id")
})
public class EnvelopeDocument {

    @Id
    @Column(name = "envelope_id", nullable = false)
    private String envelopeId;

    @Id
    @Column(name = "document_id", nullable = false)
    private String documentId;

    @Column(name = "sequence_no", nullable = false)
    private Integer sequenceNo = 1;

    public String getEnvelopeId() { return envelopeId; }
    public void setEnvelopeId(String envelopeId) { this.envelopeId = envelopeId; }
    public String getDocumentId() { return documentId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }
    public Integer getSequenceNo() { return sequenceNo; }
    public void setSequenceNo(Integer sequenceNo) { this.sequenceNo = sequenceNo; }
}
