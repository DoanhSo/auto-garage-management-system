import { describe, expect, it } from 'vitest';
import { AUDIT_SEARCH_ACTIONS_LIMIT, auditActionCodesMatching, auditCatalog } from './auditCatalog';

describe('auditActionCodesMatching', () => {
  it('từ khóa rỗng không trả mã nào', () => {
    expect(auditActionCodesMatching('')).toEqual([]);
    expect(auditActionCodesMatching('   ')).toEqual([]);
  });

  it('khớp nhãn hoặc mô tả tiếng Việt, có hoặc không dấu, và chỉ trả mã có trong từ điển', () => {
    const [code, meta] = Object.entries(auditCatalog)[0];
    const withAccents = auditActionCodesMatching(meta.label);
    const withoutAccents = auditActionCodesMatching(
      meta.label.normalize('NFD').replace(/\p{M}/gu, '').replace(/đ/gi, 'd').toUpperCase(),
    );

    expect(withAccents).toContain(code);
    expect(withoutAccents).toContain(code);
    withAccents.forEach((match) => expect(auditCatalog[match]).toBeDefined());
  });

  it('không khớp gì thì rỗng', () => {
    expect(auditActionCodesMatching('zzzz-khong-co-hanh-dong-nay')).toEqual([]);
  });

  it('không vượt giới hạn mã gửi lên máy chủ', () => {
    expect(auditActionCodesMatching('a', 3).length).toBeLessThanOrEqual(3);
    expect(auditActionCodesMatching('a').length).toBeLessThanOrEqual(AUDIT_SEARCH_ACTIONS_LIMIT);
  });
});
