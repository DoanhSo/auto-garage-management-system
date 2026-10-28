import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useToast } from '../../../app/providers/ToastProvider';
import { applyFormError, errorMessage } from '../../../shared/api/errors';
import { Button, Field, FormError, Sheet } from '../../../shared/components/ui';
import type { Vehicle } from '../../../shared/types/contracts';
import { useCustomersQuery } from '../../customer/hooks/useCustomers';
import { useTransferOwnershipMutation } from '../hooks/useVehicles';
import {
  transferDefaults,
  transferOwnershipSchema,
  type TransferOwnershipFormValues,
} from '../schemas/vehicleSchema';
import { ShieldAlert } from 'lucide-react';

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
  const { data: customers = [], isLoading: isLoadingCustomers } = useCustomersQuery({
    isActive: true,
  });

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
          transferOdometer: values.transferOdometer
            ? Number(values.transferOdometer)
            : vehicle.odometer,
          transferNote: values.transferNote?.trim() || null,
        },
      });
      notify(`Đã chuyển nhượng quyền sở hữu xe ${vehicle.licensePlate} thành công`);
      reset();
      onClose();
    } catch (err: unknown) {
      applyFormError(err, setError, Object.keys(values));
    }
  }

  const generalError = transfer.error;

  return (
    <Sheet
      open={open}
      onClose={onClose}
      placement="center"
      size="md"
      title={`Chuyển nhượng xe ${vehicle.licensePlate}`}
      description={`Chuyển giao quyền sở hữu xe ${vehicle.brand} ${vehicle.model} cho khách hàng mới.`}
    >
      <form
        onSubmit={handleSubmit(onSubmit)}
        noValidate
        className="editor-form account-editor-form"
      >
        <FormError message={generalError ? errorMessage(generalError) : undefined} />

        <div className="info-strip">
          <ShieldAlert size={18} />
          <span>
            <strong>Quy tắc bảo mật lịch sử :</strong> Lịch sử sửa chữa kỹ thuật trước đây vẫn được lưu trữ trên phương tiện, nhưng toàn bộ dữ liệu tài chính của chủ cũ sẽ tự động ẩn đối với chủ mới.
          </span>
        </div>

        <div className="form-section-label">THÔNG TIN CHUYỂN NHƯỢNG SỞ HỮU</div>

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
          hint="Chọn khách hàng tiếp nhận quyền sở hữu"
        >
          <select
            className="select"
            disabled={isLoadingCustomers || transfer.isPending}
            {...register('newCustomerId')}
          >
            <option value="">-- Chọn khách hàng chủ mới --</option>
            {eligibleCustomers.map((c) => (
              <option key={c.id} value={c.id}>
                {c.customerCode ? `[${c.customerCode}] ` : ''}
                {c.fullName} - {c.phone}
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

        <Field label="Ghi chú chuyển nhượng" error={errors.transferNote?.message} hint="Số hợp đồng mua bán hoặc giấy tờ liên quan">
          <input
            type="text"
            className="input"
            placeholder="Số HĐ mua bán, ủy quyền..."
            disabled={transfer.isPending}
            {...register('transferNote')}
          />
        </Field>

        <div className="form-actions account-editor-actions">
          <Button disabled={transfer.isPending} onClick={onClose}>
            Hủy
          </Button>
          <Button variant="primary" type="submit" pending={transfer.isPending}>
            {transfer.isPending ? 'Đang chuyển giao…' : 'Xác nhận chuyển nhượng'}
          </Button>
        </div>
      </form>
    </Sheet>
  );
}
