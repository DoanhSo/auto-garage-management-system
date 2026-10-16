package com.Group13_65CNTT.AutoGarage.vehicle.service;

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
@Transactional
public class VehicleServiceImpl implements VehicleService {

    private final VehicleRepository vehicleRepository;
    private final CustomerRepository customerRepository;
    private final VehicleMapper vehicleMapper;

    public VehicleServiceImpl(VehicleRepository vehicleRepository,
                              CustomerRepository customerRepository,
                              VehicleMapper vehicleMapper) {
        this.vehicleRepository = vehicleRepository;
        this.customerRepository = customerRepository;
        this.vehicleMapper = vehicleMapper;
    }

    @Override
    public VehicleResponse createVehicle(CreateVehicleRequest request) {
        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khách hàng sở hữu với ID: " + request.getCustomerId()));

        String normalizedPlate = normalizePlate(request.getLicensePlate());
        if (vehicleRepository.existsByLicensePlate(normalizedPlate)) {
            throw new ConflictException("Biển số xe " + normalizedPlate + " đã tồn tại trong hệ thống");
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
        Customer customer = customerRepository.findById(vehicle.getCustomerId()).orElse(null);
        return vehicleMapper.toResponse(vehicle,
                customer != null ? customer.getFullName() : null,
                customer != null ? customer.getPhone() : null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VehicleResponse> searchVehicles(Long customerId, String search, Boolean isActive) {
        String cleanSearch = (search != null && !search.isBlank()) ? search.trim() : null;
        return vehicleRepository.searchVehicles(customerId, cleanSearch, isActive)
                .stream()
                .map(v -> {
                    Customer c = customerRepository.findById(v.getCustomerId()).orElse(null);
                    return vehicleMapper.toResponse(v,
                            c != null ? c.getFullName() : null,
                            c != null ? c.getPhone() : null);
                })
                .toList();
    }

    @Override
    public VehicleResponse updateVehicle(Long id, UpdateVehicleRequest request) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phương tiện với ID: " + id));

        String normalizedPlate = normalizePlate(request.getLicensePlate());
        if (vehicleRepository.existsByLicensePlateAndIdNot(normalizedPlate, id)) {
            throw new ConflictException("Biển số xe " + normalizedPlate + " đã tồn tại ở phương tiện khác");
        }
        request.setLicensePlate(normalizedPlate);

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
    public VehicleResponse transferOwnership(Long vehicleId, TransferOwnershipRequest request) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
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
    public void deactivateVehicle(Long id) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phương tiện với ID: " + id));
        vehicle.setIsActive(false);
        vehicleRepository.save(vehicle);
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
        if ("CUSTOMER".equalsIgnoreCase(userRole)) {
            if (requestingCustomerId == null || !requestingCustomerId.equals(vehicle.getCustomerId())) {
                throw new BusinessRuleException("Bạn không có quyền truy cập lịch sử của phương tiện này (BR02)");
            }
        }

        Customer currentOwner = customerRepository.findById(vehicle.getCustomerId()).orElse(null);

        com.Group13_65CNTT.AutoGarage.vehicle.dto.VehicleTimelineResponse timeline = new com.Group13_65CNTT.AutoGarage.vehicle.dto.VehicleTimelineResponse();
        timeline.setVehicleId(vehicle.getId());
        timeline.setLicensePlate(vehicle.getLicensePlate());
        timeline.setBrand(vehicle.getBrand());
        timeline.setModel(vehicle.getModel());
        timeline.setCurrentOdometer(vehicle.getOdometer());
        timeline.setCurrentCustomerId(vehicle.getCustomerId());
        timeline.setCurrentCustomerName(currentOwner != null ? currentOwner.getFullName() : null);

        return timeline;
    }

    private String normalizePlate(String plate) {
        if (plate == null) {
            return "";
        }
        return plate.replaceAll("[\\s.-]", "").toUpperCase();
    }
}
