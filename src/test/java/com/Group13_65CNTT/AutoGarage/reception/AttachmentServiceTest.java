package com.Group13_65CNTT.AutoGarage.reception;

import com.Group13_65CNTT.AutoGarage.reception.dto.AttachmentResponse;
import com.Group13_65CNTT.AutoGarage.reception.entity.Attachment;
import com.Group13_65CNTT.AutoGarage.reception.entity.AttachmentCategory;
import com.Group13_65CNTT.AutoGarage.reception.mapper.VehicleCheckMapper;
import com.Group13_65CNTT.AutoGarage.reception.repository.AttachmentRepository;
import com.Group13_65CNTT.AutoGarage.reception.service.AttachmentServiceImpl;
import com.Group13_65CNTT.AutoGarage.shared.exception.BusinessRuleException;
import com.Group13_65CNTT.AutoGarage.common.enums.ResponseCode;
import com.Group13_65CNTT.AutoGarage.common.exception.BusinessException;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AttachmentServiceTest {
    @Mock private com.Group13_65CNTT.AutoGarage.reception.repository.VehicleCheckRepository vehicleCheckRepository;
    @Mock private com.Group13_65CNTT.AutoGarage.common.security.CustomerScope customerScope;
    @org.mockito.Spy private com.Group13_65CNTT.AutoGarage.common.config.WorkshopProperties workshop = new com.Group13_65CNTT.AutoGarage.common.config.WorkshopProperties();

    @Mock
    private AttachmentRepository attachmentRepository;

    @Spy
    private VehicleCheckMapper vehicleCheckMapper = new VehicleCheckMapper();

    @InjectMocks
    private AttachmentServiceImpl attachmentService;

    private MockMultipartFile validImageFile;

    @BeforeEach
    void setUp() {
        validImageFile = new MockMultipartFile(
                "file",
                "car_scratch.jpg",
                "image/jpeg",
                "dummy image content bytes".getBytes()
        );
    }

    @Test
    void forbiddenAttachmentReadDoesNotLoadMetadata() {
        doThrow(new BusinessException(ResponseCode.FORBIDDEN)).when(customerScope)
                .requireResource(com.Group13_65CNTT.AutoGarage.common.enums.AccessResource.ATTACHMENT, 10L);

        assertThrows(BusinessException.class, () -> attachmentService.getAttachmentById(10L));
        verifyNoInteractions(attachmentRepository);
    }

    @Test
    void uploadRespectsConfiguredLimit() {
        workshop.setMaxAttachmentBytes(1);
        assertThrows(BusinessRuleException.class, () -> attachmentService.uploadAttachment(
                100L, 1L, null, AttachmentCategory.CHECK_IN, 2L, validImageFile));
        verifyNoInteractions(attachmentRepository);
    }

    @Test
    void uploadRejectsMissingActorWithoutSaving() {
        BusinessException failure = assertThrows(BusinessException.class, () ->
                attachmentService.uploadAttachment(100L, 1L, null, AttachmentCategory.CHECK_IN, null, validImageFile));
        assertEquals(ResponseCode.UNAUTHORIZED, failure.getResponseCode());
        verifyNoInteractions(attachmentRepository);
    }

    @Test
    void uploadReadFailurePreservesCauseAndServerError() throws IOException {
        MultipartFile unreadable = mock(MultipartFile.class);
        when(unreadable.isEmpty()).thenReturn(false);
        when(unreadable.getSize()).thenReturn(20L);
        when(unreadable.getContentType()).thenReturn("image/jpeg");
        IOException unavailable = new IOException("private storage details");
        when(unreadable.getInputStream()).thenThrow(unavailable);

        BusinessException failure = assertThrows(BusinessException.class, () ->
                attachmentService.uploadAttachment(100L, 1L, null, AttachmentCategory.CHECK_IN, 2L, unreadable));
        assertEquals(ResponseCode.INTERNAL_SERVER_ERROR, failure.getResponseCode());
        assertSame(unavailable, failure.getCause());
        assertFalse(failure.getMessage().contains("private storage details"));
        verifyNoInteractions(attachmentRepository);
    }

    @Test
    @DisplayName("TV2-08: Tải lên tệp ảnh thành công, sinh sha256 và storageKey an toàn")
    void testUploadAttachment_Success() {
        when(attachmentRepository.save(any(Attachment.class))).thenAnswer(invocation -> {
            Attachment a = invocation.getArgument(0);
            a.setId(10L);
            return a;
        });

        AttachmentResponse response = attachmentService.uploadAttachment(
                100L, 1L, null, AttachmentCategory.CHECK_IN, 2L, validImageFile
        );

        assertNotNull(response);
        assertEquals(10L, response.getId());
        assertEquals("car_scratch.jpg", response.getOriginalFilename());
        assertEquals("image/jpeg", response.getContentType());
        assertNotNull(response.getSha256());
        assertNotNull(response.getStorageKey());
        assertTrue(response.getStorageKey().startsWith("attachments/check_in/"));
        assertFalse(response.getStorageKey().contains("car_scratch.jpg")); // Không dùng filename tùy ý
        verify(attachmentRepository, times(1)).save(any(Attachment.class));
    }

    @Test
    @DisplayName("TV2-08: Từ chối tải lên tệp rỗng")
    void testUploadAttachment_EmptyFile() {
        MockMultipartFile emptyFile = new MockMultipartFile("file", "test.jpg", "image/jpeg", new byte[0]);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () ->
                attachmentService.uploadAttachment(100L, 1L, null, AttachmentCategory.CHECK_IN, 2L, emptyFile));
        assertTrue(ex.getMessage().contains("không được để trống"));
        verify(attachmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("TV2-08: Từ chối MIME type nguy hiểm hoặc không hợp lệ")
    void testUploadAttachment_InvalidMimeType() {
        MockMultipartFile exeFile = new MockMultipartFile("file", "malware.exe", "application/x-msdownload", "evil".getBytes());

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () ->
                attachmentService.uploadAttachment(100L, 1L, null, AttachmentCategory.CHECK_IN, 2L, exeFile));
        assertTrue(ex.getMessage().contains("không được hỗ trợ"));
        verify(attachmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("TV2-08: Từ chối tệp khi không gắn với phiếu sửa chữa hoặc bảo hành")
    void testUploadAttachment_NoTarget() {
        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () ->
                attachmentService.uploadAttachment(null, null, null, AttachmentCategory.CHECK_IN, 2L, validImageFile));
        assertTrue(ex.getMessage().contains("phải gắn với"));
        verify(attachmentRepository, never()).save(any());
    }
}
