import { Calendar, CheckCircle2, Clock, Gauge, Lock, Paperclip } from 'lucide-react';
import { Button, Loading, QueryError, Sheet } from '../../../shared/components/ui';
import { useVehicleTimelineQuery } from '../hooks/useVehicles';
import { money, formatDateTime } from '../../../shared/utils/format';

export function VehicleTimelineModal({
  vehicleId,
  open,
  onClose,
}: {
  vehicleId: string | null;
  open: boolean;
  onClose: () => void;
}) {
  const {
    data: timeline,
    isLoading,
    isError,
    error,
    refetch,
  } = useVehicleTimelineQuery(open ? vehicleId : null);

  return (
    <Sheet
      open={open}
      onClose={onClose}
      placement="center"
      size="lg"
      title="Dòng thời gian sửa chữa & bảo dưỡng"
      description={
        timeline
          ? `Phương tiện: ${timeline.licensePlate} (${timeline.brand} ${timeline.model}) • ODO: ${timeline.currentOdometer.toLocaleString('vi-VN')} km`
          : 'Lịch sử bảo dưỡng và sửa chữa kỹ thuật của phương tiện'
      }
    >
      <div className="editor-form space-y-4">
        {isLoading ? (
          <Loading label="Đang tải dòng thời gian sửa chữa…" />
        ) : isError ? (
          <QueryError error={error} retry={() => refetch()} />
        ) : !timeline || timeline.historyEntries.length === 0 ? (
          <div className="text-center py-12 text-muted-foreground">
            <Clock className="mx-auto mb-3 opacity-40" size={36} />
            <p className="text-base font-medium">Chưa có lịch sử dịch vụ sửa chữa</p>
            <p className="text-xs mt-1">
              Phương tiện này chưa phát sinh lệnh sửa chữa nào trong hệ thống.
            </p>
          </div>
        ) : (
          <div className="relative border-l-2 border-primary/20 ml-4 pl-6 space-y-6">
            {timeline.historyEntries.map((entry) => (
              <div key={entry.repairOrderId} className="relative group">
                {/* Timeline bullet */}
                <span className="absolute -left-[31px] top-1 flex h-4 w-4 items-center justify-center rounded-full bg-primary ring-4 ring-background" />

                {/* Header mốc thời gian */}
                <div className="flex flex-wrap items-center justify-between gap-2 mb-2">
                  <div className="flex items-center gap-2">
                    <span className="font-semibold text-base text-foreground">
                      Lệnh sửa chữa #{entry.orderCode}
                    </span>
                    <span
                      className={`badge text-xs ${
                        entry.isCurrentOwnerRecord ? 'badge-primary' : 'badge-neutral'
                      }`}
                    >
                      {entry.isCurrentOwnerRecord ? 'Chủ xe hiện tại' : 'Chủ sở hữu trước'}
                    </span>
                  </div>

                  <div className="flex items-center gap-4 text-xs text-muted-foreground">
                    <span className="flex items-center gap-1">
                      <Calendar size={13} />
                      {formatDateTime(entry.receivedAt)}
                    </span>
                    <span className="flex items-center gap-1">
                      <Gauge size={13} />
                      {entry.odometerIn.toLocaleString('vi-VN')} km
                    </span>
                  </div>
                </div>

                {/* Card nội dung mốc sửa chữa */}
                <div className="panel p-4 space-y-3">
                  {/* Yêu cầu & Chẩn đoán */}
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-3 text-sm">
                    {entry.customerComplaint && (
                      <div>
                        <span className="text-xs text-muted-foreground block font-medium">
                          Hiện tượng / Yêu cầu:
                        </span>
                        <p className="text-foreground mt-0.5">{entry.customerComplaint}</p>
                      </div>
                    )}
                    {entry.diagnosis && (
                      <div>
                        <span className="text-xs text-muted-foreground block font-medium">
                          Chẩn đoán kỹ thuật:
                        </span>
                        <p className="text-foreground mt-0.5">{entry.diagnosis}</p>
                      </div>
                    )}
                  </div>

                  {/* Công việc đã thực hiện */}
                  {entry.tasksDone && entry.tasksDone.length > 0 && (
                    <div>
                      <span className="text-xs text-muted-foreground block font-medium mb-1">
                        Hạng mục công việc đã thực hiện ({entry.tasksDone.length}):
                      </span>
                      <ul className="grid grid-cols-1 md:grid-cols-2 gap-1.5 text-xs text-foreground">
                        {entry.tasksDone.map((task, idx) => (
                          <li key={idx} className="flex items-start gap-1.5">
                            <CheckCircle2 size={13} className="text-success shrink-0 mt-0.5" />
                            <span>{task}</span>
                          </li>
                        ))}
                      </ul>
                    </div>
                  )}

                  {/* Phụ tùng linh kiện đã thay thế */}
                  {entry.partsReplaced && entry.partsReplaced.length > 0 && (
                    <div>
                      <span className="text-xs text-muted-foreground block font-medium mb-1">
                        Vật tư & Phụ tùng thay thế ({entry.partsReplaced.length}):
                      </span>
                      <div className="flex flex-wrap gap-1.5">
                        {entry.partsReplaced.map((part, idx) => (
                          <span
                            key={idx}
                            className="badge badge-neutral text-xs"
                          >
                            {part}
                          </span>
                        ))}
                      </div>
                    </div>
                  )}

                  {/* File đính kèm kỹ thuật */}
                  {entry.technicalAttachments && entry.technicalAttachments.length > 0 && (
                    <div>
                      <span className="text-xs text-muted-foreground block font-medium mb-1">
                        Tài liệu & Ảnh kỹ thuật ({entry.technicalAttachments.length}):
                      </span>
                      <div className="flex flex-wrap gap-2">
                        {entry.technicalAttachments.map((att) => (
                          <a
                            key={att.id}
                            href={att.fileUrl}
                            target="_blank"
                            rel="noreferrer"
                            className="inline-flex items-center gap-1 text-xs text-primary hover:underline bg-primary/5 px-2 py-1 rounded border border-primary/20"
                          >
                            <Paperclip size={12} />
                            <span>{att.fileName}</span>
                          </a>
                        ))}
                      </div>
                    </div>
                  )}

                  {/* Chi phí thanh toán */}
                  <div className="pt-2 border-t flex items-center justify-between text-xs">
                    <span className="text-muted-foreground">Tổng chi phí dịch vụ:</span>
                    {entry.totalAmount !== null ? (
                      <span className="font-semibold text-sm text-foreground money">
                        {money(entry.totalAmount)}
                      </span>
                    ) : (
                      <span className="badge badge-warning text-xs inline-flex items-center gap-1">
                        <Lock size={11} />
                        Bảo mật tài chính theo quy định đổi chủ xe
                      </span>
                    )}
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}

        <div className="modal-form-actions">
          <Button variant="secondary" onClick={onClose}>
            Đóng
          </Button>
        </div>
      </div>
    </Sheet>
  );
}
