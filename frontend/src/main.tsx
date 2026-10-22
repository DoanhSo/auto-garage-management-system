import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import './styles.css';
const root = createRoot(document.getElementById('root')!);
void import('./app/App')
  .then(({ default: App }) =>
    root.render(
      <StrictMode>
        <App />
      </StrictMode>,
    ),
  )
  .catch(() =>
    root.render(
      <main className="status-page">
        <h1>Chưa thể khởi động AutoGarage</h1>
        <p>Kiểm tra cấu hình ứng dụng hoặc kết nối, sau đó tải lại trang.</p>
        <button className="button button-primary" onClick={() => window.location.reload()}>
          Tải lại
        </button>
      </main>,
    ),
  );
