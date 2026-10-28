import { useCallback, useEffect, useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useToast } from '../../../app/providers/ToastProvider';
import { applyFormError, errorMessage } from '../../../shared/api/errors';
import {
  Button,
  ConfirmDialog,
  Field,
  FormError,
  Loading,
  QueryError,
  Sheet,
} from '../../../shared/components/ui';
import type { Customer } from '../../../shared/types/contracts';
import { useUnsavedChangesGuard } from '../../../shared/hooks/useUnsavedChangesGuard';
import {
  useCreateCustomerMutation,
  useUpdateCustomerMutation,
  useCustomerQuery,
} from '../hooks/useCustomers';
import {
  customerDefaults,
  customerFormSchema,
  type CustomerFormValues,
} from '../schemas/customerSchema';

function CustomerForm({
  existing,
  onSaved,
  onRequestClose,
}: {
  existing?: Customer;
  onSaved: () => void;
  onRequestClose: (handler: () => void) => void;
}) {
  const notify = useToast();
  const create = useCreateCustomerMutation();
  const update = useUpdateCustomerMutation();
  const [discard, setDiscard] = useState(false);

  const {
    register,
    handleSubmit,
    watch,
    setError,
    formState: { errors, isDirty },
  } = useForm<CustomerFormValues>({
    resolver: zodResolver(customerFormSchema),
    mode: 'onBlur',
    defaultValues: customerDefaults(existing),
  });

  const selectedType = watch('customerType');
  const busy = create.isPending || update.isPending;
  const blocker = useUnsavedChangesGuard(isDirty && !busy);

  useEffect(() => {
    onRequestClose(() => {
      if (busy) return;
      if (isDirty) setDiscard(true);
      else onSaved();
    });
  }, [onRequestClose, busy, isDirty, onSaved]);

  async function onSubmit(values: CustomerFormValues) {
    if (busy) return;
    try {
      if (existing) {
        await update.mutateAsync({
          id: existing.id,
          values: {
            fullName: values.fullName.trim(),
            customerType: values.customerType,
            phone: values.phone.trim(),
            email: values.email?.trim() || null,
            address: values.address?.trim() || null,
            companyName:
              values.customerType === 'BUSINESS' ? values.companyName?.trim() || null : null,
            taxCode: values.customerType === 'BUSINESS' ? values.taxCode?.trim() || null : null,
            creditLimit: values.creditLimit,
            paymentTermDays: values.paymentTermDays,
          },
        });
        notify('Cập nhật hồ sơ khách hàng thành công');
      } else {
        await create.mutateAsync({
          fullName: values.fullName.trim(),
          customerType: values.customerType,
          phone: values.phone.trim(),
          email: values.email?.trim() || null,
          address: values.address?.trim() || null,
          companyName:
            values.customerType === 'BUSINESS' ? values.companyName?.trim() || null : null,
          taxCode: values.customerType === 'BUSINESS' ? values.taxCode?.trim() || null : null,
          creditLimit: values.creditLimit,
          paymentTermDays: values.paymentTermDays,
        });
        notify('Tạo mới hồ sơ khách hàng thành công');
      }
      onSaved();
    } catch (err: unknown) {
      applyFormError(err, setError, Object.keys(values));
    }
  }

  const generalError = create.error || update.error;

  return (
    <>
      <form
        onSubmit={handleSubmit(onSubmit)}
        className="editor-form account-editor-form"
        noValidate
      >
        <FormError message={generalError ? errorMessage(generalError) : undefined} />

        <div className="form-section-label">THÔNG TIN KHÁCH HÀNG</div>
        <Field label="Họ và tên khách hàng" error={errors.fullName?.message} required>
          <input
            {...register('fullName')}
            type="text"
            className="input"
            placeholder="Ví dụ: Nguyễn Văn A"
            disabled={busy}
            autoFocus
          />
        </Field>

        <Field label="Số điện thoại liên hệ" error={errors.phone?.message} required>
          <input
            {...register('phone')}
            type="tel"
            className="input"
            placeholder="Ví dụ: 0912345678"
            disabled={busy}
          />
        </Field>

        <Field label="Phân loại khách hàng" error={errors.customerType?.message} required>
          <select {...register('customerType')} className="select" disabled={busy}>
            <option value="INDIVIDUAL">Khách hàng Cá nhân</option>
            <option value="BUSINESS">Khách hàng Doanh nghiệp / Tổ chức</option>
          </select>
        </Field>

        <Field label="Địa chỉ Email" error={errors.email?.message} hint="Không bắt buộc. Xóa nội dung để gỡ email.">
          <input
            {...register('email')}
            type="email"
            className="input"
            placeholder="khachhang@example.com"
            disabled={busy}
          />
        </Field>

        <div className="full-width-field">
          <Field label="Địa chỉ liên hệ / Nơi cư trú" error={errors.address?.message}>
            <input
              {...register('address')}
              type="text"
              className="input"
              placeholder="Số nhà, đường phố, quận huyện, tỉnh thành..."
              disabled={busy}
            />
          </Field>
        </div>

        {selectedType === 'BUSINESS' && (
          <>
            <div className="form-section-label">THÔNG TIN DOANH NGHIỆP</div>
            <Field
              label="Tên đầy đủ của Công ty / Doanh nghiệp"
              error={errors.companyName?.message}
              required
            >
              <input
                {...register('companyName')}
                type="text"
                className="input"
                placeholder="Công ty TNHH Vận tải ABC"
                disabled={busy}
              />
            </Field>

            <Field label="Mã số thuế doanh nghiệp" error={errors.taxCode?.message}>
              <input
                {...register('taxCode')}
                type="text"
                className="input"
                placeholder="Ví dụ: 0101234567"
                disabled={busy}
              />
            </Field>
          </>
        )}

        <div className="form-section-label">CHÍNH SÁCH CÔNG NỢ & THANH TOÁN</div>
        <Field
          label="Hạn mức nợ tối đa (VND)"
          error={errors.creditLimit?.message}
          hint="Hạn mức dư nợ tối đa cho phép đối với khách hàng này"
        >
          <input
            {...register('creditLimit')}
            type="number"
            step="500000"
            min="0"
            className="input"
            disabled={busy}
          />
        </Field>

        <Field
          label="Thời hạn thanh toán nợ (Ngày)"
          error={errors.paymentTermDays?.message}
          hint="Số ngày tối đa được nợ tiền sau khi xuất hóa đơn dịch vụ"
        >
          <input
            {...register('paymentTermDays')}
            type="number"
            step="1"
            min="0"
            className="input"
            disabled={busy}
          />
        </Field>

        <div className="form-actions account-editor-actions">
          <Button
            disabled={busy}
            onClick={() => {
              if (isDirty) setDiscard(true);
              else onSaved();
            }}
          >
            Hủy
          </Button>
          <Button
            variant="primary"
            type="submit"
            pending={busy}
            disabled={Boolean(existing && !isDirty)}
          >
            {busy ? 'Đang lưu…' : existing ? 'Lưu thay đổi' : 'Thêm khách hàng'}
          </Button>
        </div>
      </form>

      <ConfirmDialog
        open={discard || blocker.state === 'blocked'}
        title="Bỏ thay đổi chưa lưu?"
        description="Nội dung đang nhập sẽ được xóa. Dữ liệu đã lưu trên máy chủ không thay đổi."
        confirmLabel="Bỏ thay đổi"
        onConfirm={() => {
          setDiscard(false);
          if (blocker.state === 'blocked') blocker.proceed?.();
          else onSaved();
        }}
        onCancel={() => {
          setDiscard(false);
          blocker.reset?.();
        }}
      />
    </>
  );
}

export function CustomerEditor({
  customerId,
  open,
  onClose,
  onRestoreFocus,
}: {
  customerId: string | null;
  open: boolean;
  onClose: () => void;
  onRestoreFocus?: () => void;
}) {
  const isEditing = Boolean(customerId);
  const detailQuery = useCustomerQuery(customerId);
  const [closeHandler, setCloseHandler] = useState<(() => void) | null>(null);
  const registerCloseHandler = useCallback(
    (handler: () => void) => setCloseHandler(() => handler),
    [],
  );

  const title = isEditing ? 'Chỉnh sửa hồ sơ khách hàng' : 'Thêm khách hàng';
  const description = isEditing
    ? 'Cập nhật thông tin liên hệ, loại hình khách hàng và chính sách công nợ.'
    : 'Cấp quyền và quản lý thông tin khách hàng mới trong hệ thống gara.';

  return (
    <Sheet
      open={open}
      title={title}
      description={description}
      onClose={() => {
        if (closeHandler) closeHandler();
        else onClose();
      }}
      onRestoreFocus={onRestoreFocus}
      placement="center"
      size="lg"
    >
      {isEditing && detailQuery.isLoading && <Loading label="Đang lấy phiên bản mới nhất…" />}
      {isEditing && detailQuery.isError && (
        <QueryError error={detailQuery.error} retry={() => detailQuery.refetch()} />
      )}
      {(!isEditing || detailQuery.data) && (
        <CustomerForm
          existing={detailQuery.data}
          onSaved={onClose}
          onRequestClose={registerCloseHandler}
        />
      )}
    </Sheet>
  );
}
