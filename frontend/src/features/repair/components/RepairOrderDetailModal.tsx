import { Fragment, useMemo, useState } from 'react';
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
  ClipboardList,
  Lock,
  RotateCcw,
} from 'lucide-react';
import { useToast } from '../../../app/providers/ToastProvider';
import { useAuth } from '../../../app/providers/AuthProvider';
import { FRONT_DESK_ROLES, QC_ROLES, TECHNICAL_ROLES } from '../../../app/access';
import { useVehicleQuery } from '../../vehicle/hooks/useVehicles';
import {
  useOnShiftTechniciansQuery,
  useSchedulingEmployeesQuery,
} from '../../scheduling/hooks/useScheduling';
import { errorMessage } from '../../../shared/api/errors';
import {
  Badge,
  Button,
  ConfirmDialog,
  Empty,
  Loading,
  QueryError,
  Sheet,
} from '../../../shared/components/ui';
import { formatDateTime, money } from '../../../shared/utils/format';
import type { RepairOrderStatus, RepairTask } from '../types/repairTypes';
import {
  useRepairOrderQuery,
  useRepairTasksQuery,
  useUpdateDiagnosisMutation,
  useUpdateRepairOrderMutation,
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
import { AssignTaskModal } from './AssignTaskModal';
import { TaskWorkPanel } from './TaskWorkPanel';
import { CreateEstimateModal } from './CreateEstimateModal';
import {
  allowedAttachmentCategories,
  canDecideOn,
  canReopenDiagnosis,
  diagnosisLockReason,
  newEstimateMode,
} from '../utils/stageLocks';
import { estimateStatusLabels } from '../../estimate/types/estimate';
import { PerformQCModal } from './PerformQCModal';
import { HandoverModal } from './HandoverModal';
import { VehicleLabel } from './VehicleLabel';
import { DiagnosisAttachmentGallery, type CheckInSummary } from './DiagnosisAttachmentGallery';
import { useVehicleChecksQuery } from '../../reception/hooks/useReception';
import { VehicleCheckModal } from '../../reception/components/VehicleCheckModal';

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
  const { user } = useAuth();
  const isCustomer = user?.role === 'CUSTOMER';
  const canTechnical = !!user && TECHNICAL_ROLES.includes(user.role);
  const canPerformQc = !!user && QC_ROLES.includes(user.role);
  const canFrontDesk = !!user && FRONT_DESK_ROLES.includes(user.role);
  const canDecideEstimate = isCustomer || canFrontDesk;
  const [activeTab, setActiveTab] = useState<Tab>('overview');
  const updateOrder = useUpdateRepairOrderMutation();
  const [assigningTask, setAssigningTask] = useState<RepairTask | null>(null);
  const { data: staff = [] } = useSchedulingEmployeesQuery({ active: true }, canTechnical);
  const onShift = useOnShiftTechniciansQuery(open && canTechnical);
  const ownEmployeeId = staff.find((e) => String(e.userId) === String(user?.id))?.id;
  const staffName = (employee: { fullName?: string | null; employeeCode: string }) =>
    employee.fullName ?? employee.employeeCode;

  // Sub-modals
  const [createTaskOpen, setCreateTaskOpen] = useState(false);
  const [createEstimateOpen, setCreateEstimateOpen] = useState(false);
  const [reopenConfirmOpen, setReopenConfirmOpen] = useState(false);
  const [performQCOpen, setPerformQCOpen] = useState(false);
  const [handoverOpen, setHandoverOpen] = useState(false);

  // Queries
  const {
    data: order,
    isLoading,
    isError,
    error,
    refetch,
  } = useRepairOrderQuery(open ? orderId : null);
  const diagnosticTechnician = staff.find(
    (e) => String(e.id) === String(order?.diagnosticTechnicianId),
  );
  const { data: tasks = [], isLoading: isLoadingTasks } = useRepairTasksQuery(
    open ? orderId : null,
  );
  const { data: allEstimates = [], isLoading: isLoadingEstimates } = useEstimatesQuery(
    open ? orderId : null,
  );
  const { data: vehicle } = useVehicleQuery(open && order ? order.vehicleId : null);
  const estimates = isCustomer
    ? allEstimates.filter((est) => est.status !== 'DRAFT')
    : allEstimates;
  const { data: qcChecks = [], isLoading: isLoadingQC } = useQualityChecksQuery(
    open ? orderId : null,
  );
  const { data: vehicleChecks = [] } = useVehicleChecksQuery(open ? orderId : null);
  const [vehicleCheckOpen, setVehicleCheckOpen] = useState(false);

  // Biên bản tiếp nhận mới nhất; checklistJson do VehicleCheckModal ghi dạng {exterior, interior, warningLights}.
  const checkInSummary = useMemo<CheckInSummary | null>(() => {
    const latest = vehicleChecks
      .filter((c) => c.checkType === 'CHECK_IN')
      .sort((a, b) => b.checkedAt.localeCompare(a.checkedAt))[0];
    if (!latest) return null;
    let parsed: Record<string, unknown> = {};
    try {
      const value: unknown = JSON.parse(latest.checklistJson);
      if (value && typeof value === 'object') parsed = value as Record<string, unknown>;
    } catch {
      /* checklist không phải JSON: chỉ hiển thị ghi chú */
    }
    const text = (key: string) => (typeof parsed[key] === 'string' ? (parsed[key] as string) : '');
    return {
      exterior: text('exterior'),
      interior: text('interior'),
      warningLights: text('warningLights'),
      notes: latest.notes,
    };
  }, [vehicleChecks]);

  // Mutations
  const updateDiagnosis = useUpdateDiagnosisMutation();
  const transitionStatus = useTransitionStatusMutation();
  const sendEstimate = useSendEstimateMutation();
  const decideEstimate = useDecideEstimateMutation();
  const completeTask = useCompleteTaskMutation();

  const [diagnosisText, setDiagnosisText] = useState('');
  const [hasEditedDiagnosis, setHasEditedDiagnosis] = useState(false);

  const currentDiagnosis = hasEditedDiagnosis ? diagnosisText : order?.diagnosis || '';
  const diagnosisLock = order ? diagnosisLockReason(order.status) : null;
  const canEditDiagnosis = canTechnical && !diagnosisLock;
  const reopenable = !!order && canTechnical && canReopenDiagnosis(order.status, allEstimates);
  const estimateMode = order && canTechnical ? newEstimateMode(order.status, allEstimates) : null;
  const uploadCategories = order
    ? allowedAttachmentCategories(order.status, ['DIAGNOSIS', 'CHECK_IN'])
    : [];

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
      notify(
        decision === 'APPROVED' ? 'Đã duyệt hạng mục báo giá.' : 'Đã từ chối hạng mục báo giá.',
      );
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
          ? `Tiếp nhận lúc ${formatDateTime(order.receivedAt)} · ${vehicle ? `${vehicle.licensePlate} · ${vehicle.brand} ${vehicle.model} · ${vehicle.customerName}` : 'Đang tải thông tin xe…'}`
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
            <div className="p-3.5 ro-summary-banner border rounded-lg flex flex-wrap items-center justify-between gap-3">
              <div className="flex flex-wrap items-center gap-3">
                <div className="flex items-center gap-2">
                  <span className="text-xs font-medium muted">Trạng thái:</span>
                  <RepairOrderStatusBadge status={order.status} />
                </div>
                <div className="h-4 w-px bg-border hidden sm:block" />
                <div className="text-xs muted flex items-center gap-2">
                  <span>
                    Phương tiện:{' '}
                    <span>
                      <VehicleLabel vehicleId={order.vehicleId} />
                    </span>
                  </span>
                  <span>•</span>
                  <span>
                    ODO vào: <strong>{order.odometerIn.toLocaleString('vi-VN')} km</strong>
                  </span>
                  <span>•</span>
                  <span>
                    Tiếp nhận: <strong>{formatDateTime(order.receivedAt)}</strong>
                  </span>
                </div>
              </div>

              <div className="flex items-center gap-2">
                {canTechnical && order.status === 'NEW' && (
                  <Button
                    variant="primary"
                    onClick={() => handleTransitionStatus('DIAGNOSING')}
                    pending={transitionStatus.isPending}
                  >
                    Bắt đầu chẩn đoán
                  </Button>
                )}
                {canTechnical && order.status === 'DIAGNOSING' && (
                  <Button variant="primary" onClick={() => setActiveTab('estimate')}>
                    Lập báo giá dịch vụ
                  </Button>
                )}
                {reopenable && (
                  <Button variant="ghost" onClick={() => setReopenConfirmOpen(true)}>
                    <RotateCcw size={15} /> Mở lại chẩn đoán
                  </Button>
                )}
                {canTechnical && order.status === 'APPROVED' && (
                  <Button
                    variant="primary"
                    onClick={() => handleTransitionStatus('IN_PROGRESS')}
                    pending={transitionStatus.isPending}
                  >
                    Bắt đầu sửa chữa
                  </Button>
                )}
                {canTechnical && order.status === 'IN_PROGRESS' && (
                  <Button
                    variant="primary"
                    onClick={() => handleTransitionStatus('WAITING_QC')}
                    pending={transitionStatus.isPending}
                  >
                    <ArrowRight size={15} /> Yêu cầu kiểm tra QC
                  </Button>
                )}
                {canPerformQc && order.status === 'WAITING_QC' && (
                  <Button variant="primary" onClick={() => setPerformQCOpen(true)}>
                    <ShieldCheck size={15} /> Kiểm tra chất lượng (QC)
                  </Button>
                )}
                {canFrontDesk && order.status === 'COMPLETED' && (
                  <Button variant="primary" onClick={() => setHandoverOpen(true)}>
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
                      {canTechnical && (
                        <div className="flex items-center justify-between py-1 border-b">
                          <span className="muted flex items-center gap-1.5">
                            <UserCheck size={14} /> Thợ chẩn đoán tiếp nhận
                          </span>
                          <strong>
                            {diagnosticTechnician
                              ? staffName(diagnosticTechnician)
                              : 'Chưa chỉ định'}
                          </strong>
                        </div>
                      )}
                      <div className="flex items-center justify-between py-1 border-b">
                        <span className="muted flex items-center gap-1.5">
                          <Car size={14} /> Phương tiện
                        </span>
                        <VehicleLabel vehicleId={order.vehicleId} />
                      </div>
                      <div className="flex items-center justify-between py-1 border-b">
                        <span className="muted flex items-center gap-1.5">
                          <Gauge size={14} /> Số ODO khi nhận xe
                        </span>
                        <strong>{order.odometerIn.toLocaleString('vi-VN')} km</strong>
                      </div>
                      {order.fuelLevelPercent !== null && order.fuelLevelPercent !== undefined && (
                        <div className="flex items-center justify-between py-1 border-b">
                          <span className="muted flex items-center gap-1.5">
                            <Fuel size={14} /> Mức nhiên liệu
                          </span>
                          <span>{order.fuelLevelPercent}%</span>
                        </div>
                      )}
                      <div className="flex items-center justify-between py-1 border-b">
                        <span className="muted flex items-center gap-1.5">
                          <Calendar size={14} /> Thời gian nhận xe
                        </span>
                        <span className="muted">{formatDateTime(order.receivedAt)}</span>
                      </div>
                      {order.customerComplaint && (
                        <div className="p-3 ro-BayBanner border rounded-md text-xs space-y-1 mt-2">
                          <div className="font-semibold flex items-center gap-1.5">
                            <AlertTriangle size={14} className="status-text-warning" />
                            <span>Triệu chứng / Yêu cầu của khách hàng:</span>
                          </div>
                          <p className="italic muted pl-5">
                            &ldquo;{order.customerComplaint}&rdquo;
                          </p>
                        </div>
                      )}

                      <div className="p-3 border rounded-md text-xs space-y-1 mt-2">
                        <div className="font-semibold flex items-center justify-between gap-2">
                          <span className="flex items-center gap-1.5">
                            <ClipboardList size={14} /> Biên bản kiểm tra tiếp nhận
                          </span>
                          {(canTechnical || canFrontDesk) && order.status === 'NEW' && (
                            <Button variant="ghost" onClick={() => setVehicleCheckOpen(true)}>
                              <Plus size={14} /> Lập biên bản
                            </Button>
                          )}
                        </div>
                        {checkInSummary ? (
                          <dl className="checkin-summary">
                            <dt>Ngoại thất</dt>
                            <dd>{checkInSummary.exterior || '—'}</dd>
                            <dt>Nội thất</dt>
                            <dd>{checkInSummary.interior || '—'}</dd>
                            <dt>Đèn cảnh báo</dt>
                            <dd>{checkInSummary.warningLights || '—'}</dd>
                            {checkInSummary.notes && (
                              <>
                                <dt>Ghi chú</dt>
                                <dd>{checkInSummary.notes}</dd>
                              </>
                            )}
                          </dl>
                        ) : (
                          <p className="muted">Chưa lập biên bản kiểm tra khi nhận xe.</p>
                        )}
                      </div>
                    </div>
                  </div>

                  {/* Right Column: Technical Diagnosis */}
                  <div className="panel p-4 space-y-3 flex flex-col justify-between">
                    <div className="space-y-2">
                      <div className="flex items-center gap-2 text-sm font-semibold border-b pb-2">
                        <Wrench size={16} className="text-primary" />
                        <span>Kết luận chẩn đoán kỹ thuật</span>
                        {diagnosisLock && (
                          <Badge variant="secondary">
                            <Lock size={11} /> Đã chốt
                          </Badge>
                        )}
                      </div>
                      {diagnosisLock && <p className="info-strip">{diagnosisLock}</p>}
                      <textarea
                        className="input w-full"
                        rows={7}
                        readOnly={!canEditDiagnosis}
                        placeholder="Ghi nhận hiện tượng hư hỏng, nguyên nhân kỹ thuật và phương án sửa chữa đề xuất..."
                        value={currentDiagnosis}
                        onChange={(e) => {
                          setDiagnosisText(e.target.value);
                          setHasEditedDiagnosis(true);
                        }}
                      />
                    </div>
                    {canEditDiagnosis && (
                      <div className="flex justify-end pt-2 border-t">
                        <Button
                          variant="primary"
                          onClick={handleSaveDiagnosis}
                          pending={updateDiagnosis.isPending}
                        >
                          Lưu chẩn đoán kỹ thuật
                        </Button>
                      </div>
                    )}
                  </div>
                </div>

                <DiagnosisAttachmentGallery
                  repairOrderId={order.id}
                  customerComplaint={order.customerComplaint}
                  diagnosis={currentDiagnosis}
                  checkIn={checkInSummary}
                  canUpload={canTechnical || canFrontDesk}
                  uploadCategories={uploadCategories}
                />
              </div>
            )}

            {/* TAB 2: ESTIMATES & LINE APPROVAL */}
            {activeTab === 'estimate' && (
              <div className="space-y-4 pt-1">
                <div className="flex items-center justify-between">
                  <span className="text-sm font-semibold">
                    Danh sách bản báo giá ({estimates.length})
                  </span>
                  {estimateMode && estimates.length > 0 && (
                    <Button variant="primary" onClick={() => setCreateEstimateOpen(true)}>
                      <Plus size={15} />{' '}
                      {estimateMode.kind === 'SUPPLEMENTAL'
                        ? 'Lập báo giá phát sinh'
                        : 'Lập phiên bản báo giá mới'}
                    </Button>
                  )}
                </div>

                {isLoadingEstimates ? (
                  <Loading label="Đang tải danh sách báo giá…" />
                ) : estimates.length === 0 ? (
                  <Empty
                    title="Chưa có bản báo giá nào cho phiếu này"
                    description={
                      isCustomer
                        ? 'Gara sẽ gửi báo giá cho bạn duyệt sau khi chẩn đoán xong.'
                        : 'Lập báo giá dịch vụ và phụ tùng để gửi khách hàng duyệt trước khi thi công.'
                    }
                    action={
                      estimateMode ? (
                        <Button variant="primary" onClick={() => setCreateEstimateOpen(true)}>
                          <Plus size={15} /> Lập bản báo giá đầu tiên
                        </Button>
                      ) : undefined
                    }
                  />
                ) : (
                  <div className="space-y-4">
                    {estimates.map((est) => (
                      <div key={est.id} className="panel table-panel">
                        <div className="panel-heading">
                          <div className="flex items-center gap-2">
                            <span className="code-cell font-bold text-sm">#{est.estimateCode}</span>
                            <span className="badge badge-primary text-xs">
                              Bản #{est.versionNo}
                            </span>
                            <span
                              className={`badge text-xs ${
                                est.status === 'APPROVED'
                                  ? 'badge-success'
                                  : est.status === 'SENT'
                                    ? 'badge-primary'
                                    : 'badge-neutral'
                              }`}
                            >
                              {estimateStatusLabels[est.status] ?? est.status}
                            </span>
                          </div>
                          <div className="flex items-center gap-3">
                            <span className="text-sm font-semibold muted">
                              Tổng cộng:{' '}
                              <strong className="money text-base text-primary">
                                {money(est.totalAmount)}
                              </strong>
                            </span>
                            {canTechnical && est.status === 'DRAFT' && (
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
                                <th style={{ width: '10%' }} className="text-right">
                                  Số lượng
                                </th>
                                <th style={{ width: '14%' }} className="text-right">
                                  Đơn giá
                                </th>
                                <th style={{ width: '14%' }} className="text-right">
                                  Thành tiền
                                </th>
                                <th style={{ width: '12%' }} className="text-center">
                                  Trạng thái
                                </th>
                                <th style={{ width: '10%' }} className="text-right">
                                  Thao tác
                                </th>
                              </tr>
                            </thead>
                            <tbody>
                              {est.items.map((item) => (
                                <tr key={item.id}>
                                  <td className="font-medium">{item.itemName}</td>
                                  <td>
                                    <span
                                      className={`badge text-xs ${
                                        item.itemType === 'SERVICE'
                                          ? 'badge-primary'
                                          : 'badge-neutral'
                                      }`}
                                    >
                                      {item.itemType === 'SERVICE' ? 'Dịch vụ' : 'Phụ tùng'}
                                    </span>
                                  </td>
                                  <td>{item.unit}</td>
                                  <td className="text-right font-medium">{item.quantity}</td>
                                  <td className="text-right money">{money(item.unitPrice)}</td>
                                  <td className="text-right font-bold money">
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
                                    {canDecideEstimate &&
                                      canDecideOn(est) &&
                                      item.approvalStatus === 'PENDING' && (
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
                {canTechnical && (
                  <div className="panel" aria-label="Kỹ thuật viên trưởng">
                    <div className="flex items-center justify-between gap-3">
                      <div>
                        <span className="text-sm font-semibold">Kỹ thuật viên trưởng</span>
                        <p className="text-xs muted mt-0.5">
                          Chịu trách nhiệm điều phối lệnh và nhận thưởng hoàn thành khi QC đạt ngay
                          lần đầu.
                        </p>
                      </div>
                      {canFrontDesk ? (
                        <select
                          className="select"
                          aria-label="Chọn kỹ thuật viên trưởng"
                          value={order.leadTechnicianId ?? ''}
                          disabled={updateOrder.isPending}
                          onChange={(e) => {
                            if (!e.target.value) return;
                            updateOrder.mutate(
                              { id: order.id, data: { leadTechnicianId: Number(e.target.value) } },
                              {
                                onSuccess: () => notify('Đã chỉ định kỹ thuật viên trưởng'),
                                onError: (err: unknown) => notify(errorMessage(err)),
                              },
                            );
                          }}
                        >
                          <option value="">-- Chưa chỉ định --</option>
                          {staff.map((employee) => (
                            <option key={employee.id} value={employee.id}>
                              {staffName(employee)} · {employee.employeeCode}
                            </option>
                          ))}
                        </select>
                      ) : (
                        <strong>
                          {(() => {
                            const lead = staff.find(
                              (e) => String(e.id) === String(order.leadTechnicianId),
                            );
                            return lead ? staffName(lead) : 'Chưa chỉ định';
                          })()}
                        </strong>
                      )}
                    </div>
                    {canTechnical &&
                      (() => {
                        const lead = staff.find(
                          (e) => String(e.id) === String(order.leadTechnicianId),
                        );
                        const labor = tasks
                          .filter((t) => t.taskType === 'REPAIR')
                          .reduce(
                            (sum, t) => sum + Number(t.quantity || 1) * Number(t.unitPrice || 0),
                            0,
                          );
                        const rate = Number(lead?.defaultCommissionRate ?? 0);
                        return lead ? (
                          <p className="text-xs muted mt-2">
                            Quỹ hoa hồng tiền công dự kiến: {money(String((labor * rate) / 100))} (
                            {rate}% của {money(String(labor))}), chưa gồm thưởng hoàn thành lệnh.
                          </p>
                        ) : null;
                      })()}
                  </div>
                )}
                <div className="flex items-center justify-between">
                  <span className="text-sm font-semibold">
                    Danh sách công việc sửa chữa ({tasks.length})
                  </span>
                  {canTechnical && tasks.length > 0 && (
                    <Button variant="primary" onClick={() => setCreateTaskOpen(true)}>
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
                      canTechnical ? (
                        <Button variant="primary" onClick={() => setCreateTaskOpen(true)}>
                          <Plus size={15} /> Thêm công việc đầu tiên
                        </Button>
                      ) : undefined
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
                            <th style={{ width: '10%' }} className="text-right">
                              Số lượng
                            </th>
                            <th style={{ width: '15%' }} className="text-center">
                              Trạng thái
                            </th>
                            <th style={{ width: '15%' }} className="text-right">
                              Thao tác
                            </th>
                          </tr>
                        </thead>
                        <tbody>
                          {tasks.map((task) => (
                            <Fragment key={task.id}>
                              <tr>
                                <td>
                                  <div>
                                    <strong>{task.taskName}</strong>
                                    {task.description && (
                                      <p className="text-xs muted mt-0.5">{task.description}</p>
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
                                  {canTechnical && task.status !== 'COMPLETED' && (
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
                              {canTechnical && (
                                <tr>
                                  <td colSpan={5}>
                                    <TaskWorkPanel
                                      task={task}
                                      canAssign={canFrontDesk}
                                      canOperate={canTechnical}
                                      ownEmployeeId={ownEmployeeId}
                                      onAssign={setAssigningTask}
                                    />
                                  </td>
                                </tr>
                              )}
                            </Fragment>
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
                {canTechnical && (
                  <p className="text-xs muted" aria-label="Thợ QC đủ điều kiện">
                    Thợ QC đủ điều kiện đang trực ca:{' '}
                    {(onShift.data ?? [])
                      .filter((tech) => tech.canQc)
                      .map((tech) => tech.fullName)
                      .join(', ') || 'chưa có'}
                    . Thợ chính đã sửa chữa phiếu này không được tự kiểm định.
                  </p>
                )}
                <div className="flex items-center justify-between">
                  <span className="text-sm font-semibold">
                    Biên bản kiểm tra chất lượng xuất xưởng ({qcChecks.length})
                  </span>
                  {canTechnical && qcChecks.length > 0 && (
                    <Button variant="primary" onClick={() => setPerformQCOpen(true)}>
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
                      canTechnical ? (
                        <Button variant="primary" onClick={() => setPerformQCOpen(true)}>
                          <ShieldCheck size={15} /> Bắt đầu kiểm tra QC
                        </Button>
                      ) : undefined
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
                              {qc.result === 'PASSED'
                                ? '✓ Đạt tiêu chuẩn xuất xưởng'
                                : '✗ Cần làm lại (Rework)'}
                            </span>
                          </div>
                          <span className="text-xs muted">{formatDateTime(qc.checkedAt)}</span>
                        </div>
                        {qc.odometer && (
                          <div className="text-xs">
                            Số ODO khi chạy thử:{' '}
                            <strong>{qc.odometer.toLocaleString('vi-VN')} km</strong>
                          </div>
                        )}
                        {qc.notes && (
                          <p className="text-xs muted italic bg-background/50 p-2 rounded">
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
                    <div className="p-3 ro-summary-banner border rounded-lg space-y-1">
                      <div className="flex items-center gap-2 font-semibold text-primary">
                        <span className="w-5 h-5 rounded-full bg-primary/20 flex items-center justify-center text-xs font-bold">
                          1
                        </span>
                        <span>Hoàn tất thi công & QC</span>
                      </div>
                      <p className="text-xs muted">
                        Các hạng mục công việc đã hoàn thành và có biên bản kiểm tra QC đạt chuẩn.
                      </p>
                    </div>

                    <div className="p-3 ro-summary-banner border rounded-lg space-y-1">
                      <div className="flex items-center gap-2 font-semibold text-primary">
                        <span className="w-5 h-5 rounded-full bg-primary/20 flex items-center justify-center text-xs font-bold">
                          2
                        </span>
                        <span>Quyết toán chi phí</span>
                      </div>
                      <p className="text-xs muted">
                        Khách hàng thanh toán hóa đơn dịch vụ hoặc xác nhận ghi sổ công nợ hợp lệ.
                      </p>
                    </div>

                    <div className="p-3 ro-summary-banner border rounded-lg space-y-1">
                      <div className="flex items-center gap-2 font-semibold text-primary">
                        <span className="w-5 h-5 rounded-full bg-primary/20 flex items-center justify-center text-xs font-bold">
                          3
                        </span>
                        <span>Bàn giao chìa khóa</span>
                      </div>
                      <p className="text-xs muted">
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
                        <span className="text-xs muted">
                          Trạng thái phiếu hiện tại:{' '}
                          <strong>{REPAIR_STATUS_LABELS[order.status] || order.status}</strong>
                        </span>
                      )}
                    </div>

                    {order.status === 'COMPLETED' && canFrontDesk ? (
                      <Button variant="primary" onClick={() => setHandoverOpen(true)}>
                        <UserCheck size={15} /> Tiến hành bàn giao xe
                      </Button>
                    ) : order.status !== 'CLOSED' && canFrontDesk ? (
                      <span className="text-xs muted italic">
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
      <AssignTaskModal task={assigningTask} onClose={() => setAssigningTask(null)} />
      <CreateTaskModal
        orderId={order?.id || ''}
        open={createTaskOpen}
        onClose={() => setCreateTaskOpen(false)}
      />
      <CreateEstimateModal
        orderId={order?.id || ''}
        open={createEstimateOpen}
        onClose={() => setCreateEstimateOpen(false)}
        parentEstimateId={
          estimateMode?.kind === 'SUPPLEMENTAL' ? estimateMode.parentEstimateId : null
        }
      />
      <ConfirmDialog
        open={reopenConfirmOpen}
        title="Mở lại chẩn đoán?"
        description="Khách đã từ chối toàn bộ báo giá. Phiếu quay về bước chẩn đoán và các bản nháp báo giá còn lại sẽ bị hủy."
        confirmLabel="Mở lại chẩn đoán"
        onConfirm={() => {
          setReopenConfirmOpen(false);
          void handleTransitionStatus('DIAGNOSING');
        }}
        onCancel={() => setReopenConfirmOpen(false)}
      />
      <PerformQCModal
        orderId={order?.id || ''}
        open={performQCOpen}
        onClose={() => setPerformQCOpen(false)}
      />
      {order && (
        <HandoverModal order={order} open={handoverOpen} onClose={() => setHandoverOpen(false)} />
      )}
      {order && vehicleCheckOpen && (
        <VehicleCheckModal
          repairOrderId={order.id}
          orderCode={order.orderCode}
          open={vehicleCheckOpen}
          onClose={() => setVehicleCheckOpen(false)}
        />
      )}
    </Sheet>
  );
}
