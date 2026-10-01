package com.archery.range.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** 认领审核某一轮的三项分别结论，追加后不修改。 */
@Entity
@Table(name = "lost_found_review")
public class LostFoundReview {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "claim_id", nullable = false) private Long claimId;
    @Column(name = "item_id", nullable = false) private Long itemId;
    @Column(name = "review_round", nullable = false) private Integer reviewRound;
    @Column(name = "feature_conclusion", nullable = false, length = 16) private String featureConclusion;
    @Column(name = "time_place_conclusion", nullable = false, length = 16) private String timePlaceConclusion;
    @Column(name = "proof_conclusion", nullable = false, length = 16) private String proofConclusion;
    @Column(nullable = false, length = 16) private String result;
    @Column(length = 500) private String note;
    @Column(name = "reviewed_by", nullable = false, length = 32) private String reviewedBy;
    @Column(name = "reviewed_at", nullable = false) private LocalDateTime reviewedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getClaimId() { return claimId; }
    public void setClaimId(Long claimId) { this.claimId = claimId; }
    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }
    public Integer getReviewRound() { return reviewRound; }
    public void setReviewRound(Integer reviewRound) { this.reviewRound = reviewRound; }
    public String getFeatureConclusion() { return featureConclusion; }
    public void setFeatureConclusion(String featureConclusion) { this.featureConclusion = featureConclusion; }
    public String getTimePlaceConclusion() { return timePlaceConclusion; }
    public void setTimePlaceConclusion(String timePlaceConclusion) { this.timePlaceConclusion = timePlaceConclusion; }
    public String getProofConclusion() { return proofConclusion; }
    public void setProofConclusion(String proofConclusion) { this.proofConclusion = proofConclusion; }
    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public String getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(String reviewedBy) { this.reviewedBy = reviewedBy; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }
}
