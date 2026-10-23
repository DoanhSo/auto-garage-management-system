package com.Group13_65CNTT.AutoGarage.scheduling;

import com.Group13_65CNTT.AutoGarage.common.enums.ResponseCode;
import com.Group13_65CNTT.AutoGarage.common.exception.BusinessException;
import com.Group13_65CNTT.AutoGarage.scheduling.dto.request.MaintenanceReservationRequest;
import com.Group13_65CNTT.AutoGarage.scheduling.dto.request.ServiceBayCreateRequest;
import com.Group13_65CNTT.AutoGarage.scheduling.dto.request.ServiceBayUpdateRequest;
import com.Group13_65CNTT.AutoGarage.scheduling.dto.response.BayReservationResponse;
import com.Group13_65CNTT.AutoGarage.scheduling.dto.response.ServiceBayResponse;
import com.Group13_65CNTT.AutoGarage.scheduling.entity.BayReservation;
import com.Group13_65CNTT.AutoGarage.scheduling.entity.ReservationStatus;
import com.Group13_65CNTT.AutoGarage.scheduling.entity.ReservationType;
import com.Group13_65CNTT.AutoGarage.scheduling.entity.ServiceBay;
import com.Group13_65CNTT.AutoGarage.scheduling.mapper.BayReservationMapper;
import com.Group13_65CNTT.AutoGarage.scheduling.mapper.ServiceBayMapper;
import com.Group13_65CNTT.AutoGarage.scheduling.repository.BayReservationRepository;
import com.Group13_65CNTT.AutoGarage.scheduling.repository.ServiceBayRepository;
import com.Group13_65CNTT.AutoGarage.scheduling.service.impl.SchedulingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServiceBayServiceTest {

    @Mock
    private ServiceBayRepository serviceBayRepository;

    @Mock
    private BayReservationRepository bayReservationRepository;

    @Mock
    private ServiceBayMapper serviceBayMapper;

    @Mock
    private BayReservationMapper bayReservationMapper;

    @InjectMocks
    private SchedulingServiceImpl schedulingService;

    private ServiceBay bay;
    private ServiceBayCreateRequest createRequest;
    private ServiceBayResponse response;
    private Instant now;

    @BeforeEach
    void setUp() {
        now = Instant.now();
        createRequest = ServiceBayCreateRequest.builder()
                .bayCode("BAY-01")
                .bayName("Khoang nhanh 01")
                .bayType("QUICK_SERVICE")
                .description("Khoang sua chua bao duong nhanh")
                .build();

        bay = ServiceBay.builder()
                .id(1L)
                .bayCode("BAY-01")
                .bayName("Khoang nhanh 01")
                .bayType("QUICK_SERVICE")
                .description("Khoang sua chua bao duong nhanh")
                .isActive(true)
                .createdAt(now)
                .updatedAt(now)
                .version(0L)
                .build();

        response = ServiceBayResponse.builder()
                .id(1L)
                .bayCode("BAY-01")
                .bayName("Khoang nhanh 01")
                .bayType("QUICK_SERVICE")
                .description("Khoang sua chua bao duong nhanh")
                .isActive(true)
                .createdAt(now)
                .updatedAt(now)
                .version(0L)
                .build();
    }

    @Test
    @DisplayName("Tạo khoang sửa chữa thành công")
    void createServiceBay_success() {
        when(serviceBayRepository.existsByBayCode("BAY-01")).thenReturn(false);
        when(serviceBayMapper.toEntity(createRequest)).thenReturn(bay);
        when(serviceBayRepository.save(any(ServiceBay.class))).thenReturn(bay);
        when(serviceBayMapper.toResponse(bay)).thenReturn(response);

        ServiceBayResponse actual = schedulingService.createServiceBay(createRequest);

        assertThat(actual).isNotNull();
        assertThat(actual.getBayCode()).isEqualTo("BAY-01");
        verify(serviceBayRepository).save(any(ServiceBay.class));
    }

    @Test
    @DisplayName("Tạo khoang thất bại khi trùng mã khoang")
    void createServiceBay_duplicateCode_throwsDuplicate() {
        when(serviceBayRepository.existsByBayCode("BAY-01")).thenReturn(true);

        assertThatThrownBy(() -> schedulingService.createServiceBay(createRequest))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("responseCode", ResponseCode.DUPLICATE_RESOURCE);

        verify(serviceBayRepository, never()).save(any());
    }

    @Test
    @DisplayName("Cập nhật thông tin khoang thành công")
    void updateServiceBay_success() {
        ServiceBayUpdateRequest updateRequest = ServiceBayUpdateRequest.builder()
                .bayName("Khoang nhanh VIP 01")
                .bayType("QUICK_SERVICE")
                .description("Mo rong")
                .isActive(true)
                .build();

        when(serviceBayRepository.findById(1L)).thenReturn(Optional.of(bay));
        when(serviceBayRepository.save(bay)).thenReturn(bay);
        when(serviceBayMapper.toResponse(bay)).thenReturn(response);

        ServiceBayResponse actual = schedulingService.updateServiceBay(1L, updateRequest);

        assertThat(actual).isNotNull();
        verify(serviceBayMapper).updateEntityFromRequest(updateRequest, bay);
        verify(serviceBayRepository).save(bay);
    }

    @Test
    @DisplayName("Vô hiệu hóa khoang thành công (soft deactivate)")
    void deactivateServiceBay_success() {
        when(serviceBayRepository.findById(1L)).thenReturn(Optional.of(bay));

        schedulingService.deactivateServiceBay(1L);

        assertThat(bay.getIsActive()).isFalse();
        verify(serviceBayRepository).save(bay);
    }

    @Test
    @DisplayName("Tạo lịch bảo trì khoang thành công (UC37)")
    void scheduleMaintenance_success() {
        MaintenanceReservationRequest maintenanceRequest = MaintenanceReservationRequest.builder()
                .bayId(1L)
                .startsAt(now.plus(Duration.ofDays(1)))
                .endsAt(now.plus(Duration.ofDays(1)).plus(Duration.ofHours(4)))
                .notes("Bao tri dinh ky cau nang")
                .build();

        BayReservation reservation = BayReservation.builder()
                .id(10L)
                .bayId(1L)
                .reservationType(ReservationType.MAINTENANCE)
                .status(ReservationStatus.IN_USE)
                .startsAt(maintenanceRequest.getStartsAt())
                .endsAt(maintenanceRequest.getEndsAt())
                .notes("Bao tri dinh ky cau nang")
                .build();

        BayReservationResponse reservationResponse = BayReservationResponse.builder()
                .id(10L)
                .bayId(1L)
                .reservationType(ReservationType.MAINTENANCE)
                .status(ReservationStatus.IN_USE)
                .startsAt(maintenanceRequest.getStartsAt())
                .endsAt(maintenanceRequest.getEndsAt())
                .build();

        when(serviceBayRepository.findByIdWithLock(1L)).thenReturn(Optional.of(bay));
        when(bayReservationRepository.findOverlappingReservations(eq(1L), anyCollection(), eq(maintenanceRequest.getStartsAt()), eq(maintenanceRequest.getEndsAt())))
                .thenReturn(Collections.emptyList());
        when(bayReservationRepository.save(any(BayReservation.class))).thenReturn(reservation);
        when(bayReservationMapper.toResponse(reservation)).thenReturn(reservationResponse);

        BayReservationResponse actual = schedulingService.scheduleMaintenance(100L, maintenanceRequest);

        assertThat(actual).isNotNull();
        assertThat(actual.getReservationType()).isEqualTo(ReservationType.MAINTENANCE);
        verify(bayReservationRepository).save(any(BayReservation.class));
    }

    @Test
    @DisplayName("Tạo lịch bảo trì thất bại khi khoang đang bận / trùng lịch (BR06)")
    void scheduleMaintenance_overlappingReservation_throwsDuplicate() {
        MaintenanceReservationRequest maintenanceRequest = MaintenanceReservationRequest.builder()
                .bayId(1L)
                .startsAt(now.plus(Duration.ofDays(1)))
                .endsAt(now.plus(Duration.ofDays(1)).plus(Duration.ofHours(4)))
                .notes("Bao tri dinh ky cau nang")
                .build();

        BayReservation existing = BayReservation.builder()
                .id(5L)
                .bayId(1L)
                .status(ReservationStatus.IN_USE)
                .build();

        when(serviceBayRepository.findByIdWithLock(1L)).thenReturn(Optional.of(bay));
        when(bayReservationRepository.findOverlappingReservations(eq(1L), anyCollection(), eq(maintenanceRequest.getStartsAt()), eq(maintenanceRequest.getEndsAt())))
                .thenReturn(List.of(existing));

        assertThatThrownBy(() -> schedulingService.scheduleMaintenance(100L, maintenanceRequest))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("responseCode", ResponseCode.DUPLICATE_RESOURCE);

        verify(bayReservationRepository, never()).save(any());
    }
}
