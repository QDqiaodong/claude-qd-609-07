package com.archery.range.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** 失物原登记与当前保管状态；原登记字段不通过编辑接口修改。 */
@Entity
@Table(name = "lost_found_item")
public class LostFoundItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "item_no", nullable = false, unique = true, length = 24)
    private String itemNo;
    @Column(nullable = false, length = 24)
    private String category;
    @Column(name = "public_category", nullable = false, length = 24)
    private String publicCategory;
    @Column(name = "found_area_type", nullable = false, length = 16)
    private String foundAreaType;
    @Column(name = "found_area_detail", nullable = false, length = 128)
    private String foundAreaDetail;
    @Column(name = "found_at", nullable = false)
    private LocalDateTime foundAt;
    @Column(nullable = false, length = 500)
    private String appearance;
    @Column(name = "photo_refs", nullable = false, length = 1000)
    private String photoRefs;
    @Column(name = "finder_type", nullable = false, length = 16)
    private String finderType;
    @Column(name = "finder_member_id")
    private Long finderMemberId;
    @Column(name = "finder_name", nullable = false, length = 32)
    private String finderName;
    @Column(name = "finder_contact", length = 64)
    private String finderContact;
    @Column(name = "current_custodian", nullable = false, length = 32)
    private String currentCustodian;
    @Column(name = "storage_area", nullable = false, length = 64)
    private String storageArea;
    @Column(name = "seal_no", nullable = false, length = 32)
    private String sealNo;
    @Column(name = "seal_status", nullable = false, length = 16)
    private String sealStatus;
    @Column(name = "sealed_at", nullable = false)
    private LocalDateTime sealedAt;
    @Column(name = "high_value", nullable = false)
    private Boolean highValue = false;
    @Column(name = "identity_item", nullable = false)
    private Boolean identityItem = false;
    @Column(nullable = false, length = 16)
    private String status;
    @Column(name = "retention_until", nullable = false)
    private LocalDate retentionUntil;
    @Column(name = "registered_by", nullable = false, length = 32)
    private String registeredBy;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    @Column(name = "returned_at")
    private LocalDateTime returnedAt;
    @Column(name = "disposed_at")
    private LocalDateTime disposedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getItemNo() { return itemNo; }
    public void setItemNo(String itemNo) { this.itemNo = itemNo; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getPublicCategory() { return publicCategory; }
    public void setPublicCategory(String publicCategory) { this.publicCategory = publicCategory; }
    public String getFoundAreaType() { return foundAreaType; }
    public void setFoundAreaType(String foundAreaType) { this.foundAreaType = foundAreaType; }
    public String getFoundAreaDetail() { return foundAreaDetail; }
    public void setFoundAreaDetail(String foundAreaDetail) { this.foundAreaDetail = foundAreaDetail; }
    public LocalDateTime getFoundAt() { return foundAt; }
    public void setFoundAt(LocalDateTime foundAt) { this.foundAt = foundAt; }
    public String getAppearance() { return appearance; }
    public void setAppearance(String appearance) { this.appearance = appearance; }
    public String getPhotoRefs() { return photoRefs; }
    public void setPhotoRefs(String photoRefs) { this.photoRefs = photoRefs; }
    public String getFinderType() { return finderType; }
    public void setFinderType(String finderType) { this.finderType = finderType; }
    public Long getFinderMemberId() { return finderMemberId; }
    public void setFinderMemberId(Long finderMemberId) { this.finderMemberId = finderMemberId; }
    public String getFinderName() { return finderName; }
    public void setFinderName(String finderName) { this.finderName = finderName; }
    public String getFinderContact() { return finderContact; }
    public void setFinderContact(String finderContact) { this.finderContact = finderContact; }
    public String getCurrentCustodian() { return currentCustodian; }
    public void setCurrentCustodian(String currentCustodian) { this.currentCustodian = currentCustodian; }
    public String getStorageArea() { return storageArea; }
    public void setStorageArea(String storageArea) { this.storageArea = storageArea; }
    public String getSealNo() { return sealNo; }
    public void setSealNo(String sealNo) { this.sealNo = sealNo; }
    public String getSealStatus() { return sealStatus; }
    public void setSealStatus(String sealStatus) { this.sealStatus = sealStatus; }
    public LocalDateTime getSealedAt() { return sealedAt; }
    public void setSealedAt(LocalDateTime sealedAt) { this.sealedAt = sealedAt; }
    public Boolean getHighValue() { return highValue; }
    public void setHighValue(Boolean highValue) { this.highValue = highValue; }
    public Boolean getIdentityItem() { return identityItem; }
    public void setIdentityItem(Boolean identityItem) { this.identityItem = identityItem; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDate getRetentionUntil() { return retentionUntil; }
    public void setRetentionUntil(LocalDate retentionUntil) { this.retentionUntil = retentionUntil; }
    public String getRegisteredBy() { return registeredBy; }
    public void setRegisteredBy(String registeredBy) { this.registeredBy = registeredBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getReturnedAt() { return returnedAt; }
    public void setReturnedAt(LocalDateTime returnedAt) { this.returnedAt = returnedAt; }
    public LocalDateTime getDisposedAt() { return disposedAt; }
    public void setDisposedAt(LocalDateTime disposedAt) { this.disposedAt = disposedAt; }
}
