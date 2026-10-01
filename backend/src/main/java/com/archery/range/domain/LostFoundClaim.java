package com.archery.range.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** 认领申请与最终双人交还事实；历史审核与补证另表追加。 */
@Entity
@Table(name = "lost_found_claim")
public class LostFoundClaim {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "claim_no", nullable = false, unique = true, length = 24) private String claimNo;
    @Column(name = "item_id", nullable = false) private Long itemId;
    @Column(name = "claimant_type", nullable = false, length = 16) private String claimantType;
    @Column(name = "claimant_member_id") private Long claimantMemberId;
    @Column(name = "claimant_name", nullable = false, length = 32) private String claimantName;
    @Column(name = "claimant_contact", nullable = false, length = 64) private String claimantContact;
    @Column(nullable = false, length = 500) private String description;
    @Column(name = "lost_time_start", nullable = false) private LocalDateTime lostTimeStart;
    @Column(name = "lost_time_end", nullable = false) private LocalDateTime lostTimeEnd;
    @Column(name = "possible_areas", nullable = false, length = 300) private String possibleAreas;
    @Column(name = "privacy_feature", nullable = false, length = 300) private String privacyFeature;
    @Column(name = "evidence_refs", nullable = false, length = 1000) private String evidenceRefs;
    @Column(nullable = false, length = 16) private String status;
    @Column(name = "submitted_by", nullable = false, length = 32) private String submittedBy;
    @Column(name = "submitted_at", nullable = false) private LocalDateTime submittedAt;
    @Column(name = "actual_receiver", length = 32) private String actualReceiver;
    @Column(name = "receiver_relation", length = 32) private String receiverRelation;
    @Column(name = "id_verify_method", length = 64) private String idVerifyMethod;
    @Column(name = "return_custodian", length = 32) private String returnCustodian;
    @Column(name = "return_witness", length = 32) private String returnWitness;
    @Column(name = "returned_at") private LocalDateTime returnedAt;
    @Column(name = "return_note", length = 500) private String returnNote;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getClaimNo() { return claimNo; }
    public void setClaimNo(String claimNo) { this.claimNo = claimNo; }
    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }
    public String getClaimantType() { return claimantType; }
    public void setClaimantType(String claimantType) { this.claimantType = claimantType; }
    public Long getClaimantMemberId() { return claimantMemberId; }
    public void setClaimantMemberId(Long claimantMemberId) { this.claimantMemberId = claimantMemberId; }
    public String getClaimantName() { return claimantName; }
    public void setClaimantName(String claimantName) { this.claimantName = claimantName; }
    public String getClaimantContact() { return claimantContact; }
    public void setClaimantContact(String claimantContact) { this.claimantContact = claimantContact; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDateTime getLostTimeStart() { return lostTimeStart; }
    public void setLostTimeStart(LocalDateTime lostTimeStart) { this.lostTimeStart = lostTimeStart; }
    public LocalDateTime getLostTimeEnd() { return lostTimeEnd; }
    public void setLostTimeEnd(LocalDateTime lostTimeEnd) { this.lostTimeEnd = lostTimeEnd; }
    public String getPossibleAreas() { return possibleAreas; }
    public void setPossibleAreas(String possibleAreas) { this.possibleAreas = possibleAreas; }
    public String getPrivacyFeature() { return privacyFeature; }
    public void setPrivacyFeature(String privacyFeature) { this.privacyFeature = privacyFeature; }
    public String getEvidenceRefs() { return evidenceRefs; }
    public void setEvidenceRefs(String evidenceRefs) { this.evidenceRefs = evidenceRefs; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getSubmittedBy() { return submittedBy; }
    public void setSubmittedBy(String submittedBy) { this.submittedBy = submittedBy; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }
    public String getActualReceiver() { return actualReceiver; }
    public void setActualReceiver(String actualReceiver) { this.actualReceiver = actualReceiver; }
    public String getReceiverRelation() { return receiverRelation; }
    public void setReceiverRelation(String receiverRelation) { this.receiverRelation = receiverRelation; }
    public String getIdVerifyMethod() { return idVerifyMethod; }
    public void setIdVerifyMethod(String idVerifyMethod) { this.idVerifyMethod = idVerifyVerifyMethod; }
    public String getReturnCustodian() { return returnCustodian; }
    public void setReturnCustodian(String returnCustodian) { this.returnCustodian = returnCustodian; }
    public String getReturnWitness() { return returnWitness; }
    public void setReturnWitness(String returnWitness) { this.returnWitness = returnWitness; }
    public LocalDateTime getReturnedAt() { return returnedAt; }
    public void setReturnedAt(LocalDateTime returnedAt) { this.returnedAt = returnedAt; }
    public String getReturnNote() { return returnNote; }
    public void setReturnNote(String returnNote) { this.returnNote = returnNote; }
}
