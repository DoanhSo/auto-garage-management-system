import { describe, expect, it } from 'vitest';
import type { EstimateItem } from '../types/estimate';
import { buildLineDecisions, canDecideEstimate, pendingItems } from './lineDecisions';

function item(id: string | number, approvalStatus: EstimateItem['approvalStatus']): EstimateItem {
  return { id, approvalStatus } as unknown as EstimateItem;
}

describe('buildLineDecisions', () => {
  it('gửi đủ các dòng chờ khi id của dòng là số (đúng như dữ liệu máy chủ trả về)', () => {
    const items = [item(4, 'PENDING'), item(5, 'PENDING')];
    const lines = buildLineDecisions(items, { '4': 'APPROVED', '5': 'REJECTED' }, 'Khách đồng ý');

    expect(lines).toEqual([
      { estimateItemId: 4, decision: 'APPROVED', note: 'Khách đồng ý' },
      { estimateItemId: 5, decision: 'REJECTED', note: 'Khách đồng ý' },
    ]);
  });

  it('cũng đúng khi id của dòng là chuỗi', () => {
    const lines = buildLineDecisions([item('7', 'PENDING')], { '7': 'REJECTED' });

    expect(lines).toEqual([{ estimateItemId: 7, decision: 'REJECTED', note: undefined }]);
  });

  it('bỏ qua dòng đã quyết định', () => {
    const items = [item(1, 'APPROVED'), item(2, 'REJECTED'), item(3, 'PENDING')];
    const lines = buildLineDecisions(items, { '1': 'APPROVED', '2': 'REJECTED', '3': 'APPROVED' });

    expect(lines.map((l) => l.estimateItemId)).toEqual([3]);
  });

  it('không còn dòng chờ thì danh sách rỗng (báo giá đã duyệt hết như #4)', () => {
    const items = [
      item(4, 'APPROVED'),
      item(5, 'APPROVED'),
      item(6, 'APPROVED'),
      item(7, 'APPROVED'),
    ];

    expect(buildLineDecisions(items, { '4': 'APPROVED' })).toEqual([]);
    expect(pendingItems(items)).toEqual([]);
  });

  it('dòng chưa có lựa chọn mặc định là duyệt', () => {
    const lines = buildLineDecisions([item(9, 'PENDING')], {});

    expect(lines[0].decision).toBe('APPROVED');
  });
});

describe('canDecideEstimate', () => {
  it.each(['DRAFT', 'SENT', 'PARTIALLY_APPROVED', 'APPROVED'])('%s còn duyệt được', (status) => {
    expect(canDecideEstimate(status)).toBe(true);
  });

  it.each(['REJECTED', 'EXPIRED', 'CANCELLED', 'SUPERSEDED'])('%s không duyệt được', (status) => {
    expect(canDecideEstimate(status)).toBe(false);
  });
});
