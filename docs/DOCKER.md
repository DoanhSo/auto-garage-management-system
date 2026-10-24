# AutoGarage Docker - mo giao dien ngay

## Nhan bo dong goi

1. Cai va mo Docker Desktop, chon Linux containers.
2. Giai nen toan bo `AutoGarage-Docker.zip` vao mot thu muc.
3. Nhan dup `start-docker.cmd`, hoac chay:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\start-docker.ps1
```

Lan dau launcher nap images tu `images.tar`, tao secret ngau nhien, khoi tao MySQL,
Keycloak, tai khoan quan tri va du lieu mau. Khi dich vu healthy, trinh duyet tu mo.
May nhan chi can Docker Desktop va PowerShell, khong can Java, Maven, Node.js hay
tai source/dependency. Images danh cho Linux amd64.

Giao dien uu tien `http://localhost:3000`; neu cong dang dung se chon 3001, 3002...
URL chinh xac va thong tin dang nhap nam trong **`.docker-local/credentials.txt`**.
Ten dang nhap gara: **`garage.admin`**; mat khau tao rieng cho tung may, khong co
mat khau dung chung trong bundle. Keycloak Admin va Swagger cung duoc ghi o do.

Co san 3 khach hang, 3 xe, 2 dich vu va 3 lich hen mang nhan DEMO. Lan chay tiep
theo giu du lieu, tai khoan va mat khau; khong xoa/reset volume. Co the dung
`-NoDemoData` de bo qua tao du lieu mau va `-NoBrowser` de khong mo trinh duyet.

Dung `localhost` cho giao dien/Keycloak khi thu SSO. Dang nhap bang mat khau hoac
nut Keycloak va dang ky khach hang deu dung Keycloak that. Google chi hoat dong
khi duoc cau hinh OAuth rieng; mac dinh an.

## Bat/tat va xem log

Chay cac lenh trong thu muc da giai nen:

```powershell
docker compose --env-file .docker-local/.env ps
docker compose --env-file .docker-local/.env logs --tail 100 backend keycloak keycloak-setup
docker compose --env-file .docker-local/.env stop
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\start-docker.ps1
```

Khong chay `down -v` neu muon giu du lieu. Khong gui `.docker-local` cho nguoi
khac: thu muc nay chua credential va cau hinh rieng cua may. Khong doi secret
database sau khi khoi tao volume neu chua doi credential trong database.

Bo nay danh cho xem/kiem thu local: cong chi bind loopback, email/SMS/reminder
khong gui ra ngoai. Upload hien chi luu metadata/hash, chua co storage file.
Production can HTTPS, domain, backup, quan ly secret va cau hinh Keycloak rieng.

## Build lai tu source (nguoi dong goi)

Can JDK 21, Node.js 24.15, Docker Desktop va Maven wrapper/dependency.

```powershell
.\scripts\build-docker.ps1
.\scripts\start-docker.ps1
.\scripts\export-docker.ps1
```

Build chay **toan bo Maven tests tren MySQL test rieng**, npm lint/tests/build,
roi moi tao image. Dung `-OfflineMaven` neu dependencies da co trong cache.
Dockerfile dung JAR/dist da duoc kiem tra, khong can test database trong image.
`export-docker.ps1` stream ca 5 images truc tiep vao ZIP, kem compose khong can build
va cac file khoi tao, khong can mot ban tar lon trung gian. ZIP co SHA256. Bundle
khong chua database/volume, log hoac secret.

Ban 24/10/2026 da dat 354 tests backend (gom MySQL that), 79 tests frontend,
lint/build, dang ky va dang nhap SSO PKCE tren Keycloak that, logout/token revocation
va dang nhap lai. Launcher da thu bang Windows PowerShell 5.1; archive ca 5 images
da nap lai thanh cong bang Docker.

Tham chieu: [Keycloak health checks](https://www.keycloak.org/observability/health)
va [realm import](https://www.keycloak.org/server/importExport).
