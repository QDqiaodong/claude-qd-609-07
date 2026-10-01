package com.archery.range.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** 逾期物品处置结果与接收方凭证。 */
@Entity
@Table(name = "lost_found_disposal")
public class LostFoundDisposal {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "disposal_no", nullable = false, unique = true, length = 24) private String disposalNo;
    @Column(name = "item_id", nullable = false) private Long itemId;
    @Column(nullable = false, length = 16) private String method;
    @Column(name = "disposal_path", nullable = false, length = 24) private String disposalPath;
    @Column(name = "receiver_name", nullable = false, length = 64) private String receiverName;
    @Column(name = "credential_no", nullable = false, length = 64) private String credentialNo;
    @Column(name = "credential_refs", nullable = false, length = 1000) private String credentialRefs;
    @Column(length = 500) private String note;
    @Column(name = "disposed_by", nullable = false, length = 32) private String disposedBy;
    @Column(name = "disposed_at", nullable = false) private LocalDateTime disposedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getDisposalNo() { return disposalNo; }
    public void setDisposalNo(String disposalNo) { this.disposalNo = disposalNo; }
    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }
    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }
    public String getDisposalPath() { return disposalPath; }
    public void setDisposalPath(String disposalPath) { this.disposalPath = disposalPath; }
    public String getReceiverName() { return receiverName; }
    public void setReceiverName(String receiverName) { this.receiverName = receiverName; }
    public String getCredentialNo() { return credentialNo; }
    public void setCredentialNo(String credentialNo) { this.credentialNo = credentialNo; }
    public String getCredentialRefs() { return credentialRefs; }
    public void setCredentialRefs(String credentialRefs) { this.credentialRefs = credentialRefs; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public String getDisposedBy() { return disposedBy; }
    public void setDisposedBy(String disposedBy) { this.disposedBy = disposedBy; }
    public LocalDateTime getDisposedAt() { return disposedAt; }
    public void setDisposedAt(LocalDateTime disposedAt) { this.disposedAt = disposedAt; }
}
