import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useToast } from '../../../app/providers/ToastProvider';
import { applyFormError, errorMessage } from '../../../shared/api/errors';
import {
  Button,
  Field,
  FormError,
  Sheet,
} from '../../../shared/components/ui';
import type { Vehicle } from '../../../shared/types/contracts';
import { useCustomersQuery } from '../../customer/hooks/useCustomers';
import { useTransferOwnershipMutation } from '../hooks/useVehicles';
import {
  transferDefaults,
  transferOwnershipSchema,
  type TransferOwnershipFormValues,
} from '../schemas/vehicleSchema';
import { AlertTriangle } from 'lucide-react';

export function TransferOwnershipDialog({
  vehicle,
  open,
  onClose,
}: {
  vehicle: Vehicle | null;
  open: boolean;
  onClose: () => void;
}) {
  const notify = useToast();
  const transfer = useTransferOwnershipMutation();
  const { data: customers = [], isLoading: isLoadingCustomers } = useCustomersQuery({ isActive: true });

  const {
    register,
    handleSubmit,
    setError,
    reset,
    formState: { errors },
  } = useForm<TransferOwnershipFormValues>({
    resolver: zodResolver(transferOwnershipSchema),
    defaultValues: transferDefaults(vehicle ?? undefined),
  });

  if (!vehicle) return null;

  // Lọc danh sách khách hàng, loại bỏ chủ xe hiện tại
  const eligibleCustomers = customers.filter((c) => c.id !== vehicle.customerId);

  async function onSubmit(values: TransferOwnershipFormValues) {
    if (!vehicle) return;

    if (values.transferOdometer && values.transferOdometer < vehicle.odometer) {
      setError('transferOdometer', {
        message: `Số ODO bàn giao không được nhỏ hơn ODO hiện tại (${vehicle.odometer.toLocaleString('vi-VN')} km)`,
      });
      return;
    }

    try {
      await transfer.mutateAsync({
        id: vehicle.id,
        values: {
          newCustomerId: values.newCustomerId,
          transferOdometer: values.transferOdometer ? Number(values.transferOdometer) : vehicle.odometer,
          transferNote: values.transferNote?.trim() || null,
        },
      });
      notify.success(`Đã chuyển nhượng quyền sở hữu xe ${vehicle.licensePlate} thành công`);
      reset();
      onClose();
    } catch (err: unknown) {
      const field = applyFormError(err, setError);
      if (!field) {
        notify.error(errorMessage(err) ?? 'Chuyển nhượng phương tiện thất bại');
      }
    }
  }

  return (
    <Sheet
      open={open}
      onClose={onClose}
      placement="center"
      title={`Chuyển nhượng xe ${vehicle.licensePlate}`}
      description={`Chuyển giao quyền sở hữu xe ${vehicle.brand} ${vehicle.model} cho khách hàng mới.`}
    >
      <form onSubmit={handleSubmit(onSubmit)} noValidate className="editor-form account-editor-form">
        <FormError message={errors.root?.message} />

        <div className="p-3 bg-amber-50 dark:bg-amber-950/30 border border-amber-200 dark:border-amber-800 rounded-md text-xs text-amber-800 dark:text-amber-300 flex items-start gap-2">
          <AlertTriangle size={16} className="shrink-0 mt-0.5" />
          <div>
            <strong>Quy tắc bảo mật lịch sử (UC05/BR04):</strong> Lịch sử sửa chữa kỹ thuật trước đây vẫn được lưu trữ trên phương tiện, nhưng toàn bộ dữ liệu tài chính (chi phí dịch vụ) của chủ cũ sẽ tự động ẩn đối với chủ xe mới.
          </div>
        </div>

        <Field label="Chủ sở hữu hiện tại">
          <input
            type="text"
            className="input"
            value={`${vehicle.customerName} (${vehicle.customerPhone})`}
            disabled
          />
        </Field>

        <Field
          label="Chủ sở hữu mới"
          required
          error={errors.newCustomerId?.message}
          hint="Chọn khách hàng tiếp nhận quyền sở hữu phương tiện"
        >
          <select
            className="select"
            disabled={isLoadingCustomers || transfer.isPending}
            {...register('newCustomerId')}
          >
            <option value="">-- Chọn khách hàng chủ mới --</option>
            {eligibleCustomers.map((c) => (
              <option key={c.id} value={c.id}>
                {c.customerCode ? `[${c.customerCode}] ` : ''}{c.fullName} - {c.phone}
              </option>
            ))}
          </select>
        </Field>

        <Field
          label="Số ODO tại thời điểm bàn giao (km)"
          required
          error={errors.transferOdometer?.message}
          hint={`Tối thiểu phải bằng ODO hiện tại (${vehicle.odometer.toLocaleString('vi-VN')} km)`}
        >
          <input
            type="number"
            className="input"
            min={vehicle.odometer}
            disabled={transfer.isPending}
            {...register('transferOdometer')}
          />
        </Field>

        <Field label="Ghi chú chuyển nhượng" error={errors.transferNote?.message}>
          <textarea
            className="input min-h-[70px] resize-none"
            placeholder="Nhập lý do chuyển nhượng, số hợp đồng mua bán hoặc giấy tờ liên quan..."
            disabled={transfer.isPending}
            {...register('transferNote')}
          />
        </Field>

        <div className="form-actions account-editor-actions">
          <Button
            type="button"
            variant="ghost"
            disabled={transfer.isPending}
            onClick={onClose}
          >
            Hủy
          </Button>
          <Button
            type="submit"
            variant="primary"
            disabled={transfer.isPending}
          >
            {transfer.isPending ? 'Đang chuyển giao...' : 'Xác nhận chuyển nhượng'}
          </Button>
        </div>
      </form>
    </Sheet>
  );
}
