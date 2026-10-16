package com.Group13_65CNTT.AutoGarage.reception.dto;

import com.Group13_65CNTT.AutoGarage.reception.entity.CheckResult;
import com.Group13_65CNTT.AutoGarage.reception.entity.CheckType;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class VehicleCheckResponse {

    private Long id;
    private Long repairOrderId;
    private CheckType checkType;
    private Integer attemptNo;
    private CheckResult result;
    private String checklistJson;
    private Integer schemaVersion;
    private Integer odometer;
    private String notes;
    private Long checkedBy;
    private Instant checkedAt;
    private List<AttachmentResponse> attachments = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRepairOrderId() {
        return repairOrderId;
    }

    public void setRepairOrderId(Long repairOrderId) {
        this.repairOrderId = repairOrderId;
    }

    public CheckType getCheckType() {
        return checkType;
    }

    public void setCheckType(CheckType checkType) {
        this.checkType = checkType;
    }

    public Integer getAttemptNo() {
        return attemptNo;
    }

    public void setAttemptNo(Integer attemptNo) {
        this.attemptNo = attemptNo;
    }

    public CheckResult getResult() {
        return result;
    }

    public void setResult(CheckResult result) {
        this.result = result;
    }

    public String getChecklistJson() {
        return checklistJson;
    }

    public void setChecklistJson(String checklistJson) {
        this.checklistJson = checklistJson;
    }

    public Integer getSchemaVersion() {
        return schemaVersion;
    }

    public void setSchemaVersion(Integer schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

    public Integer getOdometer() {
        return odometer;
    }

    public void setOdometer(Integer odometer) {
        this.odometer = odometer;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Long getCheckedBy() {
        return checkedBy;
    }

    public void setCheckedBy(Long checkedBy) {
        this.checkedBy = checkedBy;
    }

    public Instant getCheckedAt() {
        return checkedAt;
    }

    public void setCheckedAt(Instant checkedAt) {
        this.checkedAt = checkedAt;
    }

    public List<AttachmentResponse> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<AttachmentResponse> attachments) {
        this.attachments = attachments;
    }
}
