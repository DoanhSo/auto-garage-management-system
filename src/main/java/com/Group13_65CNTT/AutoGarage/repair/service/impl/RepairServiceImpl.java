package com.Group13_65CNTT.AutoGarage.repair.service.impl;

import com.Group13_65CNTT.AutoGarage.common.util.RequestFingerprint;

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
import com.Group13_65CNTT.AutoGarage.repair.entity.AssignmentStage;
import com.Group13_65CNTT.AutoGarage.repair.entity.OrderType;
import com.Group13_65CNTT.AutoGarage.repair.entity.WorkResultStatus;
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
import com.Group13_65CNTT.AutoGarage.repair.service.RepairStageRules;
import com.Group13_65CNTT.AutoGarage.estimate.entity.ApprovalStatus;
import com.Group13_65CNTT.AutoGarage.estimate.entity.Estimate;
import com.Group13_65CNTT.AutoGarage.estimate.entity.EstimateStatus;
import com.Group13_65CNTT.AutoGarage.estimate.repository.EstimateItemRepository;
import com.Group13_65CNTT.AutoGarage.estimate.repository.EstimateRepository;
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

    /** How long the reception bay stays reserved for a freshly checked-in vehicle. */
    private static final java.time.Duration RECEPTION_BAY_HOLD = java.time.Duration.ofHours(12);
    private final com.Group13_65CNTT.AutoGarage.common.security.CustomerScope customerScope;
    private final com.Group13_65CNTT.AutoGarage.common.security.CurrentUserProvider currentUser;
    private final com.Group13_65CNTT.AutoGarage.identity.service.ObjectAccessService objectAccess;

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
    private final EstimateRepository estimateRepository;
    private final EstimateItemRepository estimateItemRepository;

    private final BayScheduling bayScheduling;
    private final com.Group13_65CNTT.AutoGarage.repair.service.WorkLogRecorder workLogRecorder;
    private final com.Group13_65CNTT.AutoGarage.scheduling.repository.EmployeeRepository employeeRepository;

    @Autowired(required = false)
    @org.springframework.context.annotation.Lazy
    private BillingOperations billingOperations;

    @Autowired(required = false)
    @org.springframework.context.annotation.Lazy
    private com.Group13_65CNTT.AutoGarage.shared.contracts.PayrollOperations payrollOperations;

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
        if (!java.util.Objects.equals(vehicle.getCustomerId(), customerId)) {
            throw new BusinessRuleException("Phương tiện không thuộc khách hàng tiếp nhận");
        }
        if (vehicle.getOdometer() != null && odometer < vehicle.getOdometer()) {
            throw new BusinessRuleException("Số km tiếp nhận không được nhỏ hơn số km đã ghi nhận");
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
            if (booking.getStatus() != BookingStatus.PENDING && booking.getStatus() != BookingStatus.CONFIRMED) {
                throw new BusinessRuleException("Lịch hẹn không còn ở trạng thái có thể tiếp nhận");
            }

            booking.setStatus(BookingStatus.RECEIVED);
            bookingRepository.save(booking);
            bayScheduling.releaseForBooking(actorUserId, bookingId);
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
    public void recordReceptionDetails(long repairOrderId, BigDecimal fuelLevelPercent, String customerComplaint) {
        RepairOrder order = repairOrderRepository.findByIdForUpdate(repairOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiếu tiếp nhận"));
        if (order.getStatus() != RepairOrderStatus.NEW) {
            throw new BusinessRuleException("Chỉ ghi thông tin tiếp nhận cho phiếu mới");
        }
        order.setFuelLevelPercent(fuelLevelPercent);
        order.setCustomerComplaint(customerComplaint);
        repairOrderRepository.save(order);
    }

    @Override
    @Transactional
    public void recordReceptionDispatch(
            long actorUserId, long repairOrderId, Long diagnosticTechnicianId, Long serviceBayId) {
        if (diagnosticTechnicianId == null && serviceBayId == null) {
            return;
        }
        RepairOrder order = repairOrderRepository.findByIdForUpdate(repairOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiếu tiếp nhận"));
        Instant now = Instant.now();
        if (diagnosticTechnicianId != null) {
            boolean activeEmployee = employeeRepository.findById(diagnosticTechnicianId)
                    .map(employee -> Boolean.TRUE.equals(employee.getIsActive()))
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy kỹ thuật viên chẩn đoán"));
            if (!activeEmployee) {
                throw new BusinessRuleException("Kỹ thuật viên chẩn đoán đã ngưng hoạt động");
            }
            if (workLogRecorder.activeShift(diagnosticTechnicianId, now).isEmpty()) {
                log.info("Kỹ thuật viên chẩn đoán #{} chưa có ca trực đang hoạt động tại thời điểm tiếp nhận", diagnosticTechnicianId);
            }
            order.setDiagnosticTechnicianId(diagnosticTechnicianId);
            workLogRecorder.start(order, diagnosticTechnicianId, AssignmentStage.DIAGNOSIS, null, now, null);
        }
        if (serviceBayId != null) {
            bayScheduling.reserveRepair(actorUserId, repairOrderId, serviceBayId, now, now.plus(RECEPTION_BAY_HOLD));
            order.setServiceBayId(serviceBayId);
        }
        repairOrderRepository.save(order);
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
        if (claim.getStatus() != ClaimStatus.APPROVED) {
            throw new BusinessRuleException("Yêu cầu bảo hành phải được duyệt trước khi tạo phiếu xử lý");
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
        if (inventoryOperations != null) {
            inventoryOperations.requireIssueComplete(repairOrderId);
        }
    }

    @Override
    @Transactional
    public void handOver(long actorUserId, long repairOrderId, int odometerOut, String requestKey) {
        log.info("RepairWorkflow.handOver: actor={}, repairOrderId={}, odoOut={}, requestKey={}",
                actorUserId, repairOrderId, odometerOut, requestKey);

        String requestHash = RequestFingerprint.of("HANDOVER", actorUserId, repairOrderId, odometerOut);
        if (notificationAudit != null && requestKey != null && !requestKey.isBlank()
                && notificationAudit.findCompletedRequest(requestKey, requestHash) != null) {
            return;
        }

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

        requireInvoiceReady(actorUserId, repairOrderId);

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

        // Ghi nhận thưởng/hoa hồng cho các thợ tham gia khi phiếu đã bàn giao
        if (payrollOperations != null) {
            payrollOperations.recordOrderCommissions(actorUserId, repairOrderId);
        }

        // Cập nhật Odometer của xe
        vehicleRepository.findById(order.getVehicleId()).ifPresent(v -> {
            v.setOdometer(v.getOdometer() == null ? odometerOut : Math.max(v.getOdometer(), odometerOut));
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
            invoiceRepository.findActiveByRepairOrderId(repairOrderId).ifPresent(inv -> {
                warrantyOperations.createFromInvoice(actorUserId, inv.getId(), odometerOut);
            });
        }

        // Ghi completed request nếu có notificationAudit
        if (notificationAudit != null && requestKey != null && !requestKey.isBlank()) {
            notificationAudit.recordCompletedRequest(actorUserId, "HANDOVER", "REPAIR_ORDER",
                    repairOrderId, requestKey, requestHash, "{\"status\":\"CLOSED\"}");
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
        customerScope.requireResource(com.Group13_65CNTT.AutoGarage.common.enums.AccessResource.REPAIR_ORDER, id);
        RepairOrder order = repairOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiếu sửa chữa với ID: " + id));
        return repairOrderMapper.toResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public RepairOrderResponse getRepairOrderByCode(String orderCode) {
        RepairOrder order = repairOrderRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiếu sửa chữa với mã: " + orderCode));
        customerScope.requireResource(com.Group13_65CNTT.AutoGarage.common.enums.AccessResource.REPAIR_ORDER, order.getId());
        return repairOrderMapper.toResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RepairOrderResponse> searchRepairOrders(String keyword, RepairOrderStatus status,
                                                        int page, int size, String sortBy, String sortDir) {
        Pageable pageable = pagination.page(page, size, sortBy, sortDir,
                Set.of("id", "orderCode", "receivedAt", "status", "createdAt"));
        com.Group13_65CNTT.AutoGarage.common.enums.Role role = currentUser.role();
        Page<RepairOrder> resultPage;
        if (role == com.Group13_65CNTT.AutoGarage.common.enums.Role.TECHNICIAN) {
            resultPage = repairOrderRepository.searchAssignedOrders(currentUser.userId(), keyword, status, pageable);
        } else if (role == com.Group13_65CNTT.AutoGarage.common.enums.Role.CUSTOMER) {
            resultPage = repairOrderRepository.searchOrders(customerScope.currentCustomerId(), keyword, status, pageable);
        } else {
            resultPage = repairOrderRepository.searchOrders(null, keyword, status, pageable);
        }
        return resultPage.map(repairOrderMapper::toResponse);
    }

    @Override
    @Transactional
    public RepairOrderResponse updateRepairOrder(Long actorUserId, Long id, RepairOrderUpdateRequest request) {
        RepairOrder order = repairOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiếu sửa chữa với ID: " + id));

        if (RepairStageRules.TERMINAL.contains(order.getStatus())) {
            throw new BusinessRuleException("Phiếu sửa chữa đã đóng hoặc bị hủy, không thể chỉnh sửa");
        }
        boolean changesReception = request.getFuelLevelPercent() != null || request.getCustomerComplaint() != null;
        if (changesReception && !RepairStageRules.RECEPTION_EDITABLE.contains(order.getStatus())) {
            throw new BusinessRuleException("Thông tin tiếp nhận chỉ sửa được khi phiếu vừa tiếp nhận");
        }
        if (request.getLeadTechnicianId() != null && !RepairStageRules.ASSIGNMENT_EDITABLE.contains(order.getStatus())) {
            throw new BusinessRuleException("Phiếu đã qua kiểm tra chất lượng, không thể đổi kỹ thuật viên trưởng");
        }

        if (request.getLeadTechnicianId() != null) {
            boolean activeEmployee = employeeRepository.findById(request.getLeadTechnicianId())
                    .map(employee -> Boolean.TRUE.equals(employee.getIsActive()))
                    .orElse(false);
            if (!activeEmployee) {
                throw new BusinessRuleException("Kỹ thuật viên trưởng không tồn tại hoặc đã ngưng hoạt động");
            }
        }

        repairOrderMapper.updateEntityFromRequest(request, order);
        return repairOrderMapper.toResponse(repairOrderRepository.save(order));
    }

    @Override
    @Transactional
    public RepairOrderResponse updateDiagnosis(Long actorUserId, Long id, DiagnosisUpdateRequest request) {
        objectAccess.requireRepairOrderAccess(actorUserId, id);
        RepairOrder order = repairOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiếu sửa chữa với ID: " + id));

        if (!RepairStageRules.DIAGNOSIS_EDITABLE.contains(order.getStatus())) {
            throw new BusinessRuleException("Chẩn đoán đã chốt khi lập báo giá. Hãy dùng báo giá phát sinh hoặc mở lại chẩn đoán khi khách từ chối toàn bộ");
        }

        boolean firstDiagnosis = order.getDiagnosis() == null || order.getDiagnosis().isBlank();
        order.setDiagnosis(request.getDiagnosis());
        if (firstDiagnosis && order.getDiagnosticTechnicianId() != null) {
            workLogRecorder.finish(order.getId(), order.getDiagnosticTechnicianId(), AssignmentStage.DIAGNOSIS,
                    Instant.now(), WorkResultStatus.COMPLETED, request.getDiagnosis());
        }
        if (order.getStatus() == RepairOrderStatus.NEW) {
            order.setStatus(RepairOrderStatus.DIAGNOSING);
        }
        return repairOrderMapper.toResponse(repairOrderRepository.save(order));
    }

    @Override
    @Transactional
    public RepairOrderResponse transitionStatus(Long actorUserId, Long id, RepairOrderStatus newStatus, String note) {
        objectAccess.requireRepairOrderAccess(actorUserId, id);
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

        if (Set.of(RepairOrderStatus.APPROVED, RepairOrderStatus.QC_FAILED,
                RepairOrderStatus.COMPLETED, RepairOrderStatus.INVOICED, RepairOrderStatus.CLOSED).contains(newStatus)) {
            throw new BusinessRuleException("Trạng thái này phải được cập nhật qua nghiệp vụ duyệt báo giá, QC, hóa đơn hoặc bàn giao tương ứng");
        }

        if (newStatus == RepairOrderStatus.WAITING_QC) {
            if (repairTaskRepository.existsByRepairOrderIdAndStatusNot(id, TaskStatus.COMPLETED)
                    || laborSessionRepository.countOpenSessionsByRepairOrderId(id) > 0) {
                throw new BusinessRuleException("Còn công việc chưa hoàn thành hoặc đồng hồ đang chạy, chưa thể chuyển sang chờ QC");
            }
        }

        if (current == RepairOrderStatus.WAITING_APPROVAL && newStatus == RepairOrderStatus.DIAGNOSING) {
            reopenDiagnosis(id);
        }

        if (newStatus == RepairOrderStatus.CANCELLED) {
            cancelOpenEstimates(id);
            List<RepairTask> tasks = repairTaskRepository.findByRepairOrderId(id);
            if (laborSessionRepository.countOpenSessionsByRepairOrderId(id) > 0
                    || tasks.stream().anyMatch(task -> task.getStartedAt() != null || task.getStatus() == TaskStatus.COMPLETED)) {
                throw new BusinessRuleException("Phiếu đã thi công phải xử lý công việc, vật tư và chi phí trước khi hủy");
            }
            bayScheduling.releaseForRepair(actorUserId, id);
            if (inventoryOperations != null) inventoryOperations.releaseForOrder(actorUserId, id);
            tasks.forEach(task -> task.setStatus(TaskStatus.CANCELLED));
            repairTaskRepository.saveAll(tasks);
        }

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
        customerScope.requireResource(com.Group13_65CNTT.AutoGarage.common.enums.AccessResource.REPAIR_ORDER, repairOrderId);
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

        if (order.getStatus() == RepairOrderStatus.CLOSED || order.getStatus() == RepairOrderStatus.CANCELLED || order.getStatus() == RepairOrderStatus.INVOICED) {
            throw new BusinessRuleException("Không thể thêm công việc vào phiếu sửa chữa đã đóng hoặc bị hủy");
        }

        throw new BusinessRuleException("Công việc sửa chữa phải được tạo từ hạng mục dịch vụ đã duyệt trong báo giá");
    }

    @Override
    @Transactional
    public RepairTaskResponse updateTask(Long actorUserId, Long taskId, RepairTaskUpdateRequest request) {
        RepairTask task = repairTaskRepository.findByIdForUpdate(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy công việc với ID: " + taskId));

        RepairOrder order = repairOrderRepository.findById(task.getRepairOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiếu sửa chữa của công việc này"));

        if (order.getStatus() == RepairOrderStatus.CLOSED || order.getStatus() == RepairOrderStatus.CANCELLED || order.getStatus() == RepairOrderStatus.INVOICED) {
            throw new BusinessRuleException("Không thể chỉnh sửa công việc trong phiếu sửa chữa đã kết thúc");
        }

        if (request.getStatus() != null && request.getStatus() != task.getStatus()) {
            throw new BusinessRuleException("Thay đổi trạng thái công việc phải qua luồng thực hiện hoặc hoàn thành công việc");
        }
        if (task.getStatus() == TaskStatus.COMPLETED) {
            throw new BusinessRuleException("Công việc đã hoàn thành, không thể chỉnh sửa");
        }
        if (task.getEstimateItemId() != null && (request.getQuantity() != null || request.getUnitPrice() != null)) {
            throw new BusinessRuleException("Số lượng và đơn giá đã duyệt chỉ được điều chỉnh bằng báo giá phát sinh");
        }
        repairTaskMapper.updateEntityFromRequest(request, task);
        return repairTaskMapper.toResponse(repairTaskRepository.save(task));
    }

    @Override
    @Transactional
    public void deleteTask(Long actorUserId, Long taskId) {
        RepairTask task = repairTaskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy công việc với ID: " + taskId));

        RepairOrder order = repairOrderRepository.findById(task.getRepairOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiếu sửa chữa"));

        if (order.getStatus() == RepairOrderStatus.CLOSED || order.getStatus() == RepairOrderStatus.CANCELLED || order.getStatus() == RepairOrderStatus.INVOICED) {
            throw new BusinessRuleException("Không thể xóa công việc trong phiếu sửa chữa đã kết thúc");
        }

        if (task.getStatus() == TaskStatus.IN_PROGRESS || task.getStatus() == TaskStatus.COMPLETED) {
            throw new BusinessRuleException("Không thể xóa công việc đang thực hiện hoặc đã hoàn thành");
        }

        if (task.getEstimateItemId() != null) {
            throw new BusinessRuleException("Không được xóa công việc đã sinh từ báo giá được duyệt");
        }

        repairTaskRepository.delete(task);
    }

    // =========================================================================
    // Helper Methods
    // =========================================================================

    private void validateTransition(RepairOrderStatus from, RepairOrderStatus to, RepairOrder order) {
        if (to == RepairOrderStatus.CANCELLED) {
            if (from != RepairOrderStatus.NEW && from != RepairOrderStatus.DIAGNOSING && from != RepairOrderStatus.WAITING_APPROVAL) {
                throw new BusinessRuleException("Chỉ hủy trực tiếp phiếu chưa được duyệt thi công");
            }
            return;
        }

        switch (from) {
            case NEW, DIAGNOSING -> {
                if (to == RepairOrderStatus.WAITING_APPROVAL) {
                    throw new BusinessRuleException("Chỉ chuyển sang chờ duyệt bằng cách lập báo giá");
                }
                if (from == RepairOrderStatus.DIAGNOSING || to != RepairOrderStatus.DIAGNOSING) {
                    throwInvalidTransition(from, to);
                }
            }
            case WAITING_APPROVAL -> {
                // APPROVED is reached only through estimate approval; DIAGNOSING reopens the diagnosis.
                if (to != RepairOrderStatus.APPROVED && to != RepairOrderStatus.DIAGNOSING) {
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

    /**
     * The only way back to an earlier stage: allowed when the customer has turned down every line,
     * so no estimate is approved or still awaiting a decision. Leftover drafts are cancelled.
     */
    private void reopenDiagnosis(Long repairOrderId) {
        if (estimateItemRepository.existsLineByRepairOrderIdAndStatus(repairOrderId, ApprovalStatus.APPROVED)) {
            throw new BusinessRuleException("Phiếu đã có hạng mục được duyệt, không thể mở lại chẩn đoán");
        }
        Instant now = Instant.now();
        boolean awaitingCustomer = estimateRepository.findByRepairOrderIdAndStatusIn(repairOrderId,
                        EnumSet.of(EstimateStatus.SENT, EstimateStatus.PARTIALLY_APPROVED)).stream()
                .anyMatch(estimate -> estimate.getValidUntil() == null || now.isBefore(estimate.getValidUntil()));
        if (awaitingCustomer) {
            throw new BusinessRuleException("Chỉ mở lại chẩn đoán khi khách đã từ chối toàn bộ báo giá");
        }
        cancelOpenEstimates(repairOrderId);
    }

    /** Estimates that can no longer be decided once the order goes back or is cancelled. */
    private void cancelOpenEstimates(Long repairOrderId) {
        List<Estimate> open = estimateRepository.findByRepairOrderIdAndStatusIn(repairOrderId,
                EnumSet.of(EstimateStatus.DRAFT, EstimateStatus.SENT, EstimateStatus.PARTIALLY_APPROVED));
        if (open.isEmpty()) {
            return;
        }
        open.forEach(estimate -> estimate.setStatus(EstimateStatus.CANCELLED));
        estimateRepository.saveAll(open);
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
