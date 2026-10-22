package com.Group13_65CNTT.AutoGarage.vehicle.dto;

import com.Group13_65CNTT.AutoGarage.reception.dto.AttachmentResponse;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class VehicleHistoryEntryDto {

    private Long repairOrderId;
    private String orderCode;
    private Instant receivedAt;
    private Instant completedAt;
    private Integer odometerIn;
    private String customerComplaint;
    private String diagnosis;
    private String status;
    private Long orderCustomerId;
    private boolean isCurrentOwnerRecord;
    private BigDecimal totalAmount; // Hidden if viewed by a new owner (BR04)
    private List<String> tasksDone = new ArrayList<>();
    private List<String> partsReplaced = new ArrayList<>();
    private List<AttachmentResponse> technicalAttachments = new ArrayList<>();

    public Long getRepairOrderId() {
        return repairOrderId;
    }

    public void setRepairOrderId(Long repairOrderId) {
        this.repairOrderId = repairOrderId;
    }

    public String getOrderCode() {
        return orderCode;
    }

    public void setOrderCode(String orderCode) {
        this.orderCode = orderCode;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(Instant receivedAt) {
        this.receivedAt = receivedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public Integer getOdometerIn() {
        return odometerIn;
    }

    public void setOdometerIn(Integer odometerIn) {
        this.odometerIn = odometerIn;
    }

    public String getCustomerComplaint() {
        return customerComplaint;
    }

    public void setCustomerComplaint(String customerComplaint) {
        this.customerComplaint = customerComplaint;
    }

    public String getDiagnosis() {
        return diagnosis;
    }

    public void setDiagnosis(String diagnosis) {
        this.diagnosis = diagnosis;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getOrderCustomerId() {
        return orderCustomerId;
    }

    public void setOrderCustomerId(Long orderCustomerId) {
        this.orderCustomerId = orderCustomerId;
    }

    public boolean isCurrentOwnerRecord() {
        return isCurrentOwnerRecord;
    }

    public void setCurrentOwnerRecord(boolean currentOwnerRecord) {
        isCurrentOwnerRecord = currentOwnerRecord;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public List<String> getTasksDone() {
        return tasksDone;
    }

    public void setTasksDone(List<String> tasksDone) {
        this.tasksDone = tasksDone;
    }

    public List<String> getPartsReplaced() {
        return partsReplaced;
    }

    public void setPartsReplaced(List<String> partsReplaced) {
        this.partsReplaced = partsReplaced;
    }

    public List<AttachmentResponse> getTechnicalAttachments() {
        return technicalAttachments;
    }

    public void setTechnicalAttachments(List<AttachmentResponse> technicalAttachments) {
        this.technicalAttachments = technicalAttachments;
    }
}
