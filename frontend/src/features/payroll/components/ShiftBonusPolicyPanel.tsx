import { useMemo } from 'react';
import type { ColumnDef } from '@tanstack/react-table';
import { DataTable } from '../../../shared/components/DataTable';
import { Badge, Button } from '../../../shared/components/ui';
import { money } from '../../../shared/utils/format';
import { QC_PASS_BONUS, SHIFT_BONUS_TIERS, type PayrollItem } from '../types/payroll';

type RankRow = PayrollItem & { rank: number };

export function ShiftBonusPolicyPanel({
  items,
  monthLabel,
  onOpenSummary,
}: {
  /** Items of the selected month's period, or null when that period does not exist yet. */
  items: PayrollItem[] | null;
  monthLabel: string;
  onOpenSummary: (item: PayrollItem) => void;
}) {
  const ranking: RankRow[] = useMemo(
    () =>
      [...(items ?? [])]
        .sort(
          (a, b) =>
            b.completedOrderCount - a.completedOrderCount ||
            Number(b.commissionAmount) - Number(a.commissionAmount),
        )
        .map((item, index) => ({ ...item, rank: index + 1 })),
    [items],
  );

  const columns: ColumnDef<RankRow>[] = [
    {
      id: 'rank',
      header: 'Hạng',
      cell: ({ row }) => (
        <Badge variant={row.original.rank <= 3 ? 'primary' : 'secondary'}>
          #{row.original.rank}
        </Badge>
      ),
    },
    {
      id: 'employee',
      header: 'Nhân viên',
      cell: ({ row }) => (
        <div>
          <strong>{row.original.employeeName ?? '—'}</strong>
          <div className="description">{row.original.employeeCode}</div>
        </div>
      ),
    },
    {
      id: 'shifts',
      header: 'Ca trực',
      cell: ({ row }) => <span className="tabular">{row.original.shiftCount}</span>,
    },
    {
      id: 'orders',
      header: 'Đơn hoàn thành',
      cell: ({ row }) => (
        <Badge variant={row.original.completedOrderCount > 0 ? 'success' : 'secondary'}>
          {row.original.completedOrderCount} xe
        </Badge>
      ),
    },
    {
      id: 'bonus',
      header: 'Thưởng đơn & hoa hồng',
      cell: ({ row }) => (
        <strong className="tabular">{money(row.original.commissionAmount)}</strong>
      ),
    },
    {
      id: 'actions',
      header: '',
      cell: ({ row }) => (
        <Button variant="ghost" onClick={() => onOpenSummary(row.original)}>
          Xe & ca
        </Button>
      ),
    },
  ];

  return (
    <>
      <section className="panel stack" aria-label="Quy tắc tính lương và thưởng">
        <h2 className="section-title">Quy tắc tính lương & thưởng năng suất</h2>
        <div className="policy-tier-grid">
          <article>
            <span>Lương theo giờ công</span>
            <strong>Giờ thường × Đơn giá</strong>
            <p className="description">
              Cộng giờ tăng ca × Đơn giá × Hệ số tăng ca (mặc định 1,5).
            </p>
          </article>
          {SHIFT_BONUS_TIERS.map((tier) => (
            <article key={tier.label}>
              <span>
                {tier.label} · {tier.range}
              </span>
              <strong>+{money(tier.amount)} / đơn</strong>
              <p className="description">Tính cho mỗi đơn nghiệm thu xong trong ca trực.</p>
            </article>
          ))}
          <article>
            <span>Thưởng nghiệm thu QC</span>
            <strong>+{money(QC_PASS_BONUS)} / lượt</strong>
            <p className="description">KTV kiểm định cho mỗi lượt QC đạt chuẩn.</p>
          </article>
        </div>
      </section>

      <section className="panel table-panel" aria-label="Bảng xếp hạng năng suất">
        <h2 className="section-title">Xếp hạng năng suất {monthLabel}</h2>
        <DataTable
          caption={`Xếp hạng thưởng đơn ${monthLabel}`}
          columns={columns}
          data={ranking}
          hideOnMobile={['shifts', 'bonus']}
          emptyTitle="Chưa có số liệu tháng này"
          emptyDescription="Khởi tạo và tính kỳ lương ở mục Bảng lương tháng để xem xếp hạng."
        />
      </section>
    </>
  );
}
