import { useForm } from 'react-hook-form';
import { useToast } from '../../../app/providers/ToastProvider';
import { applyFormError, errorMessage } from '../../../shared/api/errors';
import { Button, Field, FormError, Sheet } from '../../../shared/components/ui';
import { useCreateTaskMutation } from '../hooks/useRepair';

export function CreateTaskModal({
  orderId,
  open,
  onClose,
}: {
  orderId: string | number;
  open: boolean;
  onClose: () => void;
}) {
  const notify = useToast();
  const createTask = useCreateTaskMutation();

  const {
    register,
    handleSubmit,
    reset,
    setError,
    formState: { errors },
  } = useForm<{
    taskName: string;
    description: string;
    taskType: 'REPAIR' | 'DIAGNOSIS' | 'MAINTENANCE';
    quantity: number;
    unitPrice: number;
  }>({
    defaultValues: {
      taskName: '',
      description: '',
      taskType: 'REPAIR',
      quantity: 1,
      unitPrice: 0,
    },
  });

  async function onSubmit(values: {
    taskName: string;
    description: string;
    taskType: 'REPAIR' | 'DIAGNOSIS' | 'MAINTENANCE';
    quantity: number;
    unitPrice: number;
  }) {
    try {
      await createTask.mutateAsync({
        orderId,
        data: {
          taskName: values.taskName.trim(),
          description: values.description?.trim() || undefined,
          taskType: values.taskType,
          quantity: Number(values.quantity) || 1,
          unitPrice: Number(values.unitPrice) || 0,
        },
      });
      notify('Đã thêm công việc thành công.');
      reset();
      onClose();
    } catch (err: unknown) {
      applyFormError(err, setError, Object.keys(values));
    }
  }

  return (
    <Sheet
      open={open}
      onClose={onClose}
      placement="center"
      size="md"
      title="Thêm công việc sửa chữa"
      description="Thêm hạng mục công việc vào lệnh sửa chữa để phân công kỹ thuật viên thực hiện."
    >
      <form onSubmit={handleSubmit(onSubmit)} noValidate className="modal-form">
        <FormError message={createTask.error ? errorMessage(createTask.error) : undefined} />

        <div className="full-width">
          <Field label="Tên công việc" required error={errors.taskName?.message}>
            <input
              className="input"
              placeholder="VD: Thay má phanh trước, Thay dầu động cơ..."
              {...register('taskName', { required: 'Tên công việc không được để trống' })}
            />
          </Field>
        </div>

        <Field label="Loại công việc">
          <select className="select" {...register('taskType')}>
            <option value="REPAIR">Sửa chữa</option>
            <option value="DIAGNOSIS">Chẩn đoán</option>
            <option value="MAINTENANCE">Bảo dưỡng</option>
          </select>
        </Field>

        <Field label="Số lượng">
          <input
            type="number"
            step="1"
            min="1"
            className="input"
            {...register('quantity', { valueAsNumber: true })}
          />
        </Field>

        <div className="full-width">
          <Field label="Ghi chú kỹ thuật">
            <textarea
              className="input"
              rows={3}
              placeholder="Ghi chú yêu cầu kỹ thuật hoặc lưu ý an toàn cho kỹ thuật viên..."
              {...register('description')}
            />
          </Field>
        </div>

        <div className="modal-form-actions">
          <Button type="button" variant="secondary" onClick={onClose}>
            Hủy
          </Button>
          <Button type="submit" variant="primary" pending={createTask.isPending}>
            {createTask.isPending ? 'Đang thêm…' : 'Thêm công việc'}
          </Button>
        </div>
      </form>
    </Sheet>
  );
}
