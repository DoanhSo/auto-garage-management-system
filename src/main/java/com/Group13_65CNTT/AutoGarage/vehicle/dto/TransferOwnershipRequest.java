package com.Group13_65CNTT.AutoGarage.vehicle.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class TransferOwnershipRequest {

    @NotNull(message = "ID chủ xe mới không được để trống")
    private Long newCustomerId;

    @Size(max = 500, message = "Ghi chú chuyển nhượng tối đa 500 ký tự")
    private String transferNote;

    public Long getNewCustomerId() {
        return newCustomerId;
    }

    public void setNewCustomerId(Long newCustomerId) {
        this.newCustomerId = newCustomerId;
    }

    public String getTransferNote() {
        return transferNote;
    }

    public void setTransferNote(String transferNote) {
        this.transferNote = transferNote;
    }
}
