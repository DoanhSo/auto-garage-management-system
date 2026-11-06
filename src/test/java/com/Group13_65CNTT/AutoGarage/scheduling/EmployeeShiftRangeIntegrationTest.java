package com.Group13_65CNTT.AutoGarage.scheduling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.Group13_65CNTT.AutoGarage.IntegrationTestSupport;
import com.Group13_65CNTT.AutoGarage.scheduling.constant.ShiftPolicy;
import com.Group13_65CNTT.AutoGarage.scheduling.dto.response.ShiftResponse;
import com.Group13_65CNTT.AutoGarage.scheduling.entity.EmployeeShift;
import com.Group13_65CNTT.AutoGarage.scheduling.entity.ShiftStatus;
import com.Group13_65CNTT.AutoGarage.scheduling.repository.EmployeeShiftRepository;
import com.Group13_65CNTT.AutoGarage.scheduling.service.SchedulingService;
import com.Group13_65CNTT.AutoGarage.shared.exception.BusinessRuleException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/** Shifts listed by garage-local date range, including shifts that run across midnight. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@RequiredArgsConstructor
class EmployeeShiftRangeIntegrationTest extends IntegrationTestSupport {
    private static final long ADMIN_ID = 910101L;
    private static final long TECH_ID = 910102L;
    private static final long EMPLOYEE_A = 910101L;
    private static final long EMPLOYEE_B = 910102L;

    private final JdbcTemplate jdbc;
    private final EmployeeShiftRepository shifts;
    private final SchedulingService service;

    private static Instant local(String isoLocal) {
        return LocalDateTime.parse(isoLocal).atZone(ShiftPolicy.ZONE).toInstant();
    }

    private EmployeeShift save(long employeeId, String start, String end, ShiftStatus status) {
        return shifts.saveAndFlush(EmployeeShift.builder()
                .employeeId(employeeId)
                .startsAt(local(start))
                .endsAt(local(end))
                .status(status)
                .createdBy(ADMIN_ID)
                .build());
    }

    private EmployeeShift inWeekA;
    private EmployeeShift spillIn;
    private EmployeeShift overnightOut;
    private EmployeeShift outside;
    private EmployeeShift cancelled;
    private EmployeeShift otherEmployee;

    @BeforeEach
    void fixtures() {
        jdbc.update("INSERT INTO users(id,username,password_hash,full_name,role)"
                + " VALUES(910101,'shift_admin','test','Admin','ADMIN'),(910102,'shift_tech','test','Tech','TECHNICIAN')");
        jdbc.update("INSERT INTO employees(id,user_id,employee_code) VALUES(910101,910102,'RANGE_A')");
        jdbc.update("INSERT INTO users(id,username,password_hash,full_name,role) VALUES(910103,'shift_tech2','test','Tech2','TECHNICIAN')");
        jdbc.update("INSERT INTO employees(id,user_id,employee_code) VALUES(910102,910103,'RANGE_B')");

        spillIn = save(EMPLOYEE_A, "2026-11-01T22:00:00", "2026-11-02T06:00:00", ShiftStatus.ACTIVE);
        inWeekA = save(EMPLOYEE_A, "2026-11-03T08:00:00", "2026-11-03T17:00:00", ShiftStatus.ACTIVE);
        overnightOut = save(EMPLOYEE_A, "2026-11-08T22:00:00", "2026-11-09T06:00:00", ShiftStatus.ACTIVE);
        outside = save(EMPLOYEE_A, "2026-11-20T08:00:00", "2026-11-20T17:00:00", ShiftStatus.ACTIVE);
        cancelled = save(EMPLOYEE_A, "2026-11-04T08:00:00", "2026-11-04T17:00:00", ShiftStatus.CANCELLED);
        otherEmployee = save(EMPLOYEE_B, "2026-11-05T08:00:00", "2026-11-05T17:00:00", ShiftStatus.ACTIVE);
    }

    private java.util.List<Long> ids(LocalDate from, LocalDate to, Long employeeId, ShiftStatus status) {
        return service.getShifts(employeeId, status, from, to, PageRequest.of(0, 50)).getContent().stream()
                .map(ShiftResponse::getId)
                .toList();
    }

    @Test
    @DisplayName("Xem tuần 02-08/11: gồm ca qua nửa đêm chạm đầu và cuối tuần, loại ca ngoài tuần")
    void weekRangeIncludesShiftsCrossingMidnightAtBothEdges() {
        var week = ids(LocalDate.parse("2026-11-02"), LocalDate.parse("2026-11-08"), null, null);

        assertThat(week).contains(spillIn.getId(), inWeekA.getId(), overnightOut.getId(), cancelled.getId(),
                otherEmployee.getId());
        assertThat(week).doesNotContain(outside.getId());
    }

    @Test
    @DisplayName("Lọc theo kỹ thuật viên và trạng thái trong khoảng ngày")
    void rangeCombinesWithEmployeeAndStatusFilters() {
        var from = LocalDate.parse("2026-11-02");
        var to = LocalDate.parse("2026-11-08");

        assertThat(ids(from, to, EMPLOYEE_B, null)).containsExactly(otherEmployee.getId());
        assertThat(ids(from, to, EMPLOYEE_A, ShiftStatus.CANCELLED)).containsExactly(cancelled.getId());
        assertThat(ids(from, to, EMPLOYEE_A, ShiftStatus.ACTIVE))
                .contains(spillIn.getId(), inWeekA.getId(), overnightOut.getId())
                .doesNotContain(cancelled.getId(), otherEmployee.getId());
    }

    @Test
    @DisplayName("Ca kết thúc đúng 00:00 ngày bắt đầu khoảng hoặc bắt đầu đúng 00:00 sau khoảng thì không tính")
    void rangeBoundariesAreHalfOpen() {
        var edge = save(EMPLOYEE_B, "2026-11-09T00:00:00", "2026-11-09T08:00:00", ShiftStatus.ACTIVE);
        var before = save(EMPLOYEE_B, "2026-11-01T20:00:00", "2026-11-02T00:00:00", ShiftStatus.ACTIVE);

        var week = ids(LocalDate.parse("2026-11-02"), LocalDate.parse("2026-11-08"), EMPLOYEE_B, null);

        assertThat(week).doesNotContain(edge.getId(), before.getId());
    }

    @Test
    @DisplayName("Chỉ có ngày bắt đầu: lấy mọi ca từ ngày đó trở đi")
    void openEndedRangeReturnsLaterShifts() {
        var fromNov10 = ids(LocalDate.parse("2026-11-10"), null, null, null);

        assertThat(fromNov10).contains(outside.getId()).doesNotContain(inWeekA.getId(), overnightOut.getId());
    }

    @Test
    @DisplayName("Ngày kết thúc trước ngày bắt đầu: báo lỗi nghiệp vụ")
    void invertedRangeIsRejected() {
        assertThatThrownBy(() -> ids(LocalDate.parse("2026-11-09"), LocalDate.parse("2026-11-02"), null, null))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("Không truyền khoảng ngày: hành vi cũ, trả cả ca xa")
    void withoutRangeBehavesAsBefore() {
        var all = service.getShifts(null, null, PageRequest.of(0, 50)).getContent().stream()
                .map(ShiftResponse::getId)
                .toList();

        assertThat(all).contains(outside.getId(), inWeekA.getId());
    }
}
