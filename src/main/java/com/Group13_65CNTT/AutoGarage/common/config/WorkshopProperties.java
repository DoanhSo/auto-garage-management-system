package com.Group13_65CNTT.AutoGarage.common.config;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import java.time.Duration;
import java.nio.file.Path;

@Getter
@Setter
@Validated
@ConfigurationProperties("garage.workshop")
public class WorkshopProperties {
    @NotNull
    private Path attachmentDirectory = Path.of("data", "attachments");

    @Min(1)
    private int defaultServiceMinutes = 60;

    @Min(1)
    @jakarta.validation.constraints.Max(Integer.MAX_VALUE - 1)
    private long maxAttachmentBytes = 15L * 1024 * 1024;

    @Min(1)
    private long maxUploadRequestBytes = 16L * 1024 * 1024;

    /** Thư mục lưu nội dung tệp đính kèm; storageKey được nối tương đối vào thư mục này. */
    public String getAttachmentStorageDir() { return attachmentDirectory.toString(); }
    public void setAttachmentStorageDir(String directory) { attachmentDirectory = Path.of(directory); }

    @NotNull
    private Duration reservationHold = Duration.ofMinutes(30);

    @AssertTrue(message = "Reservation hold must be positive")
    public boolean isReservationHoldValid() {
        return reservationHold != null && !reservationHold.isZero() && !reservationHold.isNegative();
    }

    @AssertTrue(message = "Upload request limit must exceed the attachment limit for multipart metadata")
    public boolean isUploadRequestLimitValid() {
        return maxUploadRequestBytes > maxAttachmentBytes;
    }
}
