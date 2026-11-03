import { useCallback, useEffect, useMemo, useRef, useState, type FormEvent } from 'react';
import { useForm, type Path } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useToast } from '../../../app/providers/ToastProvider';
import { ApiError, applyFormError, errorMessage } from '../../../shared/api/errors';
import { CalendarClock } from 'lucide-react';
import { Button, ConfirmDialog, Field, FormError, Sheet } from '../../../shared/components/ui';
import { useDebouncedValue } from '../../../shared/hooks/useDebouncedValue';
import { useUnsavedChangesGuard } from '../../../shared/hooks/useUnsavedChangesGuard';
import { useAuth } from '../../../app/providers/AuthProvider';
import { useCustomersQuery, useMyCustomerQuery } from '../../customer/hooks/useCustomers';
import { useVehiclesQuery } from '../../vehicle/hooks/useVehicles';
import { useServicesQuery } from '../../catalog/hooks/useCatalog';
import { useCreateBookingMutation } from '../hooks/useReception';
import { BookingTimeSlotPicker } from './BookingTimeSlotPicker';
import { PICKER_TIME_SLOTS, formatAppointmentDisplay, getInitialAppointmentSlot, isSlotInPast, todayKey } from '../constants/bookingSlots';
import {
  bookingDefaults,
  bookingFormSchema,
  bookingPayload,
  bookingStepOneFields,
  normalizePhone,
  normalizePlate,
  type BookingFormValues,
} from '../schemas/receptionSchema';

type BookingStep = 1 | 2;

const NEW_VEHICLE_OPTION = '__new_vehicle__';
const PHONE_DUPLICATE_CODE = 2014;
const PLATE_DUPLICATE_CODE = 2015;
const VEHICLE_OWNER_MISMATCH_CODE = 2018;

const SERVER_FIELDS: readonly string[] = [
  ...bookingStepOneFields,
  'serviceId',
  'bayId',
  'appointmentAt',
  'requestDescription',
];

const STEPS: { step: BookingStep; label: string }[] = [
  { step: 1, label: 'Khách hàng & xe' },
  { step: 2, label: 'Lịch hẹn' },
];

function BookingForm({
  onSaved,
  onRequestClose,
  initialAppointmentAt,
}: {
  onSaved: () => void;
  onRequestClose: (handler: () => void) => void;
  initialAppointmentAt?: string;
}) {
  const notify = useToast();
  const { user } = useAuth();
  const isCustomer = user?.role === 'CUSTOMER';
  const create = useCreateBookingMutation();
  const [discard, setDiscard] = useState(false);
  const [step, setStep] = useState<BookingStep>(1);
  const pendingFocus = useRef<Path<BookingFormValues> | null>(null);

  const directory = useCustomersQuery({ isActive: true }, !isCustomer);
  const ownProfile = useMyCustomerQuery(isCustomer);
  const customers = useMemo(
    () => (isCustomer ? (ownProfile.data ? [ownProfile.data] : []) : (directory.data ?? [])),
    [isCustomer, ownProfile.data, directory.data],
  );
  const isLoadingCustomers = isCustomer ? ownProfile.isLoading : directory.isLoading;
  const { data: allVehicles = [], isLoading: isLoadingVehicles } = useVehiclesQuery({
    isActive: true,
  });
  const { data: services = [], isLoading: isLoadingServices } = useServicesQuery({
    isActive: true,
  });

  const {
    register,
    handleSubmit,
    watch,
    trigger,
    setError,
    clearErrors,
    setFocus,
    setValue,
    formState: { errors, isDirty },
  } = useForm<BookingFormValues>({
    resolver: zodResolver(bookingFormSchema),
    mode: 'onBlur',
    defaultValues: { ...bookingDefaults(), appointmentAt: getInitialAppointmentSlot(initialAppointmentAt) },
  });

  useEffect(() => {
    if (isCustomer && ownProfile.data?.id) {
      setValue('customerId', ownProfile.data.id);
    }
  }, [isCustomer, ownProfile.data, setValue]);

  useEffect(() => {
    if (!pendingFocus.current) return;
    setFocus(pendingFocus.current);
    pendingFocus.current = null;
  }, [step, setFocus]);

  const customerMode = watch('customerMode');
  const vehicleMode = watch('vehicleMode');
  const selectedCustomerId = watch('customerId');
  const selectedVehicleId = watch('vehicleId');
  const isNewCustomer = customerMode === 'new';
  const isNewVehicle = isNewCustomer || vehicleMode === 'new';

  const customerVehicles = useMemo(() => {
    if (isNewCustomer || !selectedCustomerId) return [];
    return allVehicles.filter((v) => v.customerId === selectedCustomerId);
  }, [allVehicles, isNewCustomer, selectedCustomerId]);

  const debouncedPhone = useDebouncedValue(normalizePhone(watch('newCustomer.phone')));
  const debouncedPlate = useDebouncedValue(normalizePlate(watch('newVehicle.licensePlate')));

  const phoneOwner = useMemo(() => {
    if (!isNewCustomer || !debouncedPhone) return undefined;
    return customers.find((c) => normalizePhone(c.phone) === debouncedPhone);
  }, [customers, debouncedPhone, isNewCustomer]);

  const plateMatch = useMemo(() => {
    if (!isNewVehicle || !debouncedPlate) return undefined;
    return allVehicles.find((v) => normalizePlate(v.licensePlate) === debouncedPlate);
  }, [allVehicles, debouncedPlate, isNewVehicle]);
  const plateBelongsToCustomer =
    !!plateMatch && !isNewCustomer && plateMatch.customerId === selectedCustomerId;

  const busy = create.isPending;
  const blocker = useUnsavedChangesGuard(isDirty && !busy);

  useEffect(() => {
    onRequestClose(() => {
      if (busy) return;
      if (isDirty) setDiscard(true);
      else onSaved();
    });
  }, [onRequestClose, busy, isDirty, onSaved]);

  function chooseCustomerMode(mode: BookingFormValues['customerMode']) {
    if (mode === customerMode) return;
    setValue('customerMode', mode, { shouldDirty: true });
    setValue('vehicleMode', mode === 'new' ? 'new' : 'existing', { shouldDirty: true });
    setValue('customerId', '');
    setValue('vehicleId', '');
    clearErrors();
  }

  function selectExistingCustomer(customerId: string) {
    setValue('customerMode', 'existing', { shouldDirty: true });
    setValue('customerId', customerId, { shouldDirty: true });
    setValue('vehicleId', '');
    clearErrors('newCustomer');
  }

  function selectExistingVehicle(vehicleId: string) {
    setValue('vehicleMode', 'existing', { shouldDirty: true });
    setValue('vehicleId', vehicleId, { shouldDirty: true });
    clearErrors('newVehicle');
  }

  function showStepOneError(name: Path<BookingFormValues>, message: string) {
    pendingFocus.current = name;
    setStep(1);
    setError(name, { type: 'server', message });
  }

  async function goToSchedule() {
    const valid = await trigger([...bookingStepOneFields], { shouldFocus: true });
    if (!valid) return;
    if (phoneOwner) {
      setError(
        'newCustomer.phone',
        { message: 'Số điện thoại đã có hồ sơ. Hãy dùng hồ sơ có sẵn.' },
        { shouldFocus: true },
      );
      return;
    }
    if (plateMatch) {
      if (plateBelongsToCustomer) {
        selectExistingVehicle(plateMatch.id);
      } else {
        const owner = plateMatch.customerName ? ' (' + plateMatch.customerName + ')' : '';
        setError(
          'newVehicle.licensePlate',
          { message: 'Biển số đã đăng ký cho xe của khách khác' + owner + '.' },
          { shouldFocus: true },
        );
        return;
      }
    }
    pendingFocus.current = 'appointmentAt';
    setStep(2);
  }

  async function onSubmit(values: BookingFormValues) {
    if (busy) return;
    const appointmentDate = new Date(values.appointmentAt);
    if (isNaN(appointmentDate.getTime())) {
      setError('appointmentAt', { message: 'Thời gian hẹn không hợp lệ' });
      return;
    }
    if (appointmentDate.getTime() < Date.now()) {
      setError('appointmentAt', { message: 'Thời gian hẹn không được ở trong quá khứ' });
      return;
    }
    try {
      await create.mutateAsync(bookingPayload(values, appointmentDate.toISOString()));
      notify('Đặt lịch hẹn thành công');
      onSaved();
    } catch (err: unknown) {
      if (
        err instanceof ApiError &&
        err.code === PHONE_DUPLICATE_CODE &&
        values.customerMode === 'new'
      ) {
        showStepOneError('newCustomer.phone', err.message);
        return;
      }
      if (err instanceof ApiError && err.code === PLATE_DUPLICATE_CODE) {
        showStepOneError('newVehicle.licensePlate', err.message);
        return;
      }
      if (err instanceof ApiError && err.code === VEHICLE_OWNER_MISMATCH_CODE) {
        showStepOneError('vehicleId', err.message);
        return;
      }
      applyFormError(err, setError, SERVER_FIELDS);
    }
  }

  function handleFormSubmit(event: FormEvent<HTMLFormElement>) {
    if (step === 1) {
      event.preventDefault();
      void goToSchedule();
      return;
    }
    void handleSubmit(onSubmit)(event);
  }

  const generalError = create.error;
  const summaryCustomer = isNewCustomer
    ? watch('newCustomer.fullName')
    : customers.find((c) => c.id === selectedCustomerId)?.fullName;
  const summaryVehicle = isNewVehicle
    ? watch('newVehicle.licensePlate')
    : customerVehicles.find((v) => v.id === selectedVehicleId)?.licensePlate;

  const customerRegistration = register('customerId', {
    onChange: () => setValue('vehicleId', ''),
  });
  const currentAppointmentAt = watch('appointmentAt');
  const selectedBookingDate = currentAppointmentAt ? currentAppointmentAt.slice(0, 10) : todayKey();
  const selectedBookingTime =
    currentAppointmentAt && currentAppointmentAt.includes('T')
      ? currentAppointmentAt.slice(11, 16)
      : '';

  const vehicleRegistration = register('vehicleId', {
    onChange: (event) => {
      if (event.target.value === NEW_VEHICLE_OPTION) {
        setValue('vehicleId', '');
        setValue('vehicleMode', 'new', { shouldDirty: true });
      }
    },
  });

  return (
    <>
      <form className="editor-form account-editor-form grid-3col" onSubmit={handleFormSubmit} noValidate>
        <ol className="form-stepper full-width" aria-label="Các bước đặt lịch hẹn">
          {STEPS.map((item) => (
            <li
              key={item.step}
              className={
                item.step === step ? 'is-active' : item.step < step ? 'is-done' : undefined
              }
              aria-current={item.step === step ? 'step' : undefined}
            >
              <span aria-hidden>{item.step}</span>
              {item.label}
            </li>
          ))}
        </ol>

        <FormError message={generalError ? errorMessage(generalError) : undefined} />

        {step === 1 ? (
          <>
            {!isCustomer && (
              <div className="full-width" role="group" aria-label="Loại khách hàng">
                <div className="status-segments">
                  <button
                    type="button"
                    aria-pressed={!isNewCustomer}
                    disabled={busy}
                    onClick={() => chooseCustomerMode('existing')}
                  >
                    Khách đã có hồ sơ
                  </button>
                  <button
                    type="button"
                    aria-pressed={isNewCustomer}
                    disabled={busy}
                    onClick={() => chooseCustomerMode('new')}
                  >
                    Khách mới
                  </button>
                </div>
              </div>
            )}

            <div className="form-section-label">KHÁCH HÀNG</div>

            {isNewCustomer ? (
              <>
                <Field
                  label="Họ tên khách hàng"
                  required
                  error={errors.newCustomer?.fullName?.message}
                >
                  <input
                    className="input"
                    autoComplete="name"
                    disabled={busy}
                    {...register('newCustomer.fullName')}
                  />
                </Field>
                <Field
                  label="Số điện thoại"
                  required
                  error={errors.newCustomer?.phone?.message}
                  hint="Dùng để tra hồ sơ trùng"
                >
                  <input
                    className="input"
                    type="tel"
                    inputMode="tel"
                    autoComplete="tel"
                    disabled={busy}
                    {...register('newCustomer.phone')}
                  />
                </Field>
                {phoneOwner && (
                  <div className="alert full-width booking-match" role="status">
                    <span>
                      Số này đã có hồ sơ: <strong>{phoneOwner.fullName}</strong>
                      {phoneOwner.customerCode ? ` (${phoneOwner.customerCode})` : ''}.
                    </span>
                    <Button
                      variant="secondary"
                      onClick={() => selectExistingCustomer(phoneOwner.id)}
                    >
                      Dùng hồ sơ này
                    </Button>
                  </div>
                )}
                <Field label="Email" error={errors.newCustomer?.email?.message}>
                  <input
                    className="input"
                    type="email"
                    autoComplete="email"
                    disabled={busy}
                    {...register('newCustomer.email')}
                  />
                </Field>
              </>
            ) : (
              <Field
                label="Khách hàng đặt hẹn"
                required
                error={errors.customerId?.message}
                hint={isCustomer ? undefined : 'Chọn hồ sơ khách hàng đã có trên hệ thống'}
              >
                {isCustomer ? (
                  <input
                    type="text"
                    className="input"
                    value={user?.fullName ?? 'Hồ sơ cá nhân của bạn'}
                    disabled
                  />
                ) : (
                  <select
                    className="select"
                    disabled={isLoadingCustomers || busy}
                    {...customerRegistration}
                  >
                    <option value="">-- Chọn khách hàng --</option>
                    {customers.map((c) => (
                      <option key={c.id} value={c.id}>
                        {c.customerCode ? `[${c.customerCode}] ` : ''}
                        {c.fullName} - {c.phone}
                      </option>
                    ))}
                  </select>
                )}
              </Field>
            )}

            <div className="form-section-label">PHƯƠNG TIỆN</div>

            {isNewVehicle ? (
              <>
                <Field
                  label="Biển số"
                  required
                  error={
                    errors.newVehicle?.licensePlate?.message ??
                    (plateMatch
                      ? plateBelongsToCustomer
                        ? 'Xe này đã có trong hồ sơ của khách.'
                        : `Biển số đã có trên hệ thống (chủ xe: ${plateMatch.customerName}).`
                      : undefined)
                  }
                  hint={
                    !isNewCustomer && !isCustomer ? (
                      <button
                        type="button"
                        className="button button-ghost"
                        disabled={busy}
                        onClick={() => setValue('vehicleMode', 'existing', { shouldDirty: true })}
                      >
                        Chọn xe có sẵn
                      </button>
                    ) : undefined
                  }
                >
                  <input
                    className="input"
                    autoCapitalize="characters"
                    disabled={busy}
                    {...register('newVehicle.licensePlate')}
                  />
                </Field>
                {plateMatch && plateBelongsToCustomer && (
                  <div className="alert full-width booking-match" role="status">
                    <span>
                      <strong>{plateMatch.licensePlate}</strong> ({plateMatch.brand}{' '}
                      {plateMatch.model}) đã thuộc khách này.
                    </span>
                    <Button
                      variant="secondary"
                      onClick={() => selectExistingVehicle(plateMatch.id)}
                    >
                      Chọn xe này
                    </Button>
                  </div>
                )}
                <Field label="Hãng xe" required error={errors.newVehicle?.brand?.message}>
                  <input className="input" disabled={busy} {...register('newVehicle.brand')} />
                </Field>
                <Field label="Dòng xe" required error={errors.newVehicle?.model?.message}>
                  <input className="input" disabled={busy} {...register('newVehicle.model')} />
                </Field>
                <Field label="Năm sản xuất" error={errors.newVehicle?.manufactureYear?.message}>
                  <input
                    className="input"
                    type="number"
                    inputMode="numeric"
                    disabled={busy}
                    {...register('newVehicle.manufactureYear')}
                  />
                </Field>
                <Field label="Màu xe" error={errors.newVehicle?.color?.message}>
                  <input className="input" disabled={busy} {...register('newVehicle.color')} />
                </Field>
              </>
            ) : (
              <Field
                label="Phương tiện dịch vụ"
                required
                error={errors.vehicleId?.message}
                hint={
                  !selectedCustomerId
                    ? 'Vui lòng chọn khách hàng trước'
                    : customerVehicles.length === 0
                      ? isCustomer
                        ? 'Bạn chưa có xe đăng ký trên hệ thống'
                        : 'Khách chưa có xe, chọn “Thêm xe mới cho khách này”'
                      : 'Chọn xe cần bảo dưỡng / sửa chữa'
                }
              >
                <select
                  className="select"
                  disabled={!selectedCustomerId || isLoadingVehicles || busy}
                  {...vehicleRegistration}
                >
                  <option value="">-- Chọn xe --</option>
                  {customerVehicles.map((v) => (
                    <option key={v.id} value={v.id}>
                      {v.licensePlate} ({v.brand} {v.model})
                    </option>
                  ))}
                  {!isCustomer && (
                    <option value={NEW_VEHICLE_OPTION}>+ Thêm xe mới cho khách này</option>
                  )}
                </select>
              </Field>
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
              <Button type="submit" variant="primary">
                Tiếp tục
              </Button>
            </div>
          </>
        ) : (
          <>
            <p className="booking-step-summary full-width">
              Khách hàng: <strong>{summaryCustomer || '—'}</strong>
              {isNewCustomer ? ' (mới)' : ''} · Xe: <strong>{summaryVehicle || '—'}</strong>
              {isNewVehicle ? ' (mới)' : ''}
            </p>

            <div className="modal-split-columns">
              <div className="modal-column-stack">
                <Field
                  label="Ngày hẹn tiếp nhận"
                  required
                  error={errors.appointmentAt?.message}
                  hint="Chọn ngày đưa xe vào gara"
                >
                  <input
                    type="date"
                    className="input"
                    min={todayKey()}
                    disabled={busy}
                    value={selectedBookingDate}
                    onChange={(e) => {
                      const newDate = e.target.value;
                      if (!newDate) return;
                      const time = selectedBookingTime || '08:30';
                      if (isSlotInPast(newDate, time)) {
                        const validSlot = PICKER_TIME_SLOTS.find((s) => !isSlotInPast(newDate, s));
                        setValue(
                          'appointmentAt',
                          validSlot ? `${newDate}T${validSlot}` : `${newDate}T08:30`,
                          { shouldDirty: true, shouldValidate: true },
                        );
                      } else {
                        setValue('appointmentAt', `${newDate}T${time}`, {
                          shouldDirty: true,
                          shouldValidate: true,
                        });
                      }
                    }}
                  />
                </Field>

                <div className="appointment-selected-card" role="status">
                  <CalendarClock size={16} />
                  <span>
                    Khung giờ đã chọn:{' '}
                    <strong>
                      {selectedBookingTime && !isSlotInPast(selectedBookingDate, selectedBookingTime)
                        ? formatAppointmentDisplay(currentAppointmentAt)
                        : 'Vui lòng chọn khung giờ bên phải'}
                    </strong>
                  </span>
                </div>

                <Field
                  label="Dịch vụ yêu cầu chính"
                  error={errors.serviceId?.message}
                  hint="Để trống nếu cần khám trước"
                >
                  <select
                    className="select"
                    disabled={isLoadingServices || busy}
                    {...register('serviceId')}
                  >
                    <option value="">Khám & Chẩn đoán tổng quát</option>
                    {services.map((s) => (
                      <option key={s.id} value={s.id}>
                        {s.serviceName} ({s.serviceType === 'PACKAGE' ? 'Gói BD' : 'Lẻ'})
                      </option>
                    ))}
                  </select>
                </Field>

                <Field
                  label="Mô tả hiện tượng / Yêu cầu cụ thể"
                  error={errors.requestDescription?.message}
                >
                  <textarea
                    rows={2}
                    className="input resize-none"
                    placeholder="Ví dụ: Xe kêu cọt kẹt khi qua gờ giảm tốc..."
                    disabled={busy}
                    {...register('requestDescription')}
                  />
                </Field>
              </div>

              <div>
                <BookingTimeSlotPicker
                  value={watch('appointmentAt')}
                  disabled={busy}
                  onChange={(next) =>
                    setValue('appointmentAt', next, { shouldDirty: true, shouldValidate: true })
                  }
                />
              </div>
            </div>

            <div className="form-actions account-editor-actions">
              <Button disabled={busy} onClick={() => setStep(1)}>
                Quay lại
              </Button>
              <Button type="submit" variant="primary" pending={busy}>
                {busy ? 'Đang lưu…' : 'Xác nhận đặt lịch'}
              </Button>
            </div>
          </>
        )}
      </form>

      <ConfirmDialog
        open={discard || blocker.state === 'blocked'}
        title="Bỏ thay đổi chưa lưu?"
        description="Nội dung lịch hẹn chưa lưu sẽ bị xóa nếu bạn đóng biểu mẫu này."
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

export function BookingEditor({
  open,
  onClose,
  onRestoreFocus,
  initialAppointmentAt,
}: {
  open: boolean;
  onClose: () => void;
  onRestoreFocus?: () => void;
  initialAppointmentAt?: string;
}) {
  const [closeHandler, setCloseHandler] = useState<(() => void) | null>(null);
  const registerCloseHandler = useCallback(
    (handler: () => void) => setCloseHandler(() => handler),
    [],
  );

  return (
    <Sheet
      open={open}
      onClose={() => {
        if (closeHandler) closeHandler();
        else onClose();
      }}
      onRestoreFocus={onRestoreFocus}
      title="Đặt lịch hẹn dịch vụ mới"
      description="Ghi nhận lịch hẹn bảo dưỡng, sửa chữa cho khách hàng và phân bổ khoang dự kiến."
      placement="center"
      size="lg"
    >
      <BookingForm
        onSaved={onClose}
        onRequestClose={registerCloseHandler}
        initialAppointmentAt={initialAppointmentAt}
      />
    </Sheet>
  );
}
