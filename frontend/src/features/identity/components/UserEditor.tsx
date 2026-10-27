import { useEffect, useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useMutation } from '@tanstack/react-query';
import { useAuth } from '../../../app/providers/AuthProvider';
import { useToast } from '../../../app/providers/ToastProvider';
import { ApiError, applyFormError, errorMessage } from '../../../shared/api/errors';
import {
  Button,
  ConfirmDialog,
  Field,
  FormError,
  Loading,
  QueryError,
  ReadOnlyField,
  RoleBadge,
  Sheet,
  StatusBadge,
} from '../../../shared/components/ui';
import { roles, roleLabels, type UiPolicy, type User } from '../../../shared/types/contracts';
import { useUnsavedChangesGuard } from '../../../shared/hooks/useUnsavedChangesGuard';
import { usersApi } from '../api/usersApi';
import { useCreateUserMutation, useUpdateUserMutation, useUserQuery } from '../hooks/useUsers';
import { userDefaults, userFormSchema, type UserFormValues } from '../schemas/userSchema';

function UserForm({
  existing,
  onSaved,
  onRequestClose,
}: {
  existing?: User;
  onSaved: () => void;
  onRequestClose: (handler: () => void) => void;
}) {
  const auth = useAuth();
  const notify = useToast();
  const create = useCreateUserMutation();
  const update = useUpdateUserMutation();
  const [discard, setDiscard] = useState(false);
  const [confirmSelf, setConfirmSelf] = useState<UserFormValues | null>(null);
  const [version, setVersion] = useState(existing?.version ?? '0');
  const [conflict, setConflict] = useState(false);
  const {
    register,
    handleSubmit,
    reset,
    setError,
    getValues,
    formState: { errors, isDirty },
  } = useForm<UserFormValues>({
    resolver: zodResolver(userFormSchema()),
    mode: 'onBlur',
    defaultValues: userDefaults(existing),
  });
  const busy = create.isPending || update.isPending;
  const blocker = useUnsavedChangesGuard(isDirty && !busy);
  const reload = useMutation({
    mutationFn: () => usersApi.get(existing!.id),
    retry: 0,
  });
  const { reset: resetCreate } = create;
  useEffect(() => () => resetCreate(), [resetCreate]);
  useEffect(() => {
    onRequestClose(() => {
      if (busy) return;
      if (isDirty) setDiscard(true);
      else onSaved();
    });
  }, [onRequestClose, busy, isDirty, onSaved]);
  async function save(values: UserFormValues) {
    if (busy) return;
    try {
      const body = {
        fullName: values.fullName.trim(),
        email: values.email.trim() ? values.email.trim().toLowerCase() : null,
        role: z.enum(roles).parse(values.role),
      };
      const result = existing
        ? await update.mutateAsync({
            id: existing.id,
            values: { ...body, active: values.active, version },
          })
        : await create.mutateAsync({
            ...body,
            username: values.username.toLowerCase(),
            ...(values.password.trim() ? { password: values.password.trim() } : {}),
          });
      reset(userDefaults(result));
      setConfirmSelf(null);
      if (
        auth.user &&
        existing?.id === auth.user.id &&
        (result.role !== auth.user.role || !result.active)
      ) {
        auth.clearSession('Quyền truy cập của bạn đã thay đổi. Vui lòng đăng nhập lại.');
        return;
      }
      if (result.id === auth.user?.id) auth.setCurrentUser(result);
      notify(existing ? 'Đã lưu thay đổi tài khoản.' : 'Đã tạo tài khoản thành công.');
      onSaved();
    } catch (error) {
      setConfirmSelf(null);
      applyFormError(error, setError, ['fullName', 'username', 'email', 'role']);
      if (error instanceof ApiError && error.code === 2005) setConflict(true);
    } finally {
      create.reset();
    }
  }
  const submit = handleSubmit((values) => {
    if (
      auth.user &&
      existing?.id === auth.user.id &&
      (values.role !== auth.user.role || !values.active)
    ) {
      setConfirmSelf(values);
      return;
    }
    return save(values);
  });
  return (
    <>
      <form className="editor-form account-editor-form" noValidate onSubmit={submit}>
        <div className="form-section-label">THÔNG TIN TÀI KHOẢN</div>
        <Field label="Họ và tên" required error={errors.fullName?.message}>
          <input
            className="input"
            {...register('fullName')}
            maxLength={150}
            disabled={busy}
            autoComplete="name"
            autoFocus
          />
        </Field>
        <Field
          label="Tên đăng nhập"
          required
          hint={existing ? 'Tên đăng nhập không thể thay đổi.' : '3–100 ký tự: chữ/số, dấu . _ -'}
          error={errors.username?.message}
        >
          <input
            className="input"
            {...register('username')}
            readOnly={!!existing}
            disabled={busy}
            maxLength={100}
            autoComplete="off"
            title="3–100 ký tự: chữ/số ASCII, dấu chấm, gạch dưới hoặc gạch ngang."
          />
        </Field>
        <Field
          label="Email"
          error={errors.email?.message}
          hint="Không bắt buộc. Xóa nội dung để gỡ email."
        >
          <input
            className="input"
            type="email"
            {...register('email')}
            maxLength={254}
            disabled={busy}
            autoComplete="email"
          />
        </Field>
        <Field label="Vai trò" required error={errors.role?.message}>
          <select className="input" {...register('role')} disabled={busy}>
            <option value="">Chọn vai trò</option>
            {roles.map((role) => (
              <option key={role} value={role}>
                {roleLabels[role]}
              </option>
            ))}
          </select>
        </Field>
        {existing ? (
          <label className="checkbox-field">
            <input type="checkbox" {...register('active')} disabled={busy} />
            <span>Cho phép tài khoản hoạt động</span>
          </label>
        ) : (
          <>
            <Field
              label="Mật khẩu khởi tạo Keycloak"
              error={errors.password?.message}
              hint="Tùy chọn (tối thiểu 8 ký tự). Hệ thống đồng bộ trực tiếp tài khoản & vai trò sang Keycloak OIDC."
            >
              <input
                className="input"
                type="password"
                {...register('password')}
                maxLength={72}
                disabled={busy}
                autoComplete="new-password"
                placeholder="Để trống để dùng mật khẩu khởi tạo mặc định"
              />
            </Field>
            <p className="field-hint">
              Tài khoản nhân sự (`ADMIN`, `RECEPTIONIST`, `WAREHOUSE_STAFF`, `TECHNICIAN`) chỉ được
              cấp phát bởi Quản trị viên và đồng bộ tự động sang Keycloak.
            </p>
          </>
        )}
        <FormError message={errors.root?.server?.message} />
        {conflict && (
          <section className="conflict-panel">
            <h3>Dữ liệu đã thay đổi</h3>
            <p>Bản nháp của bạn vẫn được giữ. Tải bản máy chủ để xem lại trước khi tiếp tục.</p>
            <Button onClick={() => reload.mutate()} pending={reload.isPending}>
              Xem bản mới trên máy chủ
            </Button>
            {reload.error && <FormError message={errorMessage(reload.error)} />}{' '}
            {reload.data && (
              <>
                <dl className="detail-list">
                  <ReadOnlyField label="Họ tên trên máy chủ">{reload.data.fullName}</ReadOnlyField>
                  <ReadOnlyField label="Email trên máy chủ">
                    {reload.data.email || 'Chưa cung cấp'}
                  </ReadOnlyField>
                  <ReadOnlyField label="Vai trò trên máy chủ">
                    <RoleBadge role={reload.data.role} />
                  </ReadOnlyField>
                  <ReadOnlyField label="Trạng thái">
                    <StatusBadge active={reload.data.active} />
                  </ReadOnlyField>
                </dl>
                <p>“Dùng bản mới” thay thế bản nháp đang mở.</p>
                <Button
                  onClick={() => {
                    reset(userDefaults(reload.data));
                    setVersion(reload.data!.version);
                    setConflict(false);
                  }}
                >
                  Dùng bản mới
                </Button>
              </>
            )}
          </section>
        )}
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
          <Button variant="primary" type="submit" pending={busy} disabled={!!existing && !isDirty}>
            {busy ? 'Đang lưu…' : existing ? 'Lưu thay đổi' : 'Thêm tài khoản'}
          </Button>
        </div>
      </form>
      <ConfirmDialog
        open={discard || blocker.state === 'blocked'}
        title="Bỏ thay đổi chưa lưu?"
        description="Nội dung đang nhập sẽ được xóa. Dữ liệu đã lưu trên máy chủ không thay đổi."
        onCancel={() => {
          setDiscard(false);
          blocker.reset?.();
        }}
        onConfirm={() => {
          reset(userDefaults(existing));
          if (blocker.state === 'blocked') blocker.proceed();
          else onSaved();
        }}
        confirmLabel="Bỏ thay đổi"
      />
      <ConfirmDialog
        open={!!confirmSelf}
        title="Thay đổi quyền của chính bạn?"
        description="Thao tác này sẽ thu hồi phiên đăng nhập hiện tại sau khi lưu thành công."
        pending={busy}
        onCancel={() => setConfirmSelf(null)}
        onConfirm={() => {
          void save(confirmSelf ?? getValues());
        }}
        confirmLabel="Lưu và đăng nhập lại"
        danger
      />
    </>
  );
}
export function UserEditor({
  id,
  onClose,
  onRestoreFocus,
}: {
  id: 'new' | string;
  policy?: UiPolicy;
  onClose: () => void;
  onRestoreFocus: () => void;
}) {
  const user = useUserQuery(id === 'new' ? null : id);
  const [requestClose, setRequestClose] = useState<() => void>(() => onClose);
  // Register a close callback without treating it as a React updater.
  const registerClose = useState(() => (handler: () => void) => setRequestClose(() => handler))[0];
  return (
    <Sheet
      open
      placement="center"
      title={id === 'new' ? 'Thêm tài khoản' : 'Sửa tài khoản'}
      description={
        id === 'new'
          ? 'Cấp quyền truy cập cho thành viên mới.'
          : 'Cập nhật thông tin và quyền truy cập.'
      }
      onClose={() => requestClose()}
      onRestoreFocus={onRestoreFocus}
    >
      {id !== 'new' && (user.isPending || user.isFetching) ? (
        <Loading label="Đang lấy phiên bản mới nhất…" />
      ) : user.error ? (
        <QueryError
          error={user.error}
          retry={() => {
            void user.refetch();
          }}
        />
      ) : (
        <UserForm
          existing={id === 'new' ? undefined : user.data}
          onSaved={onClose}
          onRequestClose={registerClose}
        />
      )}
    </Sheet>
  );
}
