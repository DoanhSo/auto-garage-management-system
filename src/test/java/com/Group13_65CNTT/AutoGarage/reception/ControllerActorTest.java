package com.Group13_65CNTT.AutoGarage.reception;

import com.Group13_65CNTT.AutoGarage.common.security.CurrentUserProvider;
import com.Group13_65CNTT.AutoGarage.reception.controller.AttachmentController;
import com.Group13_65CNTT.AutoGarage.reception.controller.BookingController;
import com.Group13_65CNTT.AutoGarage.reception.dto.AttachmentResponse;
import com.Group13_65CNTT.AutoGarage.reception.dto.BookingResponse;
import com.Group13_65CNTT.AutoGarage.reception.dto.CreateBookingRequest;
import com.Group13_65CNTT.AutoGarage.reception.dto.CancelBookingRequest;
import com.Group13_65CNTT.AutoGarage.reception.dto.RescheduleBookingRequest;
import com.Group13_65CNTT.AutoGarage.reception.entity.AttachmentCategory;
import com.Group13_65CNTT.AutoGarage.reception.service.AttachmentService;
import com.Group13_65CNTT.AutoGarage.reception.service.BookingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ControllerActorTest {
    @Mock CurrentUserProvider currentUserProvider;
    @Mock BookingService bookingService;
    @Mock AttachmentService attachmentService;
    @InjectMocks BookingController bookingController;
    @InjectMocks AttachmentController attachmentController;

    @Test
    void bookingCreationOverwritesCallerSuppliedActor() {
        when(currentUserProvider.userId()).thenReturn(42L);
        CreateBookingRequest request = new CreateBookingRequest();
        request.setCreatedBy(1L);
        BookingResponse response = new BookingResponse();
        response.setId(9L);
        when(bookingService.createBooking(request)).thenReturn(response);

        bookingController.createBooking(request, new MockHttpServletRequest());

        verify(bookingService).createBooking(org.mockito.ArgumentMatchers.argThat(value -> value.getCreatedBy().equals(42L)));
    }

    @Test
    void bookingActionsUseAuthenticatedActor() {
        when(currentUserProvider.userId()).thenReturn(42L);
        bookingController.confirmBooking(9L, 1L, new MockHttpServletRequest());
        bookingController.markNoShow(10L, 1L, new MockHttpServletRequest());
        var reschedule = new RescheduleBookingRequest();
        reschedule.setActorUserId(1L);
        var cancel = new CancelBookingRequest();
        cancel.setActorUserId(1L);
        bookingController.rescheduleBooking(11L, reschedule, new MockHttpServletRequest());
        bookingController.cancelBooking(12L, cancel, new MockHttpServletRequest());
        verify(bookingService).confirmBooking(9L, 42L);
        verify(bookingService).markNoShow(10L, 42L);
        verify(bookingService).rescheduleBooking(eq(11L), org.mockito.ArgumentMatchers.argThat(value -> value.getActorUserId().equals(42L)));
        verify(bookingService).cancelBooking(eq(12L), org.mockito.ArgumentMatchers.argThat(value -> value.getActorUserId().equals(42L)));
    }

    @Test
    void uploadUsesAuthenticatedActor() {
        when(currentUserProvider.userId()).thenReturn(42L);
        AttachmentResponse response = new AttachmentResponse();
        response.setId(9L);
        var file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", new byte[]{1});
        when(attachmentService.uploadAttachment(eq(5L), eq(null), eq(null), eq(AttachmentCategory.CHECK_IN), eq(42L), any()))
                .thenReturn(response);

        attachmentController.uploadAttachment(5L, null, null, AttachmentCategory.CHECK_IN, 1L, file,
                new MockHttpServletRequest());

        verify(attachmentService).uploadAttachment(5L, null, null, AttachmentCategory.CHECK_IN, 42L, file);
    }
}
