import { Badge, Modal } from '../../../shared/components/ui';
import { formatDate, money } from '../../../shared/utils/format';
import {
  commissionStatusLabels,
  commissionTypeLabels,
  payrollStatusLabels,
  type Payslip,
} from '../types/payroll';

const hoursText = (value: string | number) => `${Number(value).toFixed(1).replace('.', ',')} giờ`;

export function MyPayslipModal({
  payslip,
  onClose,
}: {
  payslip: Payslip | null;
  onClose: () => void;
}) {
  const item = payslip?.item;
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
        <div className="stack">
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

            <section aria-label="Thưởng theo đơn hoàn thành">
              <h3>Thưởng & hoa hồng theo đơn</h3>
              {payslip.commissions.length === 0 ? (
                <p className="description">Chưa có khoản thưởng nào trong kỳ.</p>
              ) : (
                <table className="data-table">
                  <caption className="sr-only">Hoa hồng theo lệnh sửa chữa</caption>
                  <thead>
                    <tr>
                      <th scope="col">Lệnh</th>
                      <th scope="col">Loại</th>
                      <th scope="col">Thưởng</th>
                    </tr>
                  </thead>
                  <tbody>
                    {payslip.commissions.map((commission) => (
                      <tr key={commission.id}>
                        <td className="tabular">#{commission.repairOrderId}</td>
                        <td>
                          {commissionTypeLabels[commission.commissionType]}
                          <Badge className="ml-1">
                            {commissionStatusLabels[commission.status]}
                          </Badge>
                        </td>
                        <td className="tabular">{money(commission.commissionAmount)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
            </section>
          </div>

          <dl className="summary-list">
            <div>
              <dt>Lương cứng</dt>
              <dd className="tabular">{money(item.baseSalarySnapshot)}</dd>
            </div>
            <div>
              <dt>Lương giờ công</dt>
              <dd className="tabular">{money(item.hourlyPayAmount)}</dd>
            </div>
            <div>
              <dt>Thưởng & hoa hồng</dt>
              <dd className="tabular">{money(item.commissionAmount)}</dd>
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
        </div>
      )}
    </Modal>
  );
}
