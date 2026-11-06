import { describe, expect, it } from 'vitest';
import type { Employee, ServiceBay, Shift } from '../types/scheduling';
import { filterShifts, groupShiftsByCell, shiftChipState } from './shiftTimeline';

function shift(
  partial: Partial<Shift> & Pick<Shift, 'id' | 'employeeId' | 'startsAt' | 'endsAt'>,
): Shift {
  return { status: 'ACTIVE', createdBy: 1, createdAt: '2026-11-01T00:00:00Z', ...partial };
}

const NOW = new Date('2026-11-04T09:00:00').getTime();

describe('shiftChipState', () => {
  it('phân loại theo thời điểm hiện tại', () => {
    const base = { id: 1, employeeId: 1 };
    expect(
      shiftChipState(
        shift({ ...base, startsAt: '2026-11-04T13:00:00', endsAt: '2026-11-04T17:00:00' }),
        NOW,
      ),
    ).toBe('upcoming');
    expect(
      shiftChipState(
        shift({ ...base, startsAt: '2026-11-04T08:00:00', endsAt: '2026-11-04T17:00:00' }),
        NOW,
      ),
    ).toBe('live');
    expect(
      shiftChipState(
        shift({ ...base, startsAt: '2026-11-03T08:00:00', endsAt: '2026-11-03T17:00:00' }),
        NOW,
      ),
    ).toBe('done');
  });

  it('ca đã hủy, bị từ chối và chờ duyệt không phụ thuộc giờ', () => {
    const base = {
      id: 1,
      employeeId: 1,
      startsAt: '2026-11-04T08:00:00',
      endsAt: '2026-11-04T17:00:00',
    };
    expect(shiftChipState(shift({ ...base, status: 'CANCELLED' }), NOW)).toBe('cancelled');
    expect(shiftChipState(shift({ ...base, status: 'REJECTED' }), NOW)).toBe('cancelled');
    expect(shiftChipState(shift({ ...base, status: 'PENDING' }), NOW)).toBe('pending');
  });
});

describe('groupShiftsByCell', () => {
  const days = ['2026-11-02', '2026-11-03', '2026-11-04'];

  it('gom theo kỹ thuật viên và ngày bắt đầu, sắp xếp theo giờ', () => {
    const late = shift({
      id: 2,
      employeeId: 7,
      startsAt: '2026-11-03T13:00:00',
      endsAt: '2026-11-03T17:00:00',
    });
    const early = shift({
      id: 1,
      employeeId: 7,
      startsAt: '2026-11-03T08:00:00',
      endsAt: '2026-11-03T12:00:00',
    });

    const map = groupShiftsByCell([late, early], days);

    expect(map.get('7|2026-11-03')?.map((s) => s.id)).toEqual([1, 2]);
  });

  it('ca qua nửa đêm nằm ở ngày bắt đầu và ca ngoài tuần bị bỏ', () => {
    const overnight = shift({
      id: 3,
      employeeId: 8,
      startsAt: '2026-11-04T22:00:00',
      endsAt: '2026-11-05T06:00:00',
    });
    const outside = shift({
      id: 4,
      employeeId: 8,
      startsAt: '2026-11-20T08:00:00',
      endsAt: '2026-11-20T17:00:00',
    });

    const map = groupShiftsByCell([overnight, outside], days);

    expect(map.get('8|2026-11-04')?.map((s) => s.id)).toEqual([3]);
    expect([...map.keys()]).toEqual(['8|2026-11-04']);
  });

  it('id của ca và kỹ thuật viên dạng số hay chuỗi đều cho cùng khóa', () => {
    const numeric = shift({
      id: 5,
      employeeId: 9,
      startsAt: '2026-11-02T08:00:00',
      endsAt: '2026-11-02T17:00:00',
    });
    const text = shift({
      id: '6',
      employeeId: '9',
      startsAt: '2026-11-02T13:00:00',
      endsAt: '2026-11-02T17:00:00',
    });

    expect(groupShiftsByCell([numeric, text], days).get('9|2026-11-02')).toHaveLength(2);
  });
});

describe('filterShifts', () => {
  const employees: Record<string, Employee> = {
    '1': {
      id: 1,
      userId: 11,
      employeeCode: 'NV-TECH-01',
      username: 'an.nguyen',
      skillsJson: '["ENGINE"]',
      canQc: true,
      hourlyCost: null,
      isActive: true,
      createdAt: '',
    },
    '2': {
      id: 2,
      userId: 12,
      employeeCode: 'NV-TECH-02',
      username: 'binh.tran',
      skillsJson: '["BRAKES"]',
      canQc: false,
      hourlyCost: null,
      isActive: true,
      createdAt: '',
    },
  };
  const bays: Record<string, ServiceBay> = {
    '6': {
      id: 6,
      bayCode: 'B06',
      bayName: 'Khoang rửa xe',
      bayType: 'WASH',
      isActive: true,
    } as unknown as ServiceBay,
  };
  const names: Record<string, string> = { '1': 'Nguyễn Văn An', '2': 'Trần Bình' };
  const ctx = {
    employeeOf: (id: string | number) => employees[String(id)],
    nameOf: (id: string | number) => names[String(id)] ?? `NV #${id}`,
    bayOf: (id?: string | number | null) => (id == null ? undefined : bays[String(id)]),
  };
  const morning = shift({
    id: 1,
    employeeId: 1,
    startsAt: '2026-11-04T07:30:00',
    endsAt: '2026-11-04T11:30:00',
    shiftType: 'MORNING',
    serviceBayId: 6,
    notes: 'Thay ca cho đồng nghiệp',
  });
  const night = shift({
    id: 2,
    employeeId: 2,
    startsAt: '2026-11-04T20:00:00',
    endsAt: '2026-11-04T23:59:00',
    shiftType: 'NIGHT',
  });
  const all = [morning, night];

  it('từ khóa rỗng trả lại toàn bộ', () => {
    expect(filterShifts(all, '  ', ctx)).toHaveLength(2);
  });

  it.each([
    ['nguyen van an', [1]],
    ['NV-TECH-02', [2]],
    ['binh.tran', [2]],
    ['ca sang', [1]],
    ['ca dem', [2]],
    ['b06', [1]],
    ['rua xe', [1]],
    ['dong nghiep', [1]],
    ['qc', [1]],
  ])('tìm "%s"', (term, expectedIds) => {
    expect(filterShifts(all, term, ctx).map((s) => s.id)).toEqual(expectedIds);
  });

  it('không có kết quả khi không khớp', () => {
    expect(filterShifts(all, 'khong ton tai', ctx)).toEqual([]);
  });
});
