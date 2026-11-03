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
    @Mock private com.Group13_65CNTT.AutoGarage.reception.service.impl.AttachmentStorage storage;
    @Mock private com.Group13_65CNTT.AutoGarage.common.security.CurrentUserProvider currentUser;
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
        lenient().when(currentUser.userId()).thenReturn(2L);
        lenient().when(currentUser.role()).thenReturn(com.Group13_65CNTT.AutoGarage.common.enums.Role.TECHNICIAN);
        var check = new com.Group13_65CNTT.AutoGarage.reception.entity.VehicleCheck();
        check.setRepairOrderId(100L);
        check.setCheckType(com.Group13_65CNTT.AutoGarage.reception.entity.CheckType.CHECK_IN);
        lenient().when(vehicleCheckRepository.findById(1L)).thenReturn(java.util.Optional.of(check));
        validImageFile = new MockMultipartFile(
                "file",
                "car_scratch.jpg",
                "image/jpeg",
                new byte[]{(byte)0xff, (byte)0xd8, (byte)0xff, 0x00}
        );
    }

    @Test
    void storedContentIsServedBackWithOriginalType() throws Exception {
        var saved = new Attachment(); saved.setId(10L); saved.setStorageKey("attachments/check_in/photo.jpg");
        saved.setContentType("image/jpeg"); saved.setOriginalFilename("photo.jpg");
        byte[] bytes = validImageFile.getBytes();
        saved.setSha256(java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes)));
        when(attachmentRepository.findById(10L)).thenReturn(java.util.Optional.of(saved));
        when(storage.read(saved.getStorageKey())).thenReturn(bytes);
        var content = attachmentService.loadAttachmentContent(10L);
        assertArrayEquals(bytes, content.bytes());
        assertEquals("image/jpeg", content.contentType());
    }

    @Test
    void missingSeedFileFallsBackToEscapedSvgPlaceholder() {
        var saved = new Attachment(); saved.setId(11L); saved.setStorageKey("attachments/seed.jpg");
        saved.setOriginalFilename("<script>x</script>.jpg"); saved.setCategory(AttachmentCategory.DIAGNOSIS);
        when(attachmentRepository.findById(11L)).thenReturn(java.util.Optional.of(saved));
        when(storage.read(saved.getStorageKey())).thenThrow(new BusinessException(ResponseCode.RESOURCE_NOT_FOUND));
        var content = attachmentService.loadAttachmentContent(11L);
        assertEquals("image/svg+xml", content.contentType());
        assertFalse(new String(content.bytes(), java.nio.charset.StandardCharsets.UTF_8).contains("<script>"));
    }

    @Test
    void storageKeyEscapingTheRootIsRejected() {
        var realStorage = new com.Group13_65CNTT.AutoGarage.reception.service.impl.AttachmentStorage(workshop);
        var failure = assertThrows(BusinessException.class, () -> realStorage.read("../../etc/passwd"));
        assertEquals(ResponseCode.BAD_REQUEST, failure.getResponseCode());
    }

    @Test
    void uploadedMimeMustMatchContent() {
        var fake = new MockMultipartFile("file", "image.jpg", "image/jpeg", "not an image".getBytes());
        assertEquals(ResponseCode.UNSUPPORTED_MEDIA_TYPE, assertThrows(BusinessException.class, () ->
                attachmentService.uploadAttachment(100L, 1L, null, AttachmentCategory.CHECK_IN, 2L, fake)).getResponseCode());
        verifyNoInteractions(storage, attachmentRepository);
    }

    @Test
    void checkFromAnotherOrderCannotBeAttached() {
        var foreign = new com.Group13_65CNTT.AutoGarage.reception.entity.VehicleCheck();
        foreign.setRepairOrderId(999L);
        foreign.setCheckType(com.Group13_65CNTT.AutoGarage.reception.entity.CheckType.CHECK_IN);
        when(vehicleCheckRepository.findById(1L)).thenReturn(java.util.Optional.of(foreign));
        assertEquals(ResponseCode.BAD_REQUEST, assertThrows(BusinessException.class, () ->
                attachmentService.uploadAttachment(100L, 1L, null, AttachmentCategory.CHECK_IN, 2L, validImageFile)).getResponseCode());
        verifyNoInteractions(storage, attachmentRepository);
    }

    @Test
    void downloadRequiresPermissionBeforeReadingStoredBytes() {
        doThrow(new BusinessException(ResponseCode.FORBIDDEN)).when(customerScope)
                .requireResource(com.Group13_65CNTT.AutoGarage.common.enums.AccessResource.ATTACHMENT, 10L);
        assertThrows(BusinessException.class, () -> attachmentService.downloadAttachment(10L));
        verifyNoInteractions(storage, attachmentRepository);
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
        assertThrows(BusinessException.class, () -> attachmentService.uploadAttachment(
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
        when(attachmentRepository.saveAndFlush(any(Attachment.class))).thenAnswer(invocation -> {
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
        verify(attachmentRepository, times(1)).saveAndFlush(any(Attachment.class));
    }

    @Test
    @DisplayName("TV2-08: Từ chối tải lên tệp rỗng")
    void testUploadAttachment_EmptyFile() {
        MockMultipartFile emptyFile = new MockMultipartFile("file", "test.jpg", "image/jpeg", new byte[0]);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () ->
                attachmentService.uploadAttachment(100L, 1L, null, AttachmentCategory.CHECK_IN, 2L, emptyFile));
        assertTrue(ex.getMessage().contains("không được để trống"));
        verify(attachmentRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("TV2-08: Từ chối MIME type nguy hiểm hoặc không hợp lệ")
    void testUploadAttachment_InvalidMimeType() {
        MockMultipartFile exeFile = new MockMultipartFile("file", "malware.exe", "application/x-msdownload", "evil".getBytes());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                attachmentService.uploadAttachment(100L, 1L, null, AttachmentCategory.CHECK_IN, 2L, exeFile));
        assertEquals(ResponseCode.UNSUPPORTED_MEDIA_TYPE, ex.getResponseCode());
        verify(attachmentRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("TV2-08: Từ chối tệp khi không gắn với phiếu sửa chữa hoặc bảo hành")
    void testUploadAttachment_NoTarget() {
        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () ->
                attachmentService.uploadAttachment(null, null, null, AttachmentCategory.CHECK_IN, 2L, validImageFile));
        assertTrue(ex.getMessage().contains("phải gắn với"));
        verify(attachmentRepository, never()).saveAndFlush(any());
    }
}
