package com.Group13_65CNTT.AutoGarage.repair.service;

import com.Group13_65CNTT.AutoGarage.reception.entity.AttachmentCategory;
import com.Group13_65CNTT.AutoGarage.repair.entity.RepairOrderStatus;

import java.util.EnumSet;
import java.util.Set;

import static com.Group13_65CNTT.AutoGarage.repair.entity.RepairOrderStatus.*;

/**
 * Which repair-order stage may still change which data.
 * Data of a finished stage becomes a read-only record; see docs/REPAIR_ORDER_LIFECYCLE.md.
 */
public final class RepairStageRules {

    /** Fuel level, customer complaint and the check-in report. */
    public static final Set<RepairOrderStatus> RECEPTION_EDITABLE = EnumSet.of(NEW);

    /** The diagnosis text is the basis of the first estimate, so it freezes once one exists. */
    public static final Set<RepairOrderStatus> DIAGNOSIS_EDITABLE = EnumSet.of(NEW, DIAGNOSING);

    /** Initial estimates (new versions included) can only be drafted before the customer approves anything. */
    public static final Set<RepairOrderStatus> INITIAL_ESTIMATE_ALLOWED = EnumSet.of(NEW, DIAGNOSING, WAITING_APPROVAL);

    /** After approval, scope or price changes go through a supplemental estimate while work is still open. */
    public static final Set<RepairOrderStatus> SUPPLEMENTAL_ALLOWED = EnumSet.of(APPROVED, IN_PROGRESS, WAITING_PARTS, QC_FAILED);

    /** Lead technician can be changed until the work has passed QC. */
    public static final Set<RepairOrderStatus> ASSIGNMENT_EDITABLE =
            EnumSet.of(NEW, DIAGNOSING, WAITING_APPROVAL, APPROVED, IN_PROGRESS, WAITING_PARTS, WAITING_QC, QC_FAILED);

    /** Statuses after which nothing on the order may change any more. */
    public static final Set<RepairOrderStatus> TERMINAL = EnumSet.of(INVOICED, CLOSED, CANCELLED);

    private RepairStageRules() {
    }

    /** Stages in which a photo of the given category may still be uploaded to a repair order. */
    public static Set<RepairOrderStatus> attachmentWindow(AttachmentCategory category) {
        return switch (category) {
            case CHECK_IN -> RECEPTION_EDITABLE;
            case DIAGNOSIS -> DIAGNOSIS_EDITABLE;
            case QC -> EnumSet.of(WAITING_QC, QC_FAILED, COMPLETED);
            case HANDOVER -> EnumSet.of(COMPLETED, INVOICED, CLOSED);
            case WARRANTY -> EnumSet.noneOf(RepairOrderStatus.class);
        };
    }

    /** Vietnamese name of the stage a category belongs to, for error messages. */
    public static String attachmentStageLabel(AttachmentCategory category) {
        return switch (category) {
            case CHECK_IN -> "tiếp nhận xe";
            case DIAGNOSIS -> "chẩn đoán";
            case QC -> "kiểm tra chất lượng";
            case HANDOVER -> "bàn giao";
            case WARRANTY -> "bảo hành";
        };
    }

    public static String attachmentCategoryLabel(AttachmentCategory category) {
        return switch (category) {
            case CHECK_IN -> "nhận xe";
            case DIAGNOSIS -> "chẩn đoán";
            case QC -> "QC";
            case HANDOVER -> "bàn giao";
            case WARRANTY -> "bảo hành";
        };
    }
}
