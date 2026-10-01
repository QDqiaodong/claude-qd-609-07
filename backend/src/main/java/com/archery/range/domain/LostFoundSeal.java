package com.archery.range.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** 失物封签每次封存 / 开封的不可变记录。 */
@Entity
@Table(name = "lost_found_seal")
public class LostFoundSeal {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "item_id", nullable = false) private Long itemId;
    @Column(name = "seal_no", nullable = false, length = 32) private String sealNo;
    @Column(nullable = false, length = 16) private String action;
    @Column(name = "before_status", length = 16) private String beforeStatus;
    @Column(name = "after_status", nullable = false, length = 16) private String afterStatus;
    @Column(nullable = false, length = 32) private String custodian;
    @Column(name = "storage_area", nullable = false, length = 64) private String storageArea;
    @Column(nullable = false, length = 500) private String reason;
    @Column(nullable = false, length = 32) private String operator;
    @Column(name = "operator_role", nullable = false, length = 16) private String operatorRole;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }
    public String getSealNo() { return sealNo; }
    public void setSealNo(String sealNo) { this.sealNo = sealNo; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getBeforeStatus() { return beforeStatus; }
    public void setBeforeStatus(String beforeStatus) { this.beforeStatus = beforeStatus; }
    public String getAfterStatus() { return afterStatus; }
    public void setAfterStatus(String afterStatus) { this.afterStatus = afterStatus; }
    public String getCustodian() { return custodian; }
    public void setCustodian(String custodian) { this.custodian = custodian; }
    public String getStorageArea() { return storageArea; }
    public void setStorageArea(String storageArea) { this.storageArea = storageArea; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }
    public String getOperatorRole() { return operatorRole; }
    public void setOperatorRole(String operatorRole) { this.operatorRole = operatorRole; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
