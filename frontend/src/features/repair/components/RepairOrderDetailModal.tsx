import { useState } from 'react';
import {
  Wrench,
  CheckCircle,
  Gauge,
  Plus,
  Send,
  Check,
  X,
  Car,
  ShieldCheck,
  UserCheck,
  Fuel,
  Calendar,
  AlertTriangle,
  ArrowRight,
} from 'lucide-react';
import { useToast } from '../../../app/providers/ToastProvider';
import { errorMessage } from '../../../shared/api/errors';
import { Button, Empty, Loading, QueryError, Sheet } from '../../../shared/components/ui';
import { formatDateTime, money } from '../../../shared/utils/format';
import type { RepairOrderStatus } from '../types/repairTypes';
import {
  useRepairOrderQuery,
  useRepairTasksQuery,
  useUpdateDiagnosisMutation,
  useTransitionStatusMutation,
  useCompleteTaskMutation,
} from '../hooks/useRepair';
import {
  useEstimatesQuery,
  useSendEstimateMutation,
  useDecideEstimateMutation,
} from '../../estimate/hooks/useEstimates';
import { useQualityChecksQuery } from '../../quality/hooks/useQuality';
import { RepairOrderStatusBadge } from './RepairOrderCards';
import { REPAIR_STATUS_LABELS } from './RepairOrderFilters';
import { CreateTaskModal } from './CreateTaskModal';
import { CreateEstimateModal } from './CreateEstimateModal';
import { PerformQCModal } from './PerformQCModal';
import { HandoverModal } from './HandoverModal';

type Tab = 'overview' | 'estimate' | 'tasks' | 'qc' | 'handover';

export function RepairOrderDetailModal({
  orderId,
  open,
  onClose,
}: {
  orderId: string | null;
  open: boolean;
  onClose: () => void;
}) {
  const notify = useToast();
  const [activeTab, setActiveTab] = useState<Tab>('overview');

  // Sub-modals
  const [createTaskOpen, setCreateTaskOpen] = useState(false);
  const [createEstimateOpen, setCreateEstimateOpen] = useState(false);
  const [performQCOpen, setPerformQCOpen] = useState(false);
  const [handoverOpen, setHandoverOpen] = useState(false);

  // Queries
  const { data: order, isLoading, isError, error, refetch } = useRepairOrderQuery(open ? orderId : null);
  const { data: tasks = [], isLoading: isLoadingTasks } = useRepairTasksQuery(open ? orderId : null);
  const { data: estimates = [], isLoading: isLoadingEstimates } = useEstimatesQuery(open ? orderId : null);
  const { data: qcChecks = [], isLoading: isLoadingQC } = useQualityChecksQuery(open ? orderId : null);

  // Mutations
  const updateDiagnosis = useUpdateDiagnosisMutation();
  const transitionStatus = useTransitionStatusMutation();
  const sendEstimate = useSendEstimateMutation();
  const decideEstimate = useDecideEstimateMutation();
  const completeTask = useCompleteTaskMutation();

  const [diagnosisText, setDiagnosisText] = useState('');
  const [hasEditedDiagnosis, setHasEditedDiagnosis] = useState(false);

  const currentDiagnosis = hasEditedDiagnosis ? diagnosisText : order?.diagnosis || '';

  const handleSaveDiagnosis = async () => {
    if (!order) return;
    try {
      await updateDiagnosis.mutateAsync({
        id: order.id,
        diagnosis: currentDiagnosis.trim(),
      });
      notify('Đã lưu kết quả chẩn đoán kỹ thuật thành công.');
      setHasEditedDiagnosis(false);
    } catch (err: unknown) {
      notify(errorMessage(err));
    }
  };

  const handleTransitionStatus = async (newStatus: RepairOrderStatus) => {
    if (!order) return;
    try {
      await transitionStatus.mutateAsync({
        id: order.id,
        newStatus,
      });
      notify(`Đã chuyển trạng thái: ${REPAIR_STATUS_LABELS[newStatus] || newStatus}`);
    } catch (err: unknown) {
      notify(errorMessage(err));
    }
  };

  const handleSendEstimate = async (estimateId: string | number) => {
    try {
      await sendEstimate.mutateAsync(estimateId);
      notify('Đã gửi báo giá cho khách hàng phê duyệt.');
    } catch (err: unknown) {
      notify(errorMessage(err));
    }
  };

  const handleDecideLine = async (
    estimateId: string | number,
    itemId: string | number,
    decision: 'APPROVED' | 'REJECTED',
  ) => {
    try {
      await decideEstimate.mutateAsync({
        id: estimateId,
        data: {
          lines: [{ estimateItemId: itemId, decision }],
        },
      });
      notify(decision === 'APPROVED' ? 'Đã duyệt hạng mục báo giá.' : 'Đã từ chối hạng mục báo giá.');
    } catch (err: unknown) {
      notify(errorMessage(err));
    }
  };

  const handleCompleteTask = async (taskId: string | number) => {
    try {
      await completeTask.mutateAsync({
        taskId,
        note: 'Hoàn thành công việc',
      });
      notify('Đã xác nhận hoàn thành công việc.');
    } catch (err: unknown) {
      notify(errorMessage(err));
    }
  };

  if (!open) return null;

  return (
    <Sheet
      open={open}
      onClose={onClose}
      placement="center"
      size="xl"
      title={order ? `Lệnh sửa chữa #${order.orderCode}` : 'Chi tiết lệnh sửa chữa'}
      description={
        order
          ? `Tiếp nhận lúc ${formatDateTime(order.receivedAt)} · Phương tiện #${order.vehicleId} · Khách hàng #${order.customerId}`
          : 'Xem thông tin chẩn đoán, báo giá, tiến độ thi công và bàn giao xe'
      }
    >
      <div className="editor-form space-y-4">
        {isLoading ? (
          <Loading label="Đang tải thông tin lệnh sửa chữa…" />
        ) : isError || !order ? (
          <QueryError error={error} retry={() => refetch()} />
        ) : (
          <>
            {/* Top Workflow Summary Banner */}
            <div className="p-3.5 bg-muted/15 border rounded-lg flex flex-wrap items-center justify-between gap-3">
              <div className="flex flex-wrap items-center gap-3">
                <div className="flex items-center gap-2">
                  <span className="text-xs font-medium text-muted-foreground">Trạng thái:</span>
                  <RepairOrderStatusBadge status={order.status} />
                </div>
                <div className="h-4 w-px bg-border hidden sm:block" />
                <div className="text-xs text-muted-foreground flex items-center gap-2">
                  <span>Phương tiện: <strong className="text-foreground">Xe #{order.vehicleId}</strong></span>
                  <span>•</span>
                  <span>ODO vào: <strong className="text-foreground">{order.odometerIn.toLocaleString('vi-VN')} km</strong></span>
                  <span>•</span>
                  <span>Tiếp nhận: <strong className="text-foreground">{formatDateTime(order.receivedAt)}</strong></span>
                </div>
              </div>

              <div className="flex items-center gap-2">
                {order.status === 'NEW' && (
                  <Button
                    variant="primary"
                    onClick={() => handleTransitionStatus('DIAGNOSING')}
                    pending={transitionStatus.isPending}
                  >
                    Bắt đầu chẩn đoán
                  </Button>
                )}
                {order.status === 'DIAGNOSING' && (
                  <Button
                    variant="primary"
                    onClick={() => setActiveTab('estimate')}
                  >
                    Lập báo giá dịch vụ
                  </Button>
                )}
                {order.status === 'APPROVED' && (
                  <Button
                    variant="primary"
                    onClick={() => handleTransitionStatus('IN_PROGRESS')}
                    pending={transitionStatus.isPending}
                  >
                    Bắt đầu sửa chữa
                  </Button>
                )}
                {order.status === 'IN_PROGRESS' && (
                  <Button
                    variant="primary"
                    onClick={() => handleTransitionStatus('WAITING_QC')}
                    pending={transitionStatus.isPending}
                  >
                    <ArrowRight size={15} /> Yêu cầu kiểm tra QC
                  </Button>
                )}
                {(order.status === 'WAITING_QC' || order.status === 'QC_FAILED') && (
                  <Button
                    variant="primary"
                    onClick={() => setPerformQCOpen(true)}
                  >
                    <ShieldCheck size={15} /> Kiểm tra chất lượng (QC)
                  </Button>
                )}
                {order.status === 'COMPLETED' && (
                  <Button
                    variant="primary"
                    onClick={() => setHandoverOpen(true)}
                  >
                    <UserCheck size={15} /> Bàn giao xe
                  </Button>
                )}
              </div>
            </div>

            {/* Standard Tabs Navigation */}
            <div className="tabs" role="group" aria-label="Nội dung lệnh sửa chữa">
              <button
                type="button"
                aria-pressed={activeTab === 'overview'}
                onClick={() => setActiveTab('overview')}
              >
                Chẩn đoán & Phương tiện
              </button>
              <button
                type="button"
                aria-pressed={activeTab === 'estimate'}
                onClick={() => setActiveTab('estimate')}
              >
                Báo giá ({estimates.length})
              </button>
              <button
                type="button"
                aria-pressed={activeTab === 'tasks'}
                onClick={() => setActiveTab('tasks')}
              >
                Công việc ({tasks.length})
              </button>
              <button
                type="button"
                aria-pressed={activeTab === 'qc'}
                onClick={() => setActiveTab('qc')}
              >
                Kiểm định chất lượng ({qcChecks.length})
              </button>
              <button
                type="button"
                aria-pressed={activeTab === 'handover'}
                onClick={() => setActiveTab('handover')}
              >
                Bàn giao xe
              </button>
            </div>

            {/* TAB 1: OVERVIEW & DIAGNOSIS */}
            {activeTab === 'overview' && (
              <div className="space-y-4 pt-1">
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  {/* Left Column: Vehicle & Intake Info */}
                  <div className="panel p-4 space-y-3">
                    <div className="flex items-center gap-2 text-sm font-semibold border-b pb-2">
                      <Car size={16} className="text-primary" />
                      <span>Thông tin tiếp nhận xe</span>
                    </div>

                    <div className="space-y-2 text-sm">
                      <div className="flex items-center justify-between py-1 border-b">
                        <span className="text-muted-foreground flex items-center gap-1.5">
                          <Car size={14} /> Phương tiện
                        </span>
                        <strong>Xe #{order.vehicleId}</strong>
                      </div>
                      <div className="flex items-center justify-between py-1 border-b">
                        <span className="text-muted-foreground flex items-center gap-1.5">
                          <Gauge size={14} /> Số ODO khi nhận xe
                        </span>
                        <strong>{order.odometerIn.toLocaleString('vi-VN')} km</strong>
                      </div>
                      {order.fuelLevelPercent !== null && order.fuelLevelPercent !== undefined && (
                        <div className="flex items-center justify-between py-1 border-b">
                          <span className="text-muted-foreground flex items-center gap-1.5">
                            <Fuel size={14} /> Mức nhiên liệu
                          </span>
                          <span>{order.fuelLevelPercent}%</span>
                        </div>
                      )}
                      <div className="flex items-center justify-between py-1 border-b">
                        <span className="text-muted-foreground flex items-center gap-1.5">
                          <Calendar size={14} /> Thời gian nhận xe
                        </span>
                        <span className="muted">{formatDateTime(order.receivedAt)}</span>
                      </div>
                      {order.customerComplaint && (
                        <div className="p-3 bg-muted/20 border rounded-md text-xs space-y-1 mt-2">
                          <div className="font-semibold text-foreground flex items-center gap-1.5">
                            <AlertTriangle size={14} className="text-warning" />
                            <span>Triệu chứng / Yêu cầu của khách hàng:</span>
                          </div>
                          <p className="italic text-muted-foreground pl-5">
                            &ldquo;{order.customerComplaint}&rdquo;
                          </p>
                        </div>
                      )}
                    </div>
                  </div>

                  {/* Right Column: Technical Diagnosis */}
                  <div className="panel p-4 space-y-3 flex flex-col justify-between">
                    <div className="space-y-2">
                      <div className="flex items-center gap-2 text-sm font-semibold border-b pb-2">
                        <Wrench size={16} className="text-primary" />
                        <span>Kết luận chẩn đoán kỹ thuật</span>
                      </div>
                      <textarea
                        className="input w-full"
                        rows={7}
                        placeholder="Ghi nhận hiện tượng hư hỏng, nguyên nhân kỹ thuật và phương án sửa chữa đề xuất..."
                        value={currentDiagnosis}
                        onChange={(e) => {
                          setDiagnosisText(e.target.value);
                          setHasEditedDiagnosis(true);
                        }}
                      />
                    </div>
                    <div className="flex justify-end pt-2 border-t">
                      <Button
                        variant="primary"
                        onClick={handleSaveDiagnosis}
                        pending={updateDiagnosis.isPending}
                      >
                        Lưu chẩn đoán kỹ thuật
                      </Button>
                    </div>
                  </div>
                </div>
              </div>
            )}

            {/* TAB 2: ESTIMATES & LINE APPROVAL */}
            {activeTab === 'estimate' && (
              <div className="space-y-4 pt-1">
                <div className="flex items-center justify-between">
                  <span className="text-sm font-semibold">Danh sách bản báo giá ({estimates.length})</span>
                  {estimates.length > 0 && (
                    <Button
                      variant="primary"
                      onClick={() => setCreateEstimateOpen(true)}
                    >
                      <Plus size={15} /> Lập báo giá mới
                    </Button>
                  )}
                </div>

                {isLoadingEstimates ? (
                  <Loading label="Đang tải danh sách báo giá…" />
                ) : estimates.length === 0 ? (
                  <Empty
                    title="Chưa có bản báo giá nào cho phiếu này"
                    description="Lập báo giá dịch vụ và phụ tùng để gửi khách hàng duyệt trước khi thi công."
                    action={
                      <Button
                        variant="primary"
                        onClick={() => setCreateEstimateOpen(true)}
                      >
                        <Plus size={15} /> Lập bản báo giá đầu tiên
                      </Button>
                    }
                  />
                ) : (
                  <div className="space-y-4">
                    {estimates.map((est) => (
                      <div key={est.id} className="panel table-panel">
                        <div className="panel-heading">
                          <div className="flex items-center gap-2">
                            <span className="code-cell font-bold text-sm">#{est.estimateCode}</span>
                            <span className="badge badge-info text-xs">Bản #{est.versionNo}</span>
                            <span
                              className={`badge text-xs ${
                                est.status === 'APPROVED'
                                  ? 'badge-success'
                                  : est.status === 'SENT'
                                    ? 'badge-info'
                                    : 'badge-neutral'
                              }`}
                            >
                              {est.status === 'APPROVED' ? 'Đã duyệt' : est.status === 'SENT' ? 'Đã gửi' : 'Bản nháp'}
                            </span>
                          </div>
                          <div className="flex items-center gap-3">
                            <span className="text-sm font-semibold text-muted-foreground">
                              Tổng cộng: <strong className="money text-base text-primary">{money(est.totalAmount)}</strong>
                            </span>
                            {est.status === 'DRAFT' && (
                              <Button
                                variant="secondary"
                                onClick={() => handleSendEstimate(est.id)}
                                pending={sendEstimate.isPending}
                              >
                                <Send size={13} /> Gửi khách duyệt
                              </Button>
                            )}
                          </div>
                        </div>

                        {/* Items Data Table */}
                        <div className="table-scroll">
                          <table className="data-table">
                            <thead>
                              <tr>
                                <th style={{ width: '35%' }}>Hạng mục dịch vụ / phụ tùng</th>
                                <th style={{ width: '13%' }}>Phân loại</th>
                                <th style={{ width: '8%' }}>ĐVT</th>
                                <th style={{ width: '10%' }} className="text-right">Số lượng</th>
                                <th style={{ width: '14%' }} className="text-right">Đơn giá</th>
                                <th style={{ width: '14%' }} className="text-right">Thành tiền</th>
                                <th style={{ width: '12%' }} className="text-center">Trạng thái</th>
                                <th style={{ width: '10%' }} className="text-right">Thao tác</th>
                              </tr>
                            </thead>
                            <tbody>
                              {est.items.map((item) => (
                                <tr key={item.id}>
                                  <td className="font-medium text-foreground">{item.itemName}</td>
                                  <td>
                                    <span
                                      className={`badge text-xs ${
                                        item.itemType === 'SERVICE' ? 'badge-info' : 'badge-neutral'
                                      }`}
                                    >
                                      {item.itemType === 'SERVICE' ? 'Dịch vụ' : 'Phụ tùng'}
                                    </span>
                                  </td>
                                  <td>{item.unit}</td>
                                  <td className="text-right font-medium">{item.quantity}</td>
                                  <td className="text-right money">{money(item.unitPrice)}</td>
                                  <td className="text-right font-bold money text-foreground">
                                    {money(item.lineTotal)}
                                  </td>
                                  <td className="text-center">
                                    <span
                                      className={`badge text-xs ${
                                        item.approvalStatus === 'APPROVED'
                                          ? 'badge-success'
                                          : item.approvalStatus === 'REJECTED'
                                            ? 'badge-danger'
                                            : 'badge-warning'
                                      }`}
                                    >
                                      {item.approvalStatus === 'APPROVED'
                                        ? '✓ Đã duyệt'
                                        : item.approvalStatus === 'REJECTED'
                                          ? '✗ Từ chối'
                                          : 'Chờ duyệt'}
                                    </span>
                                  </td>
                                  <td className="text-right">
                                    {est.status !== 'APPROVED' && item.approvalStatus === 'PENDING' && (
                                      <div className="flex justify-end gap-1">
                                        <button
                                          type="button"
                                          className="p-1 rounded bg-success/20 text-success hover:bg-success/30"
                                          title="Duyệt hạng mục này"
                                          onClick={() =>
                                            handleDecideLine(est.id, item.id, 'APPROVED')
                                          }
                                        >
                                          <Check size={14} />
                                        </button>
                                        <button
                                          type="button"
                                          className="p-1 rounded bg-danger/20 text-danger hover:bg-danger/30"
                                          title="Từ chối hạng mục này"
                                          onClick={() =>
                                            handleDecideLine(est.id, item.id, 'REJECTED')
                                          }
                                        >
                                          <X size={14} />
                                        </button>
                                      </div>
                                    )}
                                  </td>
                                </tr>
                              ))}
                            </tbody>
                          </table>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            )}

            {/* TAB 3: TASKS */}
            {activeTab === 'tasks' && (
              <div className="space-y-4 pt-1">
                <div className="flex items-center justify-between">
                  <span className="text-sm font-semibold">
                    Danh sách công việc sửa chữa ({tasks.length})
                  </span>
                  {tasks.length > 0 && (
                    <Button
                      variant="primary"
                      onClick={() => setCreateTaskOpen(true)}
                    >
                      <Plus size={15} /> Thêm công việc
                    </Button>
                  )}
                </div>

                {isLoadingTasks ? (
                  <Loading label="Đang tải danh sách công việc…" />
                ) : tasks.length === 0 ? (
                  <Empty
                    title="Chưa có công việc nào trong phiếu sửa chữa"
                    description="Công việc được tạo tự động khi khách duyệt báo giá hoặc bạn có thể bổ sung thủ công."
                    action={
                      <Button
                        variant="primary"
                        onClick={() => setCreateTaskOpen(true)}
                      >
                        <Plus size={15} /> Thêm công việc đầu tiên
                      </Button>
                    }
                  />
                ) : (
                  <div className="panel table-panel">
                    <div className="table-scroll">
                      <table className="data-table">
                        <thead>
                          <tr>
                            <th style={{ width: '45%' }}>Tên công việc & Yêu cầu kỹ thuật</th>
                            <th style={{ width: '15%' }}>Phân loại</th>
                            <th style={{ width: '10%' }} className="text-right">Số lượng</th>
                            <th style={{ width: '15%' }} className="text-center">Trạng thái</th>
                            <th style={{ width: '15%' }} className="text-right">Thao tác</th>
                          </tr>
                        </thead>
                        <tbody>
                          {tasks.map((task) => (
                            <tr key={task.id}>
                              <td>
                                <div>
                                  <strong className="text-foreground">{task.taskName}</strong>
                                  {task.description && (
                                    <p className="text-xs text-muted-foreground mt-0.5">{task.description}</p>
                                  )}
                                </div>
                              </td>
                              <td>
                                <span className="badge badge-neutral text-xs">
                                  {task.taskType === 'REPAIR'
                                    ? 'Sửa chữa'
                                    : task.taskType === 'DIAGNOSIS'
                                      ? 'Chẩn đoán'
                                      : 'Bảo dưỡng'}
                                </span>
                              </td>
                              <td className="text-right font-medium">{task.quantity || 1}</td>
                              <td className="text-center">
                                <span
                                  className={`badge text-xs ${
                                    task.status === 'COMPLETED'
                                      ? 'badge-success'
                                      : task.status === 'IN_PROGRESS'
                                        ? 'badge-primary'
                                        : 'badge-warning'
                                  }`}
                                >
                                  {task.status === 'COMPLETED'
                                    ? '✓ Đã hoàn thành'
                                    : task.status === 'IN_PROGRESS'
                                      ? 'Đang thực hiện'
                                      : 'Chờ thực hiện'}
                                </span>
                              </td>
                              <td className="text-right">
                                {task.status !== 'COMPLETED' && (
                                  <Button
                                    variant="secondary"
                                    onClick={() => handleCompleteTask(task.id)}
                                    pending={completeTask.isPending}
                                  >
                                    <CheckCircle size={14} /> Hoàn thành
                                  </Button>
                                )}
                              </td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  </div>
                )}
              </div>
            )}

            {/* TAB 4: QUALITY CHECK (QC) */}
            {activeTab === 'qc' && (
              <div className="space-y-4 pt-1">
                <div className="flex items-center justify-between">
                  <span className="text-sm font-semibold">
                    Biên bản kiểm tra chất lượng xuất xưởng ({qcChecks.length})
                  </span>
                  {qcChecks.length > 0 && (
                    <Button
                      variant="primary"
                      onClick={() => setPerformQCOpen(true)}
                    >
                      <ShieldCheck size={15} /> Bắt đầu kiểm tra QC
                    </Button>
                  )}
                </div>

                {isLoadingQC ? (
                  <Loading label="Đang tải lịch sử kiểm tra chất lượng…" />
                ) : qcChecks.length === 0 ? (
                  <Empty
                    title="Chưa có biên bản kiểm tra chất lượng"
                    description="Kỹ thuật viên QC sẽ tiến hành kiểm tra xe sau khi hoàn tất các hạng mục sửa chữa."
                    action={
                      <Button
                        variant="primary"
                        onClick={() => setPerformQCOpen(true)}
                      >
                        <ShieldCheck size={15} /> Bắt đầu kiểm tra QC
                      </Button>
                    }
                  />
                ) : (
                  <div className="space-y-3">
                    {qcChecks.map((qc) => (
                      <div
                        key={qc.id}
                        className={`panel p-4 text-sm space-y-2.5 ${
                          qc.result === 'PASSED'
                            ? 'border-success/30 bg-success/5'
                            : 'border-danger/30 bg-danger/5'
                        }`}
                      >
                        <div className="flex items-center justify-between border-b pb-2">
                          <div className="flex items-center gap-2">
                            <span className="font-bold">Lần kiểm tra #{qc.attemptNo}</span>
                            <span
                              className={`badge text-xs ${
                                qc.result === 'PASSED' ? 'badge-success' : 'badge-danger'
                              }`}
                            >
                              {qc.result === 'PASSED' ? '✓ Đạt tiêu chuẩn xuất xưởng' : '✗ Cần làm lại (Rework)'}
                            </span>
                          </div>
                          <span className="text-xs text-muted-foreground">
                            {formatDateTime(qc.checkedAt)}
                          </span>
                        </div>
                        {qc.odometer && (
                          <div className="text-xs">
                            Số ODO khi chạy thử: <strong>{qc.odometer.toLocaleString('vi-VN')} km</strong>
                          </div>
                        )}
                        {qc.notes && (
                          <p className="text-xs text-muted-foreground italic bg-background/50 p-2 rounded">
                            Nhận xét kiểm định: {qc.notes}
                          </p>
                        )}
                      </div>
                    ))}
                  </div>
                )}
              </div>
            )}

            {/* TAB 5: HANDOVER */}
            {activeTab === 'handover' && (
              <div className="space-y-4 pt-1">
                <div className="panel p-5 space-y-4">
                  <div className="flex items-center gap-2 font-semibold text-sm border-b pb-3">
                    <UserCheck size={18} className="text-primary" />
                    <span>Quy trình bàn giao xe cho khách hàng</span>
                  </div>

                  <div className="grid grid-cols-1 md:grid-cols-3 gap-4 text-sm">
                    <div className="p-3 bg-muted/15 border rounded-lg space-y-1">
                      <div className="flex items-center gap-2 font-semibold text-primary">
                        <span className="w-5 h-5 rounded-full bg-primary/20 flex items-center justify-center text-xs font-bold">1</span>
                        <span>Hoàn tất thi công & QC</span>
                      </div>
                      <p className="text-xs text-muted-foreground">
                        Các hạng mục công việc đã hoàn thành và có biên bản kiểm tra QC đạt chuẩn.
                      </p>
                    </div>

                    <div className="p-3 bg-muted/15 border rounded-lg space-y-1">
                      <div className="flex items-center gap-2 font-semibold text-primary">
                        <span className="w-5 h-5 rounded-full bg-primary/20 flex items-center justify-center text-xs font-bold">2</span>
                        <span>Quyết toán chi phí</span>
                      </div>
                      <p className="text-xs text-muted-foreground">
                        Khách hàng thanh toán hóa đơn dịch vụ hoặc xác nhận ghi sổ công nợ hợp lệ.
                      </p>
                    </div>

                    <div className="p-3 bg-muted/15 border rounded-lg space-y-1">
                      <div className="flex items-center gap-2 font-semibold text-primary">
                        <span className="w-5 h-5 rounded-full bg-primary/20 flex items-center justify-center text-xs font-bold">3</span>
                        <span>Bàn giao chìa khóa</span>
                      </div>
                      <p className="text-xs text-muted-foreground">
                        Ghi nhận số ODO xuất xưởng, bàn giao phương tiện và chính thức đóng phiếu.
                      </p>
                    </div>
                  </div>

                  <div className="pt-3 border-t flex items-center justify-between">
                    <div>
                      {order.status === 'CLOSED' ? (
                        <div className="text-success font-semibold text-sm flex items-center gap-1.5">
                          <CheckCircle size={16} /> Xe đã được bàn giao thành công lúc{' '}
                          {order.handedOverAt ? formatDateTime(order.handedOverAt) : 'N/A'}
                        </div>
                      ) : (
                        <span className="text-xs text-muted-foreground">
                          Trạng thái phiếu hiện tại: <strong>{REPAIR_STATUS_LABELS[order.status] || order.status}</strong>
                        </span>
                      )}
                    </div>

                    {order.status === 'COMPLETED' ? (
                      <Button variant="primary" onClick={() => setHandoverOpen(true)}>
                        <UserCheck size={15} /> Tiến hành bàn giao xe
                      </Button>
                    ) : order.status !== 'CLOSED' ? (
                      <span className="text-xs text-muted-foreground italic">
                        Cần hoàn thành sửa chữa và kiểm định QC đạt chuẩn trước khi bàn giao xe.
                      </span>
                    ) : null}
                  </div>
                </div>
              </div>
            )}
          </>
        )}
      </div>

      {/* Sub-modals */}
      <CreateTaskModal
        orderId={order?.id || ''}
        open={createTaskOpen}
        onClose={() => setCreateTaskOpen(false)}
      />
      <CreateEstimateModal
        orderId={order?.id || ''}
        open={createEstimateOpen}
        onClose={() => setCreateEstimateOpen(false)}
      />
      <PerformQCModal
        orderId={order?.id || ''}
        open={performQCOpen}
        onClose={() => setPerformQCOpen(false)}
      />
      {order && (
        <HandoverModal
          order={order}
          open={handoverOpen}
          onClose={() => setHandoverOpen(false)}
        />
      )}
    </Sheet>
  );
}
