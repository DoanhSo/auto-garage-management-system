import { Badge, Modal } from '../../../shared/components/ui';
import { formatDate, money } from '../../../shared/utils/format';
import {
  commissionStatusLabels,
  commissionTypeLabels,
  payrollStatusLabels,
  type Payslip,
} from '../types/payroll';
import { shiftTypeLabels, type ShiftType } from '../../scheduling/types/scheduling';

const hoursText = (value: string | number) => `${Number(value).toFixed(1).replace('.', ',')} giờ`;

export function MyPayslipModal({
  payslip,
  onClose,
}: {
  payslip: Payslip | null;
  onClose: () => void;
}) {
  const item = payslip?.item;
  const orderRows = (payslip?.commissions ?? []).filter(
    (commission) => commission.commissionType === 'ORDER_COMPLETION',
  );
  const otherRows = (payslip?.commissions ?? []).filter(
    (commission) => commission.commissionType !== 'ORDER_COMPLETION',
  );

  return (
    <Modal
      open={!!payslip}
      size="xl"
      title={payslip ? `Phiếu lương ${payslip.periodCode}` : 'Phiếu lương'}
      description={
        payslip
          ? `Kỳ công ${formatDate(payslip.fromDate)} – ${formatDate(payslip.toDate)} · ${payrollStatusLabels[payslip.periodStatus]}`
          : ''
      }
      onOpenChange={(open) => !open && onClose()}
    >
      {payslip && item && (
        <div className="dialog-body">
          <dl className="summary-list" aria-label="Tóm tắt phiếu lương">
            <div>
              <dt>Lương cứng · {item.shiftCount} ca trực khoang</dt>
              <dd className="tabular">{money(item.baseSalarySnapshot)}</dd>
            </div>
            <div>
              <dt>Lương giờ công</dt>
              <dd className="tabular">{money(item.hourlyPayAmount)}</dd>
            </div>
            <div>
              <dt>Thưởng đơn hoàn thành · {orderRows.length} lượt</dt>
              <dd className="tabular">{money(item.orderBonusAmount)}</dd>
            </div>
            <div>
              <dt>Hoa hồng & thưởng khác</dt>
              <dd className="tabular">
                {money(Number(item.commissionAmount) - Number(item.orderBonusAmount))}
              </dd>
            </div>
            <div>
              <dt>Phụ cấp</dt>
              <dd className="tabular">{money(item.allowanceAmount)}</dd>
            </div>
            <div>
              <dt>Giảm trừ</dt>
              <dd className="tabular">-{money(item.deductionAmount)}</dd>
            </div>
          </dl>
          <p className="payslip-total">
            Thực lĩnh <strong className="tabular">{money(item.netPay)}</strong>
          </p>

          <section aria-label="Xe đã sửa thành công">
            <h3>Xe đã sửa thành công trong các ca trực</h3>
            {orderRows.length === 0 ? (
              <p className="description">Chưa có đơn sửa thành công nào trong kỳ.</p>
            ) : (
              <div className="table-scroll">
                <table className="data-table">
                  <caption className="sr-only">Danh sách xe đã sửa thành công</caption>
                  <thead>
                    <tr>
                      <th scope="col">Ngày làm việc</th>
                      <th scope="col">Ca trực</th>
                      <th scope="col">Khoang</th>
                      <th scope="col">Biển số xe</th>
                      <th scope="col">Lệnh sửa chữa</th>
                      <th scope="col">Mức thưởng</th>
                    </tr>
                  </thead>
                  <tbody>
                    {orderRows.map((row) => (
                      <tr key={row.id}>
                        <td>{formatDate(row.earnedAt)}</td>
                        <td>
                          {row.shiftType
                            ? (shiftTypeLabels[row.shiftType as ShiftType] ?? row.shiftType)
                            : '—'}
                        </td>
                        <td>{row.bayCode ?? '—'}</td>
                        <td>
                          <strong>{row.licensePlate ?? '—'}</strong>
                        </td>
                        <td className="tabular">#{row.repairOrderId}</td>
                        <td className="tabular">
                          {money(row.commissionAmount)}{' '}
                          <Badge className="ml-1">{commissionStatusLabels[row.status]}</Badge>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </section>

          <div className="payslip-grid">
            <section aria-label="Giờ bấm thực tế">
              <h3>Giờ công theo ngày</h3>
              {payslip.laborDays.length === 0 ? (
                <p className="description">Chưa có giờ bấm thi công trong kỳ.</p>
              ) : (
                <table className="data-table">
                  <caption className="sr-only">Giờ bấm thực tế theo từng ngày</caption>
                  <thead>
                    <tr>
                      <th scope="col">Ngày</th>
                      <th scope="col">Giờ thường</th>
                      <th scope="col">Tăng ca</th>
                    </tr>
                  </thead>
                  <tbody>
                    {payslip.laborDays.map((day) => (
                      <tr key={day.date}>
                        <td>{formatDate(day.date)}</td>
                        <td className="tabular">{hoursText(day.regularHours)}</td>
                        <td className="tabular">{hoursText(day.overtimeHours)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
            </section>

            <section aria-label="Hoa hồng và thưởng khác">
              <h3>Hoa hồng & thưởng khác</h3>
              {otherRows.length === 0 ? (
                <p className="description">Chưa có khoản nào trong kỳ.</p>
              ) : (
                <table className="data-table">
                  <caption className="sr-only">Hoa hồng theo lệnh sửa chữa</caption>
                  <thead>
                    <tr>
                      <th scope="col">Lệnh</th>
                      <th scope="col">Loại</th>
                      <th scope="col">Số tiền</th>
                    </tr>
                  </thead>
                  <tbody>
                    {otherRows.map((commission) => (
                      <tr key={commission.id}>
                        <td className="tabular">#{commission.repairOrderId}</td>
                        <td>{commissionTypeLabels[commission.commissionType]}</td>
                        <td className="tabular">{money(commission.commissionAmount)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
            </section>
          </div>
        </div>
      )}
    </Modal>
  );
}
