import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useToast } from '../../../app/providers/ToastProvider';
import { errorMessage } from '../../../shared/api/errors';
import { Button, Field, FormError, Sheet } from '../../../shared/components/ui';
import { roleLabels, roles } from '../../../shared/types/contracts';
import { useBroadcastNotificationMutation } from '../hooks/useNotifications';
import {
  BROADCAST_LINK_TARGETS,
  BROADCAST_PRIORITIES,
  parseNotification,
} from '../utils/notificationParser';
import { NotificationCard } from './NotificationCard';

const priorityValues = BROADCAST_PRIORITIES.map((item) => item.value) as [
  (typeof BROADCAST_PRIORITIES)[number]['value'],
  ...(typeof BROADCAST_PRIORITIES)[number]['value'][],
];
const linkValues = ['', ...BROADCAST_LINK_TARGETS.map((item) => item.url)] as [string, ...string[]];

const schema = z.object({
  targetRoles: z.array(z.enum(roles)),
  subject: z
    .string()
    .trim()
    .min(1, 'Nhập tiêu đề thông báo.')
    .max(255, 'Tiêu đề tối đa 255 ký tự.')
    .refine((value) => !/[\r\n]/.test(value), 'Tiêu đề chỉ được viết trên một dòng.'),
  content: z
    .string()
    .trim()
    .min(1, 'Nhập nội dung thông báo.')
    .max(2000, 'Nội dung tối đa 2.000 ký tự.'),
  priority: z.enum(priorityValues),
  actionUrl: z.enum(linkValues),
});
type Values = z.infer<typeof schema>;

export function BroadcastModal({ onClose }: { onClose: () => void }) {
  const notify = useToast();
  const broadcast = useBroadcastNotificationMutation();
  const form = useForm<Values>({
    resolver: zodResolver(schema),
    mode: 'onTouched',
    defaultValues: {
      targetRoles: [],
      subject: '',
      content: '',
      priority: 'normal',
      actionUrl: '',
    },
  });
  const selected = form.watch('targetRoles');
  const [subject, content, priority, actionUrl] = form.watch([
    'subject',
    'content',
    'priority',
    'actionUrl',
  ]);
  const preview = parseNotification(
    {
      id: '0',
      channel: 'WEB',
      subject: subject.trim() || 'Tiêu đề thông báo',
      payloadJson: broadcastPayload({
        content: content.trim() || 'Nội dung thông báo sẽ hiển thị tại đây.',
        priority,
        actionUrl,
      }),
      status: 'DELIVERED',
      readAt: null,
      createdAt: new Date().toISOString(),
    },
    Intl.DateTimeFormat().resolvedOptions().timeZone,
  );
  const busy = broadcast.isPending || form.formState.isSubmitting;
  return (
    <Sheet
      open
      placement="center"
      size="md"
      title="Phát thông báo"
      description="Soạn nội dung và chọn nhóm người nhận."
      onClose={onClose}
      pending={busy}
    >
      <form
        className="editor-form notification-compose"
        noValidate
        onSubmit={form.handleSubmit(async (values) => {
          try {
            const result = await broadcast.mutateAsync({
              targetRoles: values.targetRoles,
              subject: values.subject,
              payloadJson: broadcastPayload(values),
            });
            notify(`Đã gửi thông báo tới ${result.recipientCount} tài khoản.`);
            onClose();
          } catch (error) {
            form.setError('root', { message: errorMessage(error) });
          }
        })}
      >
        <fieldset className="notification-audience" disabled={busy}>
          <legend>Người nhận</legend>
          <label className="notification-all">
            <input
              type="checkbox"
              checked={!selected.length}
              onChange={() => form.setValue('targetRoles', [], { shouldDirty: true })}
            />
            Tất cả tài khoản đang hoạt động
          </label>
          <div className="notification-role-options">
            {roles.map((role) => (
              <label key={role}>
                <input type="checkbox" value={role} {...form.register('targetRoles')} />
                {roleLabels[role]}
              </label>
            ))}
          </div>
          <p>
            {selected.length
              ? `Đã chọn ${selected.length} nhóm người nhận.`
              : 'Thông báo sẽ được gửi tới toàn hệ thống.'}
          </p>
        </fieldset>
        <Field label="Tiêu đề" required error={form.formState.errors.subject?.message}>
          <input
            className="input"
            {...form.register('subject')}
            maxLength={255}
            disabled={busy}
            placeholder="Thông tin cần thông báo"
          />
        </Field>
        <Field label="Nội dung" required error={form.formState.errors.content?.message}>
          <textarea
            className="input"
            {...form.register('content')}
            maxLength={2000}
            rows={5}
            disabled={busy}
            placeholder="Viết nội dung ngắn gọn, rõ ràng…"
          />
        </Field>
        <div className="form-grid">
          <Field label="Loại thông báo">
            <select className="input" {...form.register('priority')} disabled={busy}>
              {BROADCAST_PRIORITIES.map((item) => (
                <option key={item.value} value={item.value}>
                  {item.label}
                </option>
              ))}
            </select>
          </Field>
          <Field label="Liên kết đính kèm">
            <select className="input" {...form.register('actionUrl')} disabled={busy}>
              <option value="">Không đính kèm</option>
              {BROADCAST_LINK_TARGETS.map((item) => (
                <option key={item.url} value={item.url}>
                  {item.label}
                </option>
              ))}
            </select>
          </Field>
        </div>
        <div className="broadcast-preview-box" aria-label="Xem trước thông báo">
          <span className="broadcast-preview-label">Xem trước trong hộp thư người nhận</span>
          <NotificationCard parsed={preview} preview />
        </div>
        <FormError message={form.formState.errors.root?.message} />
        <div className="form-actions">
          <Button onClick={onClose} disabled={busy}>
            Hủy
          </Button>
          <Button type="submit" variant="primary" pending={busy}>
            Gửi thông báo
          </Button>
        </div>
      </form>
    </Sheet>
  );
}

/** Default "Tin tức chung" without a link keeps the original `{ message }` payload shape. */
function broadcastPayload(values: Pick<Values, 'content' | 'priority' | 'actionUrl'>) {
  return JSON.stringify({
    message: values.content,
    ...(values.priority !== 'normal' ? { priority: values.priority } : {}),
    ...(values.actionUrl ? { actionUrl: values.actionUrl } : {}),
  });
}
