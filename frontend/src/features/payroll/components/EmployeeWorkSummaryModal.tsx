import { Badge, Loading, Modal, QueryError } from '../../../shared/components/ui';
import { MetricGrid } from '../../../shared/components/MetricGrid';
import { formatDateTime } from '../../../shared/utils/format';
import { assignmentStageLabels, workResultLabels } from '../../vehicle/types/workHistory';
import { useEmployeeWorkSummaryQuery } from '../hooks/usePayroll';

export function EmployeeWorkSummaryModal({
  employeeId,
  employeeName,
  month,
  onClose,
}: {
  employeeId: string | null;
  employeeName?: string | null;
  month: string;
  onClose: () => void;
}) {
  const query = useEmployeeWorkSummaryQuery(employeeId, month);
  const summary = query.data;

  return (
    <Modal
      open={!!employeeId}
      size="xl"
      title={`Xe và ca đã thực hiện · ${employeeName ?? ''}`}
      description={`Tháng ${month}: đối soát giờ thi công và số lệnh đạt QC`}
      onOpenChange={(open) => !open && onClose()}
    >
      {query.isLoading && <Loading />}
      {query.isError && <QueryError error={query.error} retry={() => void query.refetch()} />}
      {summary && (
        <div className="stack">
          <MetricGrid
            label="Tổng hợp công việc"
            items={[
              {
                label: 'Tổng giờ thi công',
                value: `${(summary.totalMinutes / 60).toFixed(1).replace('.', ',')} giờ`,
              },
              { label: 'Số lệnh tham gia', value: summary.orderCount },
              { label: 'Lệnh hoàn thành đạt', value: summary.passedOrderCount, tone: 'success' },
            ]}
          />
          {summary.entries.length === 0 ? (
            <p className="description">Không có công đoạn nào trong tháng này.</p>
          ) : (
            <div className="table-scroll">
              <table className="data-table">
                <caption className="sr-only">Công đoạn của kỹ thuật viên trong tháng</caption>
                <thead>
                  <tr>
                    <th scope="col">Bắt đầu</th>
                    <th scope="col">Xe / Lệnh</th>
                    <th scope="col">Công đoạn</th>
                    <th scope="col">Thời lượng</th>
                    <th scope="col">Kết quả</th>
                  </tr>
                </thead>
                <tbody>
                  {summary.entries.map((entry) => (
                    <tr key={entry.id}>
                      <td>{formatDateTime(entry.startedAt)}</td>
                      <td>
                        <strong>{entry.licensePlate ?? '—'}</strong>
                        <div className="description">#{entry.orderCode ?? entry.repairOrderId}</div>
                      </td>
                      <td>
                        {assignmentStageLabels[entry.stage]}
                        <div className="description">{entry.taskName}</div>
                      </td>
                      <td className="tabular">{entry.actualMinutes} phút</td>
                      <td>
                        <Badge
                          variant={entry.resultStatus === 'REWORK_FAILED' ? 'danger' : 'secondary'}
                        >
                          {workResultLabels[entry.resultStatus]}
                        </Badge>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}
    </Modal>
  );
}
