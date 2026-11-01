import { useMemo, useState } from 'react';
import { AlertTriangle, ClipboardList, FileText, ImageOff, Upload, Wrench } from 'lucide-react';
import { useToast } from '../../../app/providers/ToastProvider';
import { errorMessage } from '../../../shared/api/errors';
import { Badge, Button, Loading, Modal, QueryError } from '../../../shared/components/ui';
import { formatDateTime } from '../../../shared/utils/format';
import type { Attachment, AttachmentCategory } from '../../../shared/types/contracts';
import {
  useAttachmentBlobUrl,
  useOrderAttachmentsQuery,
  useUploadOrderAttachmentMutation,
} from '../../reception/hooks/useReception';
import {
  AttachmentFilePicker,
  formatFileSize,
} from '../../reception/components/AttachmentFilePicker';

export interface CheckInSummary {
  exterior?: string;
  interior?: string;
  warningLights?: string;
  notes?: string | null;
}

const categoryMeta: Record<
  AttachmentCategory,
  { label: string; variant: 'primary' | 'warning' | 'success' | 'secondary' }
> = {
  CHECK_IN: { label: 'Ảnh tiếp nhận', variant: 'primary' },
  DIAGNOSIS: { label: 'Ảnh chẩn đoán lỗi', variant: 'warning' },
  QC: { label: 'Ảnh kiểm định QC', variant: 'success' },
  HANDOVER: { label: 'Ảnh bàn giao', variant: 'secondary' },
  WARRANTY: { label: 'Ảnh bảo hành', variant: 'secondary' },
};

const GROUP_ORDER: AttachmentCategory[] = ['CHECK_IN', 'DIAGNOSIS', 'QC', 'HANDOVER', 'WARRANTY'];
const isPdf = (a: Attachment) => a.contentType === 'application/pdf';

function AttachmentImage({
  attachment,
  className,
}: {
  attachment: Attachment;
  className?: string;
}) {
  const { url, failed } = useAttachmentBlobUrl(isPdf(attachment) ? null : attachment.id);
  if (isPdf(attachment))
    return (
      <span className={`attachment-thumb-icon ${className ?? ''}`}>
        <FileText size={26} />
      </span>
    );
  if (failed)
    return (
      <span className={`attachment-thumb-icon ${className ?? ''}`}>
        <ImageOff size={22} />
      </span>
    );
  if (!url) return <span className={`attachment-thumb-icon ${className ?? ''}`} aria-busy="true" />;
  return <img className={className} src={url} alt={attachment.fileName} />;
}

/** Bộ sưu tập ảnh của lệnh sửa chữa kèm hộp thoại đối chiếu ảnh ↔ triệu chứng ↔ kết luận. */
export function DiagnosisAttachmentGallery({
  repairOrderId,
  customerComplaint,
  diagnosis,
  checkIn,
  canUpload,
}: {
  repairOrderId: string;
  customerComplaint?: string | null;
  diagnosis?: string | null;
  checkIn?: CheckInSummary | null;
  canUpload: boolean;
}) {
  const notify = useToast();
  const attachments = useOrderAttachmentsQuery(repairOrderId);
  const upload = useUploadOrderAttachmentMutation();
  const [category, setCategory] = useState<AttachmentCategory>('DIAGNOSIS');
  const [files, setFiles] = useState<File[]>([]);
  const [viewing, setViewing] = useState<Attachment | null>(null);

  const groups = useMemo(() => {
    const list = attachments.data ?? [];
    return GROUP_ORDER.map((key) => ({
      key,
      items: list.filter((a) => a.category === key),
    })).filter((group) => group.items.length > 0);
  }, [attachments.data]);

  const handleUpload = async () => {
    let failed = 0;
    for (const file of files) {
      try {
        await upload.mutateAsync({ repairOrderId, category, file });
      } catch (err: unknown) {
        failed += 1;
        if (failed === 1) notify(errorMessage(err));
      }
    }
    if (failed === 0) notify(`Đã tải lên ${files.length} ảnh.`);
    setFiles([]);
  };

  return (
    <section className="panel diagnosis-gallery">
      <div className="diagnosis-gallery-head">
        <strong>Ảnh hiện trạng & bằng chứng chẩn đoán</strong>
        <span className="muted">
          Bấm vào ảnh để đối chiếu với mô tả của khách và kết luận kỹ thuật.
        </span>
      </div>

      {canUpload && (
        <div className="diagnosis-gallery-upload">
          <select
            className="select"
            aria-label="Phân loại ảnh"
            value={category}
            disabled={upload.isPending}
            onChange={(e) => setCategory(e.target.value as AttachmentCategory)}
          >
            <option value="DIAGNOSIS">Ảnh chẩn đoán lỗi</option>
            <option value="CHECK_IN">Ảnh hiện trạng tiếp nhận</option>
          </select>
          <AttachmentFilePicker
            files={files}
            onChange={setFiles}
            disabled={upload.isPending}
            label="Chọn ảnh"
          />
          {files.length > 0 && (
            <Button
              variant="primary"
              pending={upload.isPending}
              onClick={() => void handleUpload()}
            >
              <Upload size={15} /> Tải lên {files.length} ảnh
            </Button>
          )}
        </div>
      )}

      {attachments.isLoading ? (
        <Loading label="Đang tải ảnh đính kèm…" />
      ) : attachments.isError ? (
        <QueryError error={attachments.error} retry={() => attachments.refetch()} />
      ) : groups.length === 0 ? (
        <p className="empty-state">Lệnh sửa chữa chưa có ảnh đính kèm.</p>
      ) : (
        groups.map((group) => (
          <div key={group.key} className="diagnosis-gallery-group">
            <Badge variant={categoryMeta[group.key].variant}>
              {categoryMeta[group.key].label} ({group.items.length})
            </Badge>
            <ul className="attachment-thumbs">
              {group.items.map((a) => (
                <li key={a.id} className="attachment-thumb">
                  <button
                    type="button"
                    className="attachment-thumb-open"
                    onClick={() => setViewing(a)}
                    aria-label={`Xem đối chiếu ${a.fileName}`}
                  >
                    <AttachmentImage attachment={a} />
                  </button>
                  <span className="attachment-thumb-meta">
                    <span className="attachment-thumb-name">{a.fileName}</span>
                    <span>{formatDateTime(a.createdAt)}</span>
                  </span>
                </li>
              ))}
            </ul>
          </div>
        ))
      )}

      <Modal
        open={!!viewing}
        onOpenChange={(open) => !open && setViewing(null)}
        size="xl"
        title="Đối chiếu ảnh thực tế & chẩn đoán"
        description={viewing ? `${categoryMeta[viewing.category].label} · ${viewing.fileName}` : ''}
      >
        {viewing && (
          <div className="compare-layout">
            <figure className="compare-media">
              <AttachmentImage attachment={viewing} className="compare-image" />
              <figcaption>
                <span>{formatDateTime(viewing.createdAt)}</span>
                {viewing.fileSize != null && <span>{formatFileSize(viewing.fileSize)}</span>}
                {viewing.sha256 && (
                  <code title={viewing.sha256}>SHA-256: {viewing.sha256.slice(0, 16)}…</code>
                )}
              </figcaption>
            </figure>
            <div className="compare-notes">
              <div className="compare-block">
                <h3>
                  <AlertTriangle size={15} /> Khách hàng mô tả
                </h3>
                <p>{customerComplaint || 'Không ghi nhận triệu chứng.'}</p>
              </div>
              <div className="compare-block">
                <h3>
                  <ClipboardList size={15} /> Hiện trạng khi nhận xe
                </h3>
                {checkIn ? (
                  <dl>
                    <dt>Ngoại thất</dt>
                    <dd>{checkIn.exterior || '—'}</dd>
                    <dt>Nội thất</dt>
                    <dd>{checkIn.interior || '—'}</dd>
                    <dt>Đèn cảnh báo</dt>
                    <dd>{checkIn.warningLights || '—'}</dd>
                    {checkIn.notes && (
                      <>
                        <dt>Ghi chú</dt>
                        <dd>{checkIn.notes}</dd>
                      </>
                    )}
                  </dl>
                ) : (
                  <p>Chưa lập biên bản kiểm tra tiếp nhận.</p>
                )}
              </div>
              <div className="compare-block">
                <h3>
                  <Wrench size={15} /> Kết luận chẩn đoán kỹ thuật
                </h3>
                <p>{diagnosis || 'Chưa có kết luận chẩn đoán.'}</p>
              </div>
            </div>
          </div>
        )}
      </Modal>
    </section>
  );
}
