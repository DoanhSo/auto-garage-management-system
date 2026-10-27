package com.Group13_65CNTT.AutoGarage.repair.service.impl;

import com.Group13_65CNTT.AutoGarage.common.util.PaginationPolicy;
import com.Group13_65CNTT.AutoGarage.customer.entity.Customer;
import com.Group13_65CNTT.AutoGarage.customer.repository.CustomerRepository;
import com.Group13_65CNTT.AutoGarage.reception.entity.Booking;
import com.Group13_65CNTT.AutoGarage.reception.entity.BookingStatus;
import com.Group13_65CNTT.AutoGarage.reception.entity.CheckResult;
import com.Group13_65CNTT.AutoGarage.reception.entity.CheckType;
import com.Group13_65CNTT.AutoGarage.reception.entity.VehicleCheck;
import com.Group13_65CNTT.AutoGarage.reception.repository.BookingRepository;
import com.Group13_65CNTT.AutoGarage.reception.repository.VehicleCheckRepository;
import com.Group13_65CNTT.AutoGarage.vehicle.entity.Vehicle;
import com.Group13_65CNTT.AutoGarage.vehicle.repository.VehicleRepository;
import com.Group13_65CNTT.AutoGarage.repair.dto.request.DiagnosisUpdateRequest;
import com.Group13_65CNTT.AutoGarage.repair.dto.request.HandoverRequest;
import com.Group13_65CNTT.AutoGarage.repair.dto.request.RepairOrderCreateRequest;
import com.Group13_65CNTT.AutoGarage.repair.dto.request.RepairOrderStatusUpdateRequest;
import com.Group13_65CNTT.AutoGarage.repair.dto.request.RepairOrderUpdateRequest;
import com.Group13_65CNTT.AutoGarage.repair.dto.request.RepairTaskCreateRequest;
import com.Group13_65CNTT.AutoGarage.repair.dto.request.RepairTaskUpdateRequest;
import com.Group13_65CNTT.AutoGarage.repair.dto.response.RepairOrderResponse;
import com.Group13_65CNTT.AutoGarage.repair.dto.response.RepairTaskResponse;
import com.Group13_65CNTT.AutoGarage.repair.entity.OrderType;
import com.Group13_65CNTT.AutoGarage.repair.entity.RepairOrder;
import com.Group13_65CNTT.AutoGarage.repair.entity.RepairOrderStatus;
import com.Group13_65CNTT.AutoGarage.repair.entity.RepairTask;
import com.Group13_65CNTT.AutoGarage.repair.entity.TaskStatus;
import com.Group13_65CNTT.AutoGarage.repair.entity.TaskType;
import com.Group13_65CNTT.AutoGarage.repair.mapper.RepairOrderMapper;
import com.Group13_65CNTT.AutoGarage.repair.mapper.RepairTaskMapper;
import com.Group13_65CNTT.AutoGarage.repair.repository.LaborSessionRepository;
import com.Group13_65CNTT.AutoGarage.repair.repository.RepairOrderRepository;
import com.Group13_65CNTT.AutoGarage.repair.repository.RepairTaskRepository;
import com.Group13_65CNTT.AutoGarage.repair.service.RepairService;
import com.Group13_65CNTT.AutoGarage.shared.contracts.BayScheduling;
import com.Group13_65CNTT.AutoGarage.shared.contracts.BillingOperations;
import com.Group13_65CNTT.AutoGarage.shared.contracts.InventoryOperations;
import com.Group13_65CNTT.AutoGarage.shared.contracts.NotificationAudit;
import com.Group13_65CNTT.AutoGarage.shared.contracts.RepairWorkflow;
import com.Group13_65CNTT.AutoGarage.shared.contracts.WarrantyOperations;
import com.Group13_65CNTT.AutoGarage.shared.exception.BusinessRuleException;
import com.Group13_65CNTT.AutoGarage.shared.exception.ConflictException;
import com.Group13_65CNTT.AutoGarage.shared.exception.ResourceNotFoundException;
import com.Group13_65CNTT.AutoGarage.warranty.entity.ClaimStatus;
import com.Group13_65CNTT.AutoGarage.warranty.entity.Warranty;
import com.Group13_65CNTT.AutoGarage.warranty.entity.WarrantyClaim;
import com.Group13_65CNTT.AutoGarage.warranty.repository.WarrantyClaimRepository;
import com.Group13_65CNTT.AutoGarage.warranty.repository.WarrantyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class RepairServiceImpl implements RepairService, RepairWorkflow {

    private final RepairOrderRepository repairOrderRepository;
    private final RepairTaskRepository repairTaskRepository;
    private final LaborSessionRepository laborSessionRepository;
    private final RepairOrderMapper repairOrderMapper;
    private final RepairTaskMapper repairTaskMapper;
    private final PaginationPolicy pagination;

    private final VehicleRepository vehicleRepository;
    private final CustomerRepository customerRepository;
    private final BookingRepository bookingRepository;
    private final VehicleCheckRepository vehicleCheckRepository;
    private final WarrantyClaimRepository warrantyClaimRepository;
    private final WarrantyRepository warrantyRepository;

    private final BayScheduling bayScheduling;

    @Autowired(required = false)
    @org.springframework.context.annotation.Lazy
    private BillingOperations billingOperations;

    @Autowired(required = false)
    @org.springframework.context.annotation.Lazy
    private InventoryOperations inventoryOperations;

    @Autowired(required = false)
    private NotificationAudit notificationAudit;

    @Autowired(required = false)
    @org.springframework.context.annotation.Lazy
    private WarrantyOperations warrantyOperations;

    @Autowired(required = false)
    private com.Group13_65CNTT.AutoGarage.billing.repository.InvoiceRepository invoiceRepository;

    // =========================================================================
    // Contract I03 (RepairWorkflow)
    // =========================================================================

    @Override
    @Transactional
    public long receive(long actorUserId, Long bookingId, long customerId, long vehicleId, int odometer) {
        log.info("RepairWorkflow.receive: actor={}, booking={}, customer={}, vehicle={}, odo={}",
                actorUserId, bookingId, customerId, vehicleId, odometer);

        if (odometer < 0) {
            throw new BusinessRuleException("Số km khi vào xưởng phải >= 0");
        }

        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phương tiện với ID: " + vehicleId));
        if (Boolean.FALSE.equals(vehicle.getIsActive())) {
            throw new BusinessRuleException("Phương tiện đang bị ngừng hoạt động");
        }

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khách hàng với ID: " + customerId));
        if (Boolean.FALSE.equals(customer.getIsActive())) {
            throw new BusinessRuleException("Khách hàng đang bị khóa hoặc ngưng hoạt động");
        }

        if (bookingId != null) {
            Booking booking = bookingRepository.findById(bookingId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lịch hẹn với ID: " + bookingId));

            if (!booking.getVehicleId().equals(vehicleId)) {
                throw new BusinessRuleException("Lịch hẹn không khớp với xe tiếp nhận");
            }
            if (!booking.getCustomerId().equals(customerId)) {
                throw new BusinessRuleException("Lịch hẹn không khớp với khách hàng tiếp nhận");
            }
            if (repairOrderRepository.existsByBookingId(bookingId)) {
                throw new ConflictException("Lịch hẹn này đã được tiếp nhận tạo phiếu sửa chữa trước đó");
            }

            booking.setStatus(BookingStatus.RECEIVED);
            bookingRepository.save(booking);
        }

        String orderCode = generateOrderCode("RO");

        RepairOrder order = RepairOrder.builder()
                .orderCode(orderCode)
                .bookingId(bookingId)
                .customerId(customerId)
                .vehicleId(vehicleId)
                .orderType(OrderType.NORMAL)
                .status(RepairOrderStatus.NEW)
                .odometerIn(odometer)
                .receivedBy(actorUserId)
                .receivedAt(Instant.now())
                .build();

        RepairOrder savedOrder = repairOrderRepository.save(order);

        // Tự động tạo task DIAGNOSIS nội bộ
        RepairTask diagnosisTask = RepairTask.builder()
                .repairOrderId(savedOrder.getId())
                .taskType(TaskType.DIAGNOSIS)
                .taskName("Khám xe và chẩn đoán ban đầu")
                .description("Chẩn đoán tổng thể phương tiện khi tiếp nhận vào xưởng")
                .status(TaskStatus.PENDING)
                .quantity(BigDecimal.ONE)
                .unitPrice(BigDecimal.ZERO)
                .build();
        repairTaskRepository.save(diagnosisTask);

        // Cập nhật ODO mới nhất cho xe
        if (vehicle.getOdometer() == null || odometer > vehicle.getOdometer()) {
            vehicle.setOdometer(odometer);
            vehicleRepository.save(vehicle);
        }

        log.info("Phiếu sửa chữa được tạo thành công: id={}, code={}", savedOrder.getId(), savedOrder.getOrderCode());
        return savedOrder.getId();
    }

    @Override
    @Transactional
    public long createWarrantyOrder(long actorUserId, long warrantyClaimId) {
        log.info("RepairWorkflow.createWarrantyOrder: actor={}, warrantyClaimId={}", actorUserId, warrantyClaimId);

        WarrantyClaim claim = warrantyClaimRepository.findById(warrantyClaimId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu bảo hành với ID: " + warrantyClaimId));

        if (claim.getHandlingOrderId() != null) {
            throw new ConflictException("Yêu cầu bảo hành này đã được gắn với phiếu sửa chữa xử lý: " + claim.getHandlingOrderId());
        }

        Warranty warranty = warrantyRepository.findById(claim.getWarrantyId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin bảo hành gốc với ID: " + claim.getWarrantyId()));

        String orderCode = generateOrderCode("WO");

        RepairOrder order = RepairOrder.builder()
                .orderCode(orderCode)
                .customerId(warranty.getCustomerId())
                .vehicleId(warranty.getVehicleId())
                .orderType(OrderType.WARRANTY)
                .status(RepairOrderStatus.NEW)
                .odometerIn(claim.getCurrentOdometer())
                .customerComplaint("Bảo hành: " + claim.getDescription())
                .receivedBy(actorUserId)
                .receivedAt(Instant.now())
                .build();

        RepairOrder savedOrder = repairOrderRepository.save(order);

        // Cập nhật claim
        claim.setHandlingOrderId(savedOrder.getId());
        claim.setStatus(ClaimStatus.IN_PROGRESS);
        warrantyClaimRepository.save(claim);

        // Tạo task DIAGNOSIS cho phiếu bảo hành
        RepairTask diagnosisTask = RepairTask.builder()
                .repairOrderId(savedOrder.getId())
                .taskType(TaskType.DIAGNOSIS)
                .taskName("Kiểm tra và xác minh lỗi bảo hành")
                .description("Đánh giá tình trạng kỹ thuật phục vụ xử lý yêu cầu bảo hành")
                .status(TaskStatus.PENDING)
                .quantity(BigDecimal.ONE)
                .unitPrice(BigDecimal.ZERO)
                .build();
        repairTaskRepository.save(diagnosisTask);

        log.info("Phiếu bảo hành được tạo thành công: id={}, code={}", savedOrder.getId(), savedOrder.getOrderCode());
        return savedOrder.getId();
    }

    @Override
    @Transactional(readOnly = true)
    public void requireInvoiceReady(long actorUserId, long repairOrderId) {
        log.info("RepairWorkflow.requireInvoiceReady: actor={}, repairOrderId={}", actorUserId, repairOrderId);

        RepairOrder order = repairOrderRepository.findById(repairOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiếu sửa chữa với ID: " + repairOrderId));

        if (order.getStatus() == RepairOrderStatus.CLOSED || order.getStatus() == RepairOrderStatus.CANCELLED) {
            throw new BusinessRuleException("Phiếu sửa chữa đã đóng hoặc bị hủy, không thể lập hóa đơn");
        }

        // 1. Kiểm tra tất cả tasks phải hoàn thành
        if (repairTaskRepository.existsByRepairOrderIdAndStatusNot(repairOrderId, TaskStatus.COMPLETED)) {
            throw new BusinessRuleException("Tất cả các hạng mục công việc phải hoàn thành trước khi lập hóa đơn");
        }

        // 2. Kiểm tra không còn session timer nào đang chạy
        if (laborSessionRepository.countOpenSessionsByRepairOrderId(repairOrderId) > 0) {
            throw new BusinessRuleException("Vẫn còn phiên làm việc (timer) của kỹ thuật viên chưa kết thúc");
        }

        // 3. Kiểm tra QC mới nhất phải PASSED
        if (order.getLatestQcCheckId() == null) {
            throw new BusinessRuleException("Phiếu sửa chữa chưa thực hiện kiểm tra chất lượng (QC)");
        }

        VehicleCheck qcCheck = vehicleCheckRepository.findById(order.getLatestQcCheckId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy biên bản kiểm tra chất lượng QC"));

        if (qcCheck.getCheckType() != CheckType.QC || qcCheck.getResult() != CheckResult.PASSED) {
            throw new BusinessRuleException("Biên bản kiểm tra chất lượng gần nhất chưa đạt yêu cầu (PASSED)");
        }
    }

    @Override
    @Transactional
    public void handOver(long actorUserId, long repairOrderId, int odometerOut, String requestKey) {
        log.info("RepairWorkflow.handOver: actor={}, repairOrderId={}, odoOut={}, requestKey={}",
                actorUserId, repairOrderId, odometerOut, requestKey);

        RepairOrder order = repairOrderRepository.findByIdForUpdate(repairOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiếu sửa chữa với ID: " + repairOrderId));

        if (order.getStatus() == RepairOrderStatus.CLOSED || order.getStatus() == RepairOrderStatus.CANCELLED) {
            throw new BusinessRuleException("Phiếu sửa chữa đã đóng hoặc bị hủy, không thể bàn giao xe");
        }

        if (odometerOut < order.getOdometerIn()) {
            throw new BusinessRuleException(String.format(
                    "Số km khi bàn giao (%d) không được nhỏ hơn số km vào xưởng (%d)",
                    odometerOut, order.getOdometerIn()));
        }

        // Kiểm tra điều kiện nợ / thanh toán nếu billing module đã sẵn sàng
        if (billingOperations != null) {
            billingOperations.requireHandoverAllowed(actorUserId, repairOrderId);
        }

        // Cập nhật phiếu sửa chữa
        order.setOdometerOut(odometerOut);
        order.setHandedOverBy(actorUserId);
        Instant now = Instant.now();
        order.setHandedOverAt(now);
        order.setClosedAt(now);
        order.setStatus(RepairOrderStatus.CLOSED);
        repairOrderRepository.save(order);

        // Cập nhật Odometer của xe
        vehicleRepository.findById(order.getVehicleId()).ifPresent(v -> {
            v.setOdometer(odometerOut);
            vehicleRepository.save(v);
        });

        // Giải phóng khoang sửa chữa
        bayScheduling.releaseForRepair(actorUserId, repairOrderId);

        // Giải phóng lượng giữ phụ tùng trong kho
        if (inventoryOperations != null) {
            inventoryOperations.releaseForOrder(actorUserId, repairOrderId);
        }

        // Ghi biên bản bàn giao xe
        VehicleCheck handoverCheck = new VehicleCheck();
        handoverCheck.setRepairOrderId(repairOrderId);
        handoverCheck.setCheckType(CheckType.HANDOVER);
        handoverCheck.setResult(CheckResult.RECORDED);
        handoverCheck.setOdometer(odometerOut);
        handoverCheck.setChecklistJson("{}");
        handoverCheck.setNotes("Bàn giao xe cho khách hàng và đóng phiếu sửa chữa");
        handoverCheck.setCheckedBy(actorUserId);
        handoverCheck.setCheckedAt(now);
        vehicleCheckRepository.save(handoverCheck);

        // Tạo chính sách bảo hành I07 nếu đã có hóa đơn phát hành cho phiếu
        if (warrantyOperations != null && invoiceRepository != null) {
            invoiceRepository.findByRepairOrderId(repairOrderId).ifPresent(inv -> {
                warrantyOperations.createFromInvoice(actorUserId, inv.getId(), odometerOut);
            });
        }

        // Ghi completed request nếu có notificationAudit
        if (notificationAudit != null && requestKey != null && !requestKey.isBlank()) {
            notificationAudit.recordCompletedRequest(actorUserId, "HANDOVER", "REPAIR_ORDER",
                    repairOrderId, requestKey, requestKey, "{\"status\":\"CLOSED\"}");
        }

        log.info("Phiếu sửa chữa #{} đã được bàn giao thành công và đóng phiếu", repairOrderId);
    }

    // =========================================================================
    // RepairService CRUD & State Transitions
    // =========================================================================

    @Override
    @Transactional
    public RepairOrderResponse createRepairOrder(Long actorUserId, RepairOrderCreateRequest request) {
        long orderId = receive(actorUserId, request.getBookingId(), request.getCustomerId(),
                request.getVehicleId(), request.getOdometerIn());
        RepairOrder order = repairOrderRepository.findById(orderId).orElseThrow();
        if (request.getFuelLevelPercent() != null) {
            order.setFuelLevelPercent(request.getFuelLevelPercent());
        }
        if (request.getCustomerComplaint() != null) {
            order.setCustomerComplaint(request.getCustomerComplaint());
        }
        order = repairOrderRepository.save(order);
        return repairOrderMapper.toResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public RepairOrderResponse getRepairOrderById(Long id) {
        RepairOrder order = repairOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiếu sửa chữa với ID: " + id));
        return repairOrderMapper.toResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public RepairOrderResponse getRepairOrderByCode(String orderCode) {
        RepairOrder order = repairOrderRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiếu sửa chữa với mã: " + orderCode));
        return repairOrderMapper.toResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RepairOrderResponse> searchRepairOrders(String keyword, RepairOrderStatus status,
                                                        int page, int size, String sortBy, String sortDir) {
        Pageable pageable = pagination.page(page, size, sortBy, sortDir,
                Set.of("id", "orderCode", "receivedAt", "status", "createdAt"));
        Page<RepairOrder> resultPage = repairOrderRepository.searchOrders(keyword, status, pageable);
        return resultPage.map(repairOrderMapper::toResponse);
    }

    @Override
    @Transactional
    public RepairOrderResponse updateRepairOrder(Long actorUserId, Long id, RepairOrderUpdateRequest request) {
        RepairOrder order = repairOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiếu sửa chữa với ID: " + id));

        if (order.getStatus() == RepairOrderStatus.CLOSED || order.getStatus() == RepairOrderStatus.CANCELLED) {
            throw new BusinessRuleException("Phiếu sửa chữa đã đóng hoặc bị hủy, không thể chỉnh sửa");
        }

        repairOrderMapper.updateEntityFromRequest(request, order);
        return repairOrderMapper.toResponse(repairOrderRepository.save(order));
    }

    @Override
    @Transactional
    public RepairOrderResponse updateDiagnosis(Long actorUserId, Long id, DiagnosisUpdateRequest request) {
        RepairOrder order = repairOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiếu sửa chữa với ID: " + id));

        if (order.getStatus() == RepairOrderStatus.CLOSED || order.getStatus() == RepairOrderStatus.CANCELLED) {
            throw new BusinessRuleException("Phiếu sửa chữa đã đóng hoặc bị hủy, không thể cập nhật chẩn đoán");
        }

        order.setDiagnosis(request.getDiagnosis());
        if (order.getStatus() == RepairOrderStatus.NEW) {
            order.setStatus(RepairOrderStatus.DIAGNOSING);
        }
        return repairOrderMapper.toResponse(repairOrderRepository.save(order));
    }

    @Override
    @Transactional
    public RepairOrderResponse transitionStatus(Long actorUserId, Long id, RepairOrderStatus newStatus, String note) {
        RepairOrder order = repairOrderRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiếu sửa chữa với ID: " + id));

        RepairOrderStatus current = order.getStatus();

        if (current == newStatus) {
            return repairOrderMapper.toResponse(order);
        }

        if (current == RepairOrderStatus.CLOSED || current == RepairOrderStatus.CANCELLED) {
            throw new BusinessRuleException("Phiếu sửa chữa đã kết thúc (" + current + "), không thể thay đổi trạng thái");
        }

        validateTransition(current, newStatus, order);

        order.setStatus(newStatus);
        return repairOrderMapper.toResponse(repairOrderRepository.save(order));
    }

    @Override
    @Transactional
    public RepairOrderResponse handOverOrder(Long actorUserId, Long id, HandoverRequest request) {
        handOver(actorUserId, id, request.getOdometerOut(), request.getRequestKey());
        return getRepairOrderById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RepairTaskResponse> getTasksByRepairOrderId(Long repairOrderId) {
        if (!repairOrderRepository.existsById(repairOrderId)) {
            throw new ResourceNotFoundException("Không tìm thấy phiếu sửa chữa với ID: " + repairOrderId);
        }
        return repairTaskRepository.findByRepairOrderId(repairOrderId).stream()
                .map(repairTaskMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public RepairTaskResponse createTask(Long actorUserId, Long repairOrderId, RepairTaskCreateRequest request) {
        RepairOrder order = repairOrderRepository.findById(repairOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiếu sửa chữa với ID: " + repairOrderId));

        if (order.getStatus() == RepairOrderStatus.CLOSED || order.getStatus() == RepairOrderStatus.CANCELLED) {
            throw new BusinessRuleException("Không thể thêm công việc vào phiếu sửa chữa đã đóng hoặc bị hủy");
        }

        RepairTask task = repairTaskMapper.toEntity(request);
        task.setRepairOrderId(repairOrderId);
        task.setTaskType(TaskType.REPAIR);
        task.setStatus(TaskStatus.PENDING);
        return repairTaskMapper.toResponse(repairTaskRepository.save(task));
    }

    @Override
    @Transactional
    public RepairTaskResponse updateTask(Long actorUserId, Long taskId, RepairTaskUpdateRequest request) {
        RepairTask task = repairTaskRepository.findByIdForUpdate(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy công việc với ID: " + taskId));

        RepairOrder order = repairOrderRepository.findById(task.getRepairOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiếu sửa chữa của công việc này"));

        if (order.getStatus() == RepairOrderStatus.CLOSED || order.getStatus() == RepairOrderStatus.CANCELLED) {
            throw new BusinessRuleException("Không thể chỉnh sửa công việc trong phiếu sửa chữa đã kết thúc");
        }

        repairTaskMapper.updateEntityFromRequest(request, task);
        if (request.getStatus() == TaskStatus.COMPLETED && task.getCompletedAt() == null) {
            task.setCompletedAt(Instant.now());
        }
        return repairTaskMapper.toResponse(repairTaskRepository.save(task));
    }

    @Override
    @Transactional
    public void deleteTask(Long actorUserId, Long taskId) {
        RepairTask task = repairTaskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy công việc với ID: " + taskId));

        RepairOrder order = repairOrderRepository.findById(task.getRepairOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiếu sửa chữa"));

        if (order.getStatus() == RepairOrderStatus.CLOSED || order.getStatus() == RepairOrderStatus.CANCELLED) {
            throw new BusinessRuleException("Không thể xóa công việc trong phiếu sửa chữa đã kết thúc");
        }

        if (task.getStatus() == TaskStatus.IN_PROGRESS || task.getStatus() == TaskStatus.COMPLETED) {
            throw new BusinessRuleException("Không thể xóa công việc đang thực hiện hoặc đã hoàn thành");
        }

        repairTaskRepository.delete(task);
    }

    // =========================================================================
    // Helper Methods
    // =========================================================================

    private void validateTransition(RepairOrderStatus from, RepairOrderStatus to, RepairOrder order) {
        if (to == RepairOrderStatus.CANCELLED) {
            if (from == RepairOrderStatus.COMPLETED || from == RepairOrderStatus.INVOICED) {
                throw new BusinessRuleException("Không thể hủy phiếu sửa chữa đã hoàn thành hoặc đã xuất hóa đơn");
            }
            return;
        }

        switch (from) {
            case NEW -> {
                if (to != RepairOrderStatus.DIAGNOSING && to != RepairOrderStatus.WAITING_APPROVAL) {
                    throwInvalidTransition(from, to);
                }
            }
            case DIAGNOSING -> {
                if (to != RepairOrderStatus.WAITING_APPROVAL) {
                    throwInvalidTransition(from, to);
                }
            }
            case WAITING_APPROVAL -> {
                if (to != RepairOrderStatus.APPROVED) {
                    throwInvalidTransition(from, to);
                }
            }
            case APPROVED -> {
                if (to != RepairOrderStatus.IN_PROGRESS && to != RepairOrderStatus.WAITING_PARTS) {
                    throwInvalidTransition(from, to);
                }
            }
            case IN_PROGRESS -> {
                if (to != RepairOrderStatus.WAITING_PARTS && to != RepairOrderStatus.WAITING_QC) {
                    throwInvalidTransition(from, to);
                }
            }
            case WAITING_PARTS -> {
                if (to != RepairOrderStatus.IN_PROGRESS) {
                    throwInvalidTransition(from, to);
                }
            }
            case WAITING_QC -> {
                if (to != RepairOrderStatus.QC_FAILED && to != RepairOrderStatus.COMPLETED) {
                    throwInvalidTransition(from, to);
                }
            }
            case QC_FAILED -> {
                if (to != RepairOrderStatus.IN_PROGRESS) {
                    throwInvalidTransition(from, to);
                }
            }
            case COMPLETED -> {
                if (to != RepairOrderStatus.INVOICED) {
                    throwInvalidTransition(from, to);
                }
            }
            case INVOICED -> {
                if (to != RepairOrderStatus.CLOSED) {
                    throwInvalidTransition(from, to);
                }
            }
            default -> throwInvalidTransition(from, to);
        }
    }

    private void throwInvalidTransition(RepairOrderStatus from, RepairOrderStatus to) {
        throw new BusinessRuleException(String.format("Không thể chuyển trạng thái phiếu từ %s sang %s", from, to));
    }

    private String generateOrderCode(String prefix) {
        String dateStr = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        for (int i = 0; i < 10; i++) {
            int randomNum = ThreadLocalRandom.current().nextInt(10000, 99999);
            String candidate = String.format("%s-%s-%d", prefix, dateStr, randomNum);
            if (!repairOrderRepository.existsByOrderCode(candidate)) {
                return candidate;
            }
        }
        return String.format("%s-%s-%d", prefix, dateStr, System.currentTimeMillis() % 100000);
    }
}
