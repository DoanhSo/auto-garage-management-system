import { expect, test, type APIRequestContext, type Page } from '@playwright/test';
import { randomUUID } from 'node:crypto';
import { mkdir, readFile } from 'node:fs/promises';
import AxeBuilder from '@axe-core/playwright';

// Run only against a disposable database: the suite creates users and WEB notices.
const api = process.env.E2E_API_URL ?? 'http://127.0.0.1:55818/api/v1';
const identifier = process.env.E2E_IDENTIFIER;
const password = process.env.E2E_PASSWORD;
const allowed = process.env.E2E_ALLOW_MUTATIONS === 'true';
const suffix = randomUUID().replaceAll('-', '').slice(0, 10);
const memberPassword = randomUUID() + 'A!';
const roleNames = ['ADMIN', 'RECEPTIONIST', 'WAREHOUSE_STAFF', 'TECHNICIAN', 'CUSTOMER'] as const;
const users = new Map<string, { id: number; username: string; fullName: string }>();
let adminToken: string;
let noticeId: number;
const errors: string[] = [];

async function authenticate(request: APIRequestContext, name: string, secret: string) {
  const response = await request.post(api + '/auth/login', {
    data: { identifier: name, password: secret },
  });
  expect(response.status()).toBe(200);
  return (await response.json()).data as {
    accessToken: string;
    user: { id: number };
  };
}
async function signIn(page: Page, name = identifier!, secret = password!) {
  await page.goto('/login');
  await page.getByLabel('Tên đăng nhập hoặc email', { exact: false }).fill(name);
  await page.getByLabel('Mật khẩu *', { exact: true }).fill(secret);
  await page.getByRole('button', { name: 'Đăng nhập', exact: true }).click();
  await expect(page.getByRole('button', { name: 'Đăng xuất' })).toBeVisible();
}
test.beforeAll(async ({ request }) => {
  if (!allowed || !identifier || !password)
    throw new Error(
      'Set E2E_ALLOW_MUTATIONS=true and isolated E2E credentials to run browser integration.',
    );
  adminToken = (await authenticate(request, identifier, password)).accessToken;
  for (const role of roleNames) {
    const username = `e2e.${role.toLowerCase()}.${suffix}`;
    const fullName = `TV1 ${role} ${suffix}`;
    const result = await request.post(api + '/admin/users', {
      headers: { Authorization: `Bearer ${adminToken}` },
      data: { username, fullName, role, password: memberPassword, email: null },
    });
    expect(result.status()).toBe(201);
    users.set(role, (await result.json()).data);
  }
  const me = (await authenticate(request, identifier, password)).user;
  const notice = await request.post(api + '/notifications', {
    headers: { Authorization: `Bearer ${adminToken}` },
    data: {
      userId: me.id,
      eventKey: `e2e.web.${suffix}`,
      channel: 'WEB',
      destination: `user:${me.id}`,
      subject: `Nhắc lịch ${suffix}`,
      payloadJson: JSON.stringify({
        message: 'Xe của bạn đã sẵn sàng. <b>Vui lòng kiểm tra.</b>',
      }),
    },
  });
  expect(notice.status()).toBe(200);
  noticeId = (await notice.json()).data.id;
});
test.beforeEach(async ({ page }) => {
  errors.length = 0;
  page.on('pageerror', (error) => errors.push(error.message));
});
test.afterEach(() => expect(errors).toEqual([]));

test('admin creates, edits, clears email, locks and reactivates through the drawer', async ({
  page,
}) => {
  await signIn(page);
  await page.getByRole('link', { name: 'Tài khoản', exact: true }).click();
  await page.getByRole('button', { name: 'Thêm tài khoản', exact: true }).click();
  const drawer = page.getByRole('dialog');
  await drawer.getByRole('button', { name: 'Thêm tài khoản', exact: true }).click();
  await expect(drawer.getByText('Nhập họ và tên.')).toBeVisible();
  const name = `Nhân viên kiểm thử ${suffix}`;
  await drawer.getByLabel('Họ và tên', { exact: false }).fill(name);
  await drawer.getByLabel('Tên đăng nhập', { exact: false }).fill(`e2e.created.${suffix}`);
  await drawer.getByLabel('Email', { exact: false }).fill(`created.${suffix}@example.test`);
  await drawer.getByLabel('Vai trò', { exact: false }).selectOption('RECEPTIONIST');
  await drawer.getByLabel('Mật khẩu *', { exact: true }).fill(memberPassword);
  await drawer.getByLabel('Xác nhận mật khẩu', { exact: false }).fill(memberPassword);
  await drawer.getByRole('button', { name: 'Thêm tài khoản', exact: true }).click();
  await expect(drawer).toBeHidden();
  await page.getByLabel('Tìm tài khoản', { exact: true }).fill(`e2e.created.${suffix}`);
  const row = page.getByRole('row').filter({ hasText: name });
  await expect(row).toBeVisible();
  await row.getByRole('button', { name: `Sửa tài khoản ${name}` }).click();
  await expect(drawer.getByLabel('Họ và tên', { exact: false })).toHaveValue(name);
  await drawer.getByLabel('Email', { exact: false }).fill('');
  await drawer.getByRole('button', { name: 'Lưu thay đổi' }).click();
  await expect(drawer).toBeHidden();
  await expect(row.getByText('Chưa cung cấp')).toBeVisible();
  await row.getByRole('button', { name: `Khóa tài khoản ${name}` }).click();
  await page.getByRole('alertdialog').getByRole('button', { name: 'Khóa tài khoản' }).click();
  await expect(row.getByText('Đã khóa')).toBeVisible();
  await row.getByRole('button', { name: `Mở khóa tài khoản ${name}` }).click();
  await page
    .getByRole('alertdialog')
    .getByRole('button', { name: 'Mở khóa tài khoản', exact: true })
    .click();
  await expect(row.getByText('Hoạt động', { exact: true })).toBeVisible();
  await row.getByRole('button', { name: `Xóa tài khoản ${name}` }).click();
  await page
    .getByRole('alertdialog')
    .getByRole('button', { name: 'Xóa tài khoản', exact: true })
    .click();
  await expect(row).toHaveCount(0);
  await mkdir('output/playwright', { recursive: true });
  await page.getByLabel('Tìm tài khoản', { exact: true }).fill(suffix);
  await expect(
    page.getByRole('button', {
      name: `Sửa tài khoản ${users.get('ADMIN')!.fullName}`,
    }),
  ).toBeVisible();
  await page.screenshot({
    path: 'output/playwright/accounts-desktop.png',
    fullPage: true,
    animations: 'disabled',
  });
});
test('reports use valid dates, reject reversed periods and download real CSV', async ({ page }) => {
  await signIn(page);
  await expect(page.getByRole('heading', { name: 'Kết quả trong kỳ' })).toBeVisible();
  await expect(page.getByRole('heading', { name: 'Số dư tại thời điểm hiện tại' })).toBeVisible();
  const downloadPromise = page.waitForEvent('download');
  await page.getByRole('button', { name: 'Xuất CSV' }).click();
  const download = await downloadPromise;
  expect(download.suggestedFilename()).toMatch(/^bao-cao-gara-\d{4}-\d{2}-\d{2}\.csv$/);
  expect((await readFile((await download.path())!, 'utf8')).replace(/^\uFEFF/, '')).toContain(
    'revenue',
  );
  await page.getByLabel('Từ ngày', { exact: false }).fill('2026-10-15');
  await page.getByLabel('Đến hết ngày', { exact: false }).fill('2026-10-01');
  await expect(page.getByRole('button', { name: 'Áp dụng' })).toBeDisabled();
  await expect(page.getByRole('alert')).toBeVisible();
  await mkdir('output/playwright', { recursive: true });
  await page.screenshot({
    path: 'output/playwright/reports-desktop.png',
    fullPage: true,
    animations: 'disabled',
  });
});
test('audit shows server events and readonly details', async ({ page }) => {
  await signIn(page);
  await page.getByRole('link', { name: 'Nhật ký', exact: true }).click();
  await expect(page.getByRole('heading', { name: 'Nhật ký hoạt động' })).toBeVisible();
  await page
    .getByRole('button', { name: /Xem sự kiện/ })
    .first()
    .click();
  const detail = page.getByRole('dialog');
  await expect(detail.getByRole('heading', { name: 'Chi tiết sự kiện' })).toBeVisible();
  await expect(detail.getByRole('button', { name: 'Lưu thay đổi' })).toHaveCount(0);
  await page.keyboard.press('Escape');
  await expect(detail).toBeHidden();
});
test('own WEB notification renders text and persists the read marker', async ({
  page,
  request,
}) => {
  await signIn(page);
  await page
    .getByRole('navigation', { name: 'Điều hướng chính' })
    .getByRole('link', { name: /^Thông báo/ })
    .click();
  await page.getByRole('button', { name: new RegExp(`Nhắc lịch ${suffix}`) }).click();
  await expect(
    page
      .getByText('Xe của bạn đã sẵn sàng. <b>Vui lòng kiểm tra.</b>', {
        exact: true,
      })
      .last(),
  ).toBeVisible();
  await expect(page.getByText('Đã đọc', { exact: true })).toBeVisible();
  const result = await request.get(`${api}/notifications/${noticeId}`, {
    headers: { Authorization: `Bearer ${adminToken}` },
  });
  expect((await result.json()).data.readAt).toBeTruthy();
});
for (const role of roleNames.filter((role) => role !== 'ADMIN')) {
  test(`${role} has a private inbox and cannot open admin UI or API`, async ({ page, request }) => {
    const user = users.get(role)!;
    await signIn(page, user.username, memberPassword);
    await expect(page.getByRole('heading', { name: 'Thông báo', exact: true })).toBeVisible();
    await expect(page.getByRole('link', { name: 'Tài khoản', exact: true })).toHaveCount(0);
    await expect(page.getByRole('button', { name: new RegExp(`Nhắc lịch ${suffix}`) })).toHaveCount(
      0,
    );
    const privateSession = await authenticate(request, user.username, memberPassword);
    const forbidden = await request.get(api + '/admin/users', {
      headers: { Authorization: `Bearer ${privateSession.accessToken}` },
    });
    expect(forbidden.status()).toBe(403);
    const hidden = await request.get(`${api}/notifications/${noticeId}`, {
      headers: { Authorization: `Bearer ${privateSession.accessToken}` },
    });
    expect(hidden.status()).toBe(403);
    await page.getByRole('link', { name: 'Tài khoản của tôi' }).click();
    await page.goto('/admin/users');
    await expect(page.getByRole('heading', { name: 'Đăng nhập' })).toBeVisible();
    await page.getByLabel('Tên đăng nhập hoặc email', { exact: false }).fill(user.username);
    await page.getByLabel('Mật khẩu *', { exact: true }).fill(memberPassword);
    await page.getByRole('button', { name: 'Đăng nhập', exact: true }).click();
    await expect(page.getByRole('heading', { name: 'Bạn chưa có quyền truy cập' })).toBeVisible();
  });
}
test('changing password requires a new login and invalidates existing tokens', async ({
  page,
  request,
}) => {
  const user = users.get('TECHNICIAN')!;
  const previous = await authenticate(request, user.username, memberPassword);
  await signIn(page, user.username, memberPassword);
  await page.getByRole('link', { name: 'Tài khoản của tôi' }).click();
  await page.getByLabel('Mật khẩu hiện tại', { exact: false }).fill(memberPassword);
  const next = randomUUID() + 'B!';
  await page.getByLabel('Mật khẩu mới *', { exact: true }).fill(next);
  await page.getByLabel('Xác nhận mật khẩu mới', { exact: false }).fill(next);
  await page.getByRole('button', { name: 'Đổi mật khẩu', exact: true }).click();
  await expect(page.getByRole('heading', { name: 'Đăng nhập' })).toBeVisible();
  expect(
    (
      await request.get(api + '/auth/me', {
        headers: { Authorization: `Bearer ${previous.accessToken}` },
      })
    ).status(),
  ).toBe(401);
  await signIn(page, user.username, next);
});
test('reload clears the memory session and logout revokes the token', async ({ page }) => {
  let loginToken = '';
  page.on('response', async (response) => {
    if (response.url().endsWith('/auth/login') && response.status() === 200)
      loginToken = (await response.json()).data.accessToken;
  });
  await signIn(page);
  await page.reload();
  await expect(page.getByRole('heading', { name: 'Đăng nhập' })).toBeVisible();
  expect(await page.evaluate(() => [localStorage.length, sessionStorage.length])).toEqual([0, 0]);
  await signIn(page);
  await page.getByRole('button', { name: 'Đăng xuất' }).click();
  await expect(page.getByRole('heading', { name: 'Đăng nhập' })).toBeVisible();
  const response = await page.request.get(api + '/auth/me', {
    headers: { Authorization: `Bearer ${loginToken}` },
  });
  expect(response.status()).toBe(401);
});
test('mobile navigation and full width form remain usable at 390px', async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 });
  await signIn(page);
  await page.getByRole('button', { name: 'Mở menu' }).click();
  await page.getByRole('link', { name: 'Tài khoản', exact: true }).click();
  await page.getByRole('button', { name: 'Thêm tài khoản', exact: true }).click();
  const drawer = page.getByRole('dialog');
  await expect(drawer.getByLabel('Họ và tên', { exact: false })).toBeFocused();
  const bounds = await drawer.boundingBox();
  expect(Math.round(bounds!.width)).toBeLessThanOrEqual(390);
  await expect(drawer.getByRole('button', { name: 'Thêm tài khoản', exact: true })).toBeVisible();
  await mkdir('output/playwright', { recursive: true });
  await page.screenshot({
    path: 'output/playwright/accounts-mobile.png',
    fullPage: false,
    animations: 'disabled',
  });
  await page.keyboard.press('Escape');
  await expect(drawer).toBeHidden();
  await expect(page.getByRole('button', { name: 'Thêm tài khoản', exact: true })).toBeFocused();
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
});
test('key screens pass automated WCAG checks and fit target viewports', async ({ page }) => {
  const check = async () => {
    const result = await new AxeBuilder({ page })
      .withTags(['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa', 'wcag22aa'])
      .analyze();
    expect(
      result.violations.map((issue) => ({
        rule: issue.id,
        targets: issue.nodes.map((node) => node.target),
      })),
    ).toEqual([]);
  };
  await page.goto('/login');
  await check();
  await signIn(page);
  await expect(page.getByRole('heading', { name: 'Kết quả trong kỳ' })).toBeVisible();
  await check();
  await page.getByRole('link', { name: 'Tài khoản', exact: true }).click();
  await expect(page.getByRole('button', { name: 'Thêm tài khoản', exact: true })).toBeVisible();
  await check();
  await page.getByRole('button', { name: 'Thêm tài khoản', exact: true }).click();
  await check();
  await page.keyboard.press('Escape');
  for (const width of [320, 360, 768, 1024, 1440]) {
    await page.setViewportSize({ width, height: 900 });
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(
      true,
    );
    await page.getByRole('button', { name: 'Thêm tài khoản', exact: true }).click();
    await expect(
      page.getByRole('dialog').getByRole('button', { name: 'Thêm tài khoản', exact: true }),
    ).toBeVisible();
    await page.keyboard.press('Escape');
  }
  await page.setViewportSize({ width: 1440, height: 1000 });
  await page
    .getByRole('navigation', { name: 'Điều hướng chính' })
    .getByRole('link', { name: /^Thông báo/ })
    .click();
  await check();
  await page.getByRole('link', { name: 'Tài khoản của tôi' }).click();
  await check();
});
test('an actual concurrent update returns a conflict and preserves the UI draft', async ({
  page,
  request,
}) => {
  adminToken = (await authenticate(request, identifier!, password!)).accessToken;
  const member = users.get('CUSTOMER')!;
  await signIn(page);
  await page.getByRole('link', { name: 'Tài khoản', exact: true }).click();
  await page.getByLabel('Tìm tài khoản', { exact: true }).fill(member.username);
  await page.getByRole('button', { name: `Sửa tài khoản ${member.fullName}` }).click();
  const drawer = page.getByRole('dialog');
  const field = drawer.getByLabel('Họ và tên', { exact: false });
  await expect(field).toHaveValue(member.fullName);
  await field.fill('Bản nháp chưa lưu');
  const latest = await request.get(`${api}/admin/users/${member.id}`, {
    headers: { Authorization: `Bearer ${adminToken}` },
  });
  const current = (await latest.json()).data;
  const changed = await request.put(`${api}/admin/users/${member.id}`, {
    headers: { Authorization: `Bearer ${adminToken}` },
    data: {
      email: null,
      fullName: 'Phiên khác đã cập nhật',
      role: current.role,
      active: true,
      version: current.version,
    },
  });
  expect(changed.status()).toBe(200);
  await drawer.getByRole('button', { name: 'Lưu thay đổi' }).click();
  await expect(drawer.getByText('Dữ liệu đã thay đổi', { exact: true })).toBeVisible();
  await expect(field).toHaveValue('Bản nháp chưa lưu');
  await drawer.getByRole('button', { name: 'Xem bản mới trên máy chủ' }).click();
  await expect(drawer.getByText('Phiên khác đã cập nhật', { exact: true })).toBeVisible();
  await expect(field).toHaveValue('Bản nháp chưa lưu');
  await drawer.getByRole('button', { name: 'Dùng bản mới' }).click();
  await expect(field).toHaveValue('Phiên khác đã cập nhật');
  await page.keyboard.press('Escape');
  await expect(drawer).toBeHidden();
});
test('self demotion confirms, ends the session and revokes the old token', async ({
  page,
  request,
}) => {
  const administrator = users.get('ADMIN')!;
  const session = await authenticate(request, administrator.username, memberPassword);
  await signIn(page, administrator.username, memberPassword);
  await page.getByRole('link', { name: 'Tài khoản', exact: true }).click();
  await page.getByLabel('Tìm tài khoản', { exact: true }).fill(administrator.username);
  await page.getByRole('button', { name: `Sửa tài khoản ${administrator.fullName}` }).click();
  const drawer = page.getByRole('dialog');
  await expect(drawer.getByLabel('Vai trò', { exact: false })).toHaveValue('ADMIN');
  await drawer.getByLabel('Vai trò', { exact: false }).selectOption('RECEPTIONIST');
  await drawer.getByRole('button', { name: 'Lưu thay đổi' }).click();
  await expect(page.getByRole('alertdialog')).toBeVisible();
  await page.getByRole('alertdialog').getByRole('button', { name: 'Lưu và đăng nhập lại' }).click();
  await expect(page.getByRole('heading', { name: 'Đăng nhập', exact: true })).toBeVisible();
  expect(
    (
      await request.get(api + '/auth/me', {
        headers: { Authorization: `Bearer ${session.accessToken}` },
      })
    ).status(),
  ).toBe(401);
  await signIn(page, administrator.username, memberPassword);
  await expect(
    page.getByRole('heading', { name: 'Bạn chưa có quyền truy cập', exact: true }),
  ).toBeVisible();
});
test('authentication infrastructure 503 keeps the active session and offers retry', async ({
  page,
}) => {
  await page.route('**/api/v1/auth/me', (route) =>
    route.fulfill({
      status: 503,
      contentType: 'application/json',
      body: JSON.stringify({
        code: 4002,
        success: false,
        message: 'Temporarily unavailable',
        data: null,
        timestamp: '2026-10-15T00:00:00Z',
        path: '/api/v1/auth/me',
      }),
    }),
  );
  await signIn(page);
  await expect(
    page.getByText('Chưa thể kiểm tra lại thông tin tài khoản.', { exact: false }),
  ).toBeVisible();
  await expect(page.getByRole('button', { name: 'Đăng xuất' })).toBeVisible();
  await page.getByRole('link', { name: 'Tài khoản', exact: true }).click();
  await expect(page.getByRole('heading', { name: 'Tài khoản', exact: true })).toBeVisible();
  await page.unroute('**/api/v1/auth/me');
  await page.getByRole('button', { name: 'Thử lại', exact: true }).click();
  await expect(
    page.getByText('Chưa thể kiểm tra lại thông tin tài khoản.', { exact: false }),
  ).toBeHidden();
});
test('account add edit and delete dialogs are centered and fit one viewport', async ({ page }) => {
  await signIn(page);
  await page.getByRole('link', { name: 'Tài khoản', exact: true }).click();
  for (const viewport of [
    { width: 1366, height: 768 },
    { width: 1280, height: 720 },
    { width: 1024, height: 600 },
    { width: 390, height: 844 },
    { width: 430, height: 429 },
    { width: 320, height: 740 },
  ]) {
    await page.setViewportSize(viewport);
    await page.getByRole('button', { name: 'Thêm tài khoản', exact: true }).click();
    const dialog = page.getByRole('dialog');
    await expect(dialog.getByLabel('Họ và tên', { exact: false })).toBeFocused();
    const box = (await dialog.boundingBox())!;
    expect(Math.abs(box.x + box.width / 2 - viewport.width / 2)).toBeLessThan(1);
    expect(Math.abs(box.y + box.height / 2 - viewport.height / 2)).toBeLessThan(1);
    for (const field of ['fullName', 'username', 'email', 'role', 'password', 'confirmPassword']) {
      const control = dialog.locator(`[name="${field}"]`);
      await expect(control).toBeVisible();
      const bounds = (await control.boundingBox())!;
      expect(bounds.y).toBeGreaterThanOrEqual(0);
      expect(bounds.y + bounds.height).toBeLessThanOrEqual(viewport.height);
    }
    expect(
      await dialog.evaluate((element) => element.scrollHeight <= element.clientHeight + 1),
    ).toBe(true);
    if (viewport.width === 1366) {
      await dialog.getByRole('button', { name: 'Thêm tài khoản', exact: true }).click();
      await expect(dialog.getByText('Nhập họ và tên.', { exact: true })).toBeVisible();
      expect(
        await dialog.evaluate((element) => element.scrollHeight <= element.clientHeight + 1),
      ).toBe(true);
      await mkdir('output/playwright', { recursive: true });
      await page.screenshot({
        path: 'output/playwright/accounts-centered-add.png',
        animations: 'disabled',
      });
    }
    await page.keyboard.press('Escape');
    await expect(dialog).toBeHidden();
  }
  await page.setViewportSize({ width: 1366, height: 768 });
  const member = users.get('RECEPTIONIST')!;
  await page.getByLabel('Tìm tài khoản', { exact: true }).fill(member.username);
  await page.getByRole('button', { name: `Sửa tài khoản ${member.fullName}` }).click();
  const edit = page.getByRole('dialog');
  await expect(edit.getByRole('heading', { name: 'Sửa tài khoản' })).toBeVisible();
  await expect(edit.getByLabel('Cho phép tài khoản hoạt động')).toBeVisible();
  expect(await edit.evaluate((element) => element.scrollHeight <= element.clientHeight + 1)).toBe(
    true,
  );
  await page.screenshot({
    path: 'output/playwright/accounts-centered-edit.png',
    animations: 'disabled',
  });
  await page.keyboard.press('Escape');
  await page.getByRole('button', { name: `Xóa tài khoản ${member.fullName}` }).click();
  const confirm = page.getByRole('alertdialog');
  await expect(confirm.getByText(/khỏi danh sách quản trị/)).toBeVisible();
  const box = (await confirm.boundingBox())!;
  expect(Math.abs(box.x + box.width / 2 - 683)).toBeLessThan(1);
  await page.screenshot({
    path: 'output/playwright/accounts-centered-delete.png',
    animations: 'disabled',
  });
  await confirm.getByRole('button', { name: 'Hủy', exact: true }).click();
});
test('account filters show their state and pagination resets predictably', async ({
  page,
  request,
}) => {
  const session = await authenticate(request, identifier!, password!);
  const member = users.get('RECEPTIONIST')!;
  const response = await request.get(`${api}/admin/users/${member.id}`, {
    headers: { Authorization: `Bearer ${session.accessToken}` },
  });
  expect(response.status()).toBe(200);
  const current = (await response.json()).data;
  expect(
    (
      await request.put(`${api}/admin/users/${member.id}`, {
        headers: { Authorization: `Bearer ${session.accessToken}` },
        data: {
          fullName: current.fullName,
          email: current.email,
          role: current.role,
          active: false,
          version: current.version,
        },
      })
    ).status(),
  ).toBe(200);
  await signIn(page);
  await page.getByRole('link', { name: 'Tài khoản', exact: true }).click();
  await page.getByLabel('Số dòng mỗi trang').selectOption('10');
  await expect(page.locator('.pagination-summary')).toContainText('1–10');
  await page.getByRole('button', { name: 'Trang 2', exact: true }).click();
  await expect(page.locator('.pagination-summary')).toContainText('11–20');
  await expect(page.getByRole('button', { name: 'Trang 2', exact: true })).toHaveAttribute(
    'aria-current',
    'page',
  );
  await page.getByLabel('Tìm tài khoản', { exact: true }).fill(suffix);
  await page.getByRole('button', { name: 'Trạng thái tài khoản: Đã khóa', exact: true }).click();
  await expect(page).toHaveURL(/active=false/);
  await expect(page.getByRole('button', { name: 'Bỏ lọc từ khóa' })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Bỏ lọc trạng thái' })).toBeVisible();
  await expect(
    page.getByRole('button', { name: `Mở khóa tài khoản ${member.fullName}` }),
  ).toBeVisible();
  await expect(page.locator('.pagination-summary')).toContainText('1–1');
  await page.getByRole('button', { name: `Mở khóa tài khoản ${member.fullName}` }).click();
  await page
    .getByRole('alertdialog')
    .getByRole('button', { name: 'Mở khóa tài khoản', exact: true })
    .click();
  await expect(page.getByRole('heading', { name: 'Không có kết quả', exact: true })).toBeVisible();
  await page.getByRole('button', { name: 'Bỏ lọc trạng thái' }).click();
  await expect(
    page.getByRole('button', { name: `Khóa tài khoản ${member.fullName}` }),
  ).toBeVisible();
  const sortedResponse = page.waitForResponse(
    (response) =>
      response.url().includes('/api/v1/admin/users?') &&
      new URL(response.url()).searchParams.get('sortBy') === 'fullName' &&
      response.status() === 200,
  );
  await page.getByLabel('Sắp xếp theo', { exact: true }).selectOption('fullName:asc');
  await sortedResponse;
  await expect(page).toHaveURL(/sortBy=fullName/);
  await expect(page.locator('.pagination-summary')).toContainText('1–5');
  await expect(page.locator('.table-panel .loading-state')).toHaveCount(0);
  await expect(
    page.getByRole('button', { name: `Sửa tài khoản ${member.fullName}` }),
  ).toBeVisible();
  await mkdir('output/playwright', { recursive: true });
  await page.screenshot({
    path: 'output/playwright/accounts-filters.png',
    animations: 'disabled',
    fullPage: true,
  });
  await page.getByRole('button', { name: 'Đặt lại', exact: true }).click();
  await expect(page.getByLabel('Tìm tài khoản', { exact: true })).toHaveValue('');
  await expect(page.getByRole('button', { name: 'Trạng thái tài khoản: Tất cả' })).toHaveAttribute(
    'aria-pressed',
    'true',
  );
  await expect(page.getByLabel('Sắp xếp theo')).toHaveValue('id:asc');
  await expect(page.getByRole('button', { name: 'Bỏ lọc từ khóa' })).toHaveCount(0);
  await page.setViewportSize({ width: 320, height: 740 });
  await expect(page.getByRole('button', { name: 'Trạng thái tài khoản: Đã khóa' })).toBeVisible();
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
});
