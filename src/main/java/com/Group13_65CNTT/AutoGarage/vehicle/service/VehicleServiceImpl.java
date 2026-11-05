package com.Group13_65CNTT.AutoGarage.vehicle.service;

import com.Group13_65CNTT.AutoGarage.audit.annotation.Audited;
import com.Group13_65CNTT.AutoGarage.common.constant.AuditConstants;

import com.Group13_65CNTT.AutoGarage.common.exception.BusinessException;
import com.Group13_65CNTT.AutoGarage.common.enums.ResponseCode;
import com.Group13_65CNTT.AutoGarage.common.security.CustomerScope;
import com.Group13_65CNTT.AutoGarage.common.enums.AccessResource;
import com.Group13_65CNTT.AutoGarage.customer.entity.Customer;
import com.Group13_65CNTT.AutoGarage.customer.repository.CustomerRepository;
import com.Group13_65CNTT.AutoGarage.shared.exception.BusinessRuleException;
import com.Group13_65CNTT.AutoGarage.shared.exception.ConflictException;
import com.Group13_65CNTT.AutoGarage.shared.exception.ResourceNotFoundException;
import com.Group13_65CNTT.AutoGarage.vehicle.dto.CreateVehicleRequest;
import com.Group13_65CNTT.AutoGarage.vehicle.dto.TransferOwnershipRequest;
import com.Group13_65CNTT.AutoGarage.vehicle.dto.UpdateVehicleRequest;
import com.Group13_65CNTT.AutoGarage.vehicle.dto.VehicleResponse;
import com.Group13_65CNTT.AutoGarage.vehicle.entity.Vehicle;
import com.Group13_65CNTT.AutoGarage.vehicle.mapper.VehicleMapper;
import com.Group13_65CNTT.AutoGarage.vehicle.repository.VehicleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@lombok.RequiredArgsConstructor
@Transactional
public class VehicleServiceImpl implements VehicleService {
    private final CustomerScope customerScope;

    private final VehicleRepository vehicleRepository;
    private final CustomerRepository customerRepository;
    private final VehicleMapper vehicleMapper;

    private final com.Group13_65CNTT.AutoGarage.vehicle.repository.VehicleTimelineRepository timelineRepository;
    private final com.Group13_65CNTT.AutoGarage.vehicle.mapper.VehicleHistoryMapper historyMapper;
    private final com.Group13_65CNTT.AutoGarage.reception.mapper.VehicleCheckMapper checkMapper;
    private final com.Group13_65CNTT.AutoGarage.common.security.CurrentUserProvider currentUser;

    @Override
    @Audited(action = AuditConstants.ACTION_VEHICLE_CREATED, entity = AuditConstants.ENTITY_VEHICLE)
    public VehicleResponse createVehicle(CreateVehicleRequest request) {
        customerScope.requireFrontDesk();
        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khách hàng sở hữu với ID: " + request.getCustomerId()));

        String normalizedPlate = normalizePlate(request.getLicensePlate());
        if (vehicleRepository.existsByLicensePlate(normalizedPlate)) {
            throw new BusinessException(
                    ResponseCode.VEHICLE_PLATE_DUPLICATE,
                    "Biển số xe " + normalizedPlate + " đã tồn tại trong hệ thống");
        }
        request.setLicensePlate(normalizedPlate);

        if (request.getVin() != null && !request.getVin().isBlank()) {
            String cleanVin = request.getVin().trim().toUpperCase();
            if (vehicleRepository.existsByVin(cleanVin)) {
                throw new ConflictException("Số VIN " + cleanVin + " đã tồn tại trong hệ thống");
            }
            request.setVin(cleanVin);
        } else {
            request.setVin(null);
        }

        if (request.getOdometer() != null && request.getOdometer() < 0) {
            throw new BusinessRuleException("Số km đã đi (odometer) không được âm");
        }

        Vehicle vehicle = vehicleMapper.toEntity(request);
        Vehicle saved = vehicleRepository.save(vehicle);
        return vehicleMapper.toResponse(saved, customer.getFullName(), customer.getPhone());
    }

    @Override
    @Transactional(readOnly = true)
    public VehicleResponse getVehicleById(Long id) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phương tiện với ID: " + id));
        customerScope.requireResource(AccessResource.VEHICLE, vehicle.getId());
        Customer customer = customerRepository.findById(vehicle.getCustomerId()).orElse(null);
        return vehicleMapper.toResponse(vehicle,
                customer != null ? customer.getFullName() : null,
                customer != null ? customer.getPhone() : null);
    }

    @Override
    @Transactional(readOnly = true)
    public VehicleResponse getVehicleByPlate(String plate) {
        String normalizedPlate = normalizePlate(plate);
        Vehicle vehicle = vehicleRepository.findByLicensePlate(normalizedPlate)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phương tiện với biển số: " + plate));
        customerScope.requireResource(AccessResource.VEHICLE, vehicle.getId());
        Customer customer = customerRepository.findById(vehicle.getCustomerId()).orElse(null);
        return vehicleMapper.toResponse(vehicle,
                customer != null ? customer.getFullName() : null,
                customer != null ? customer.getPhone() : null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VehicleResponse> searchVehicles(Long customerId, String search, Boolean isActive) {
        String cleanSearch = (search != null && !search.isBlank()) ? search.trim() : null;
        Long scopedCustomerId = customerScope.customerIdForQuery(customerId);
        var vehicles = vehicleRepository.searchVehicles(scopedCustomerId, cleanSearch, isActive);
        var owners = customerRepository.findAllById(vehicles.stream().map(Vehicle::getCustomerId).distinct().toList())
                .stream().collect(java.util.stream.Collectors.toMap(Customer::getId, java.util.function.Function.identity()));
        return vehicles.stream().map(vehicle -> {
            Customer owner = owners.get(vehicle.getCustomerId());
            return vehicleMapper.toResponse(vehicle, owner == null ? null : owner.getFullName(),
                    owner == null ? null : owner.getPhone());
        }).toList();
    }

    @Override
    @Audited(action = AuditConstants.ACTION_VEHICLE_UPDATED, entity = AuditConstants.ENTITY_VEHICLE, idArgument = 0)
    public VehicleResponse updateVehicle(Long id, UpdateVehicleRequest request) {
        customerScope.requireFrontDesk();
        Vehicle vehicle = vehicleRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phương tiện với ID: " + id));

        if (request.getLicensePlate() != null && !request.getLicensePlate().isBlank()) {
            String normalizedPlate = normalizePlate(request.getLicensePlate());
            if (vehicleRepository.existsByLicensePlateAndIdNot(normalizedPlate, id)) {
                throw new BusinessException(
                        ResponseCode.VEHICLE_PLATE_DUPLICATE,
                        "Biển số xe " + normalizedPlate + " đã tồn tại ở phương tiện khác");
            }
            request.setLicensePlate(normalizedPlate);
        } else {
            request.setLicensePlate(vehicle.getLicensePlate());
        }

        if (request.getVin() != null && !request.getVin().isBlank()) {
            String cleanVin = request.getVin().trim().toUpperCase();
            if (vehicleRepository.existsByVinAndIdNot(cleanVin, id)) {
                throw new ConflictException("Số VIN " + cleanVin + " đã tồn tại ở phương tiện khác");
            }
            request.setVin(cleanVin);
        } else {
            request.setVin(null);
        }

        if (request.getOdometer() != null) {
            if (request.getOdometer() < 0) {
                throw new BusinessRuleException("Số km đã đi (odometer) không được âm");
            }
            if (request.getOdometer() < vehicle.getOdometer()) {
                throw new BusinessRuleException("Số km mới (" + request.getOdometer() +
                        ") không được nhỏ hơn số km hiện tại (" + vehicle.getOdometer() + ")");
            }
        }

        vehicleMapper.updateEntity(vehicle, request);
        Vehicle updated = vehicleRepository.save(vehicle);
        Customer customer = customerRepository.findById(updated.getCustomerId()).orElse(null);
        return vehicleMapper.toResponse(updated,
                customer != null ? customer.getFullName() : null,
                customer != null ? customer.getPhone() : null);
    }

    @Override
    @Audited(action = AuditConstants.ACTION_VEHICLE_TRANSFERRED, entity = AuditConstants.ENTITY_VEHICLE, idArgument = 0)
    public VehicleResponse transferOwnership(Long vehicleId, TransferOwnershipRequest request) {
        customerScope.requireFrontDesk();
        Vehicle vehicle = vehicleRepository.findByIdForUpdate(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phương tiện với ID: " + vehicleId));

        Customer newCustomer = customerRepository.findById(request.getNewCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khách hàng mới với ID: " + request.getNewCustomerId()));

        if (vehicle.getCustomerId().equals(request.getNewCustomerId())) {
            throw new BusinessRuleException("Phương tiện này hiện đã thuộc về khách hàng " + newCustomer.getFullName());
        }

        vehicle.setCustomerId(newCustomer.getId());
        Vehicle updated = vehicleRepository.save(vehicle);

        return vehicleMapper.toResponse(updated, newCustomer.getFullName(), newCustomer.getPhone());
    }

    @Override
    @Audited(action = AuditConstants.ACTION_VEHICLE_DEACTIVATED, entity = AuditConstants.ENTITY_VEHICLE, idArgument = 0)
    public void deactivateVehicle(Long id) {
        customerScope.requireFrontDesk();
        Vehicle vehicle = vehicleRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phương tiện với ID: " + id));
        vehicle.setIsActive(false);
        vehicleRepository.save(vehicle);
    }

    @Override
    @Audited(action = AuditConstants.ACTION_VEHICLE_ACTIVATED, entity = AuditConstants.ENTITY_VEHICLE, idArgument = 0)
    public VehicleResponse activateVehicle(Long id) {
        customerScope.requireFrontDesk();
        Vehicle vehicle = vehicleRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phương tiện với ID: " + id));
        Customer owner = customerRepository.findById(vehicle.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chủ xe của phương tiện này"));
        if (!Boolean.TRUE.equals(owner.getIsActive())) {
            throw new BusinessRuleException("Chủ xe đang ngừng theo dõi. Hãy mở lại khách hàng trước khi mở lại xe.");
        }
        vehicle.setIsActive(true);
        Vehicle saved = vehicleRepository.save(vehicle);
        return vehicleMapper.toResponse(saved, owner.getFullName(), owner.getPhone());
    }

    @Override
    @Transactional(readOnly = true)
    public com.Group13_65CNTT.AutoGarage.vehicle.dto.VehicleTimelineResponse getVehicleTimeline(Long vehicleId, Long requestingCustomerId, String userRole) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phương tiện với ID: " + vehicleId));
        return buildTimeline(vehicle, requestingCustomerId, userRole);
    }

    @Override
    @Transactional(readOnly = true)
    public com.Group13_65CNTT.AutoGarage.vehicle.dto.VehicleTimelineResponse getVehicleTimelineByPlate(String plate, Long requestingCustomerId, String userRole) {
        String normalizedPlate = normalizePlate(plate);
        Vehicle vehicle = vehicleRepository.findByLicensePlate(normalizedPlate)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phương tiện với biển số: " + plate));
        return buildTimeline(vehicle, requestingCustomerId, userRole);
    }

    private com.Group13_65CNTT.AutoGarage.vehicle.dto.VehicleTimelineResponse buildTimeline(Vehicle vehicle, Long requestingCustomerId, String userRole) {
        customerScope.requireResource(AccessResource.VEHICLE, vehicle.getId());
        var actorRole = currentUser.role();
        boolean frontDesk = actorRole == com.Group13_65CNTT.AutoGarage.common.enums.Role.ADMIN
                || actorRole == com.Group13_65CNTT.AutoGarage.common.enums.Role.RECEPTIONIST;
        Long ownCustomerId = actorRole == com.Group13_65CNTT.AutoGarage.common.enums.Role.CUSTOMER
                ? customerScope.currentCustomerId() : null;
        Long technicianId = actorRole == com.Group13_65CNTT.AutoGarage.common.enums.Role.TECHNICIAN
                ? currentUser.userId() : null;

        Customer currentOwner = customerRepository.findById(vehicle.getCustomerId()).orElse(null);

        com.Group13_65CNTT.AutoGarage.vehicle.dto.VehicleTimelineResponse timeline = new com.Group13_65CNTT.AutoGarage.vehicle.dto.VehicleTimelineResponse();
        timeline.setVehicleId(vehicle.getId());
        timeline.setLicensePlate(vehicle.getLicensePlate());
        timeline.setBrand(vehicle.getBrand());
        timeline.setModel(vehicle.getModel());
        timeline.setCurrentOdometer(vehicle.getOdometer());
        timeline.setCurrentCustomerId(vehicle.getCustomerId());
        timeline.setCurrentCustomerName(currentOwner != null ? currentOwner.getFullName() : null);

        var orders = timelineRepository.orders(vehicle.getId(), technicianId);
        if (orders.isEmpty()) return timeline;
        var ids = orders.stream().map(com.Group13_65CNTT.AutoGarage.repair.entity.RepairOrder::getId).toList();
        var tasks = timelineRepository.completedTasks(ids).stream().collect(java.util.stream.Collectors.groupingBy(
                com.Group13_65CNTT.AutoGarage.repair.entity.RepairTask::getRepairOrderId));
        var parts = timelineRepository.parts(ids).stream().collect(java.util.stream.Collectors.groupingBy(
                com.Group13_65CNTT.AutoGarage.vehicle.repository.VehicleTimelineRepository.ReplacedPart::getOrderId));
        // Files and warranty details retain historical order ownership, even after vehicle transfer.
        var privateIds = orders.stream().filter(order -> frontDesk || technicianId != null
                || java.util.Objects.equals(ownCustomerId, order.getCustomerId())).map(
                com.Group13_65CNTT.AutoGarage.repair.entity.RepairOrder::getId).toList();
        var attachments = privateIds.isEmpty() ? java.util.List.<com.Group13_65CNTT.AutoGarage.reception.entity.Attachment>of()
                : timelineRepository.attachments(privateIds);
        var warranties = privateIds.isEmpty() ? java.util.List.<com.Group13_65CNTT.AutoGarage.vehicle.dto.response.VehicleWarrantyResponse>of()
                : timelineRepository.warranties(privateIds);
        var invoices = frontDesk || ownCustomerId != null ? timelineRepository.invoices(ids)
                : java.util.List.<com.Group13_65CNTT.AutoGarage.billing.entity.Invoice>of();
        timeline.setHistoryEntries(orders.stream().map(order -> {
            var entry = historyMapper.toResponse(order);
            boolean ownRecord = java.util.Objects.equals(ownCustomerId, order.getCustomerId());
            entry.setCurrentOwnerRecord(java.util.Objects.equals(vehicle.getCustomerId(), order.getCustomerId()));
            if (frontDesk || ownRecord) {
                entry.setOrderCustomerId(order.getCustomerId());
                entry.setCustomerComplaint(order.getCustomerComplaint());
            }
            entry.setTasksDone(tasks.getOrDefault(order.getId(), java.util.List.of()).stream()
                    .map(com.Group13_65CNTT.AutoGarage.repair.entity.RepairTask::getTaskName).toList());
            entry.setPartsReplaced(parts.getOrDefault(order.getId(), java.util.List.of()).stream()
                    .map(com.Group13_65CNTT.AutoGarage.vehicle.repository.VehicleTimelineRepository.ReplacedPart::getName).toList());
            entry.setTechnicalAttachments(attachments.stream().filter(a -> order.getId().equals(a.getRepairOrderId()))
                    .map(checkMapper::toAttachmentResponse).toList());
            entry.setWarranties(warranties.stream().filter(w -> order.getId().equals(w.repairOrderId())).toList());
            invoices.stream().filter(invoice -> order.getId().equals(invoice.getRepairOrderId())
                    && (frontDesk || java.util.Objects.equals(ownCustomerId, invoice.getCustomerId())))
                    .findFirst().ifPresent(invoice -> entry.setTotalAmount(invoice.getTotalAmount()));
            return entry;
        }).toList());
        return timeline;
    }

    private String normalizePlate(String plate) {
        if (plate == null) {
            return "";
        }
        return plate.trim().replaceAll("\\s+", "").toUpperCase();
    }
}
