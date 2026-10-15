import { LockKeyhole, LockOpen, SquarePen, Trash2 } from 'lucide-react';
import type { User } from '../../../shared/types/contracts';
import { Button, Empty, RoleBadge, StatusBadge } from '../../../shared/components/ui';
import { initials } from '../../../shared/utils/format';
export function UserCards({
  users,
  currentId,
  onEdit,
  onLock,
  onDelete,
}: {
  users: User[];
  currentId?: string;
  onEdit: (id: string, trigger: HTMLElement) => void;
  onLock: (user: User) => void;
  onDelete: (user: User) => void;
}) {
  if (!users.length)
    return <Empty title="Không có kết quả" description="Thử thay đổi bộ lọc để tìm tài khoản." />;
  return (
    <ul className="user-cards" aria-label="Danh sách tài khoản">
      {users.map((user) => (
        <li key={user.id}>
          <div className="user-card-top">
            <span className="avatar">{initials(user.fullName)}</span>
            <div>
              <strong>
                {user.fullName}
                {user.id === currentId && <span className="you-tag">Bạn</span>}
              </strong>
              <span>{user.username}</span>
            </div>
            <StatusBadge active={user.active} />
          </div>
          <p className="user-card-email">{user.email || 'Chưa cung cấp email'}</p>
          <div className="user-card-bottom">
            <RoleBadge role={user.role} />
            <div className="row-actions">
              <Button
                variant="ghost"
                aria-label={`Sửa tài khoản ${user.fullName}`}
                onClick={(event) => onEdit(user.id, event.currentTarget)}
              >
                <SquarePen size={17} />
                Sửa
              </Button>
              {
                <Button
                  variant="ghost"
                  aria-label={`${user.active ? 'Khóa' : 'Mở khóa'} tài khoản ${user.fullName}`}
                  onClick={() => onLock(user)}
                >
                  {user.active ? <LockKeyhole size={17} /> : <LockOpen size={17} />}
                  {user.active ? 'Khóa' : 'Mở khóa'}
                </Button>
              }
              <Button
                variant="ghost"
                className="delete-action"
                aria-label={`Xóa tài khoản ${user.fullName}`}
                onClick={() => onDelete(user)}
              >
                <Trash2 size={17} />
                Xóa
              </Button>
            </div>
          </div>
        </li>
      ))}
    </ul>
  );
}
