package com.example.Docusign.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@IdClass(TemplateDocumentId.class)
@Table(name = "template_documents", indexes = {
    @Index(name = "idx_tpl_docs_template", columnList = "template_id"),
    @Index(name = "idx_tpl_docs_document", columnList = "document_id")
})
public class TemplateDocument {

    @Id
    @Column(name = "template_id", nullable = false)
    private String templateId;

    @Id
    @Column(name = "document_id", nullable = false)
    private String documentId; // FK -> documents.id

    @Column(name = "sequence_no", nullable = false)
    private Integer sequenceNo = 1;

    public String getTemplateId() { return templateId; }
    public void setTemplateId(String templateId) { this.templateId = templateId; }
    public String getDocumentId() { return documentId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }
    public Integer getSequenceNo() { return sequenceNo; }
    public void setSequenceNo(Integer sequenceNo) { this.sequenceNo = sequenceNo; }
}
