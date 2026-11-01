import { useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  CalendarCheck,
  CheckCircle2,
  Clock,
  MapPin,
  Phone,
  ShieldCheck,
  Sparkles,
} from 'lucide-react';
import { Button, Field } from '../../../shared/components/ui';
import { GARAGE_INFO, SERVICES } from '../constants/homeData';

export function BookingCtaSection() {
  const navigate = useNavigate();
  const [serviceId, setServiceId] = useState('');

  // Đặt lịch cần tài khoản: RequireAuth chuyển khách chưa đăng nhập sang /login rồi quay lại màn Lịch hẹn.
  const submit = (event: FormEvent) => {
    event.preventDefault();
    navigate('/reception');
  };

  const phoneDisplay =
    GARAGE_INFO.hotline === GARAGE_INFO.mobile
      ? GARAGE_INFO.hotline
      : `${GARAGE_INFO.hotline} · ${GARAGE_INFO.mobile}`;

  return (
    <section
      id="booking"
      className="home-section home-section-alt"
      aria-labelledby="home-booking-title"
    >
      <div id="contact" className="home-container home-split">
        <div className="home-booking-info">
          <div>
            <p className="home-eyebrow">Liên hệ & Đặt hẹn trực tuyến</p>
            <h2 id="home-booking-title">Đặt lịch hẹn nhanh tại xưởng</h2>
            <p className="home-booking-desc">
              Xếp chỗ khoang kỹ thuật ưu tiên, không phải chờ đợi. Cố vấn dịch vụ sẽ gọi điện xác
              nhận lịch hẹn và chuẩn bị trước vật tư thay thế cho xe của quý khách.
            </p>
          </div>

          <div className="home-contact-cards">
            <div className="home-contact-card">
              <span className="home-contact-icon">
                <MapPin size={20} aria-hidden />
              </span>
              <div className="home-contact-text">
                <strong>Địa chỉ xưởng dịch vụ</strong>
                <p>{GARAGE_INFO.address}</p>
              </div>
            </div>

            <div className="home-contact-card">
              <span className="home-contact-icon">
                <Clock size={20} aria-hidden />
              </span>
              <div className="home-contact-text">
                <strong>Thời gian làm việc</strong>
                <p>{GARAGE_INFO.openingHours}</p>
              </div>
            </div>

            <div className="home-contact-card">
              <span className="home-contact-icon">
                <Phone size={20} aria-hidden />
              </span>
              <div className="home-contact-text">
                <strong>Hotline tư vấn & Cứu hộ 24/7</strong>
                <p>
                  <a href={`tel:${GARAGE_INFO.hotline.replace(/\s/g, '')}`}>{phoneDisplay}</a>
                </p>
              </div>
            </div>
          </div>

          <div className="home-booking-badges">
            <span className="home-booking-badge">
              <CheckCircle2 size={15} aria-hidden /> Tiếp nhận xe ngay, không chờ
            </span>
            <span className="home-booking-badge">
              <ShieldCheck size={15} aria-hidden /> Báo giá minh bạch trước khi làm
            </span>
            <span className="home-booking-badge">
              <Sparkles size={15} aria-hidden /> Miễn phí rửa xe & hút bụi
            </span>
          </div>
        </div>

        <div className="home-form-card">
          <header className="home-form-header">
            <h3>Phiếu hẹn dịch vụ kỹ thuật</h3>
            <p>Điền thông tin để được cố vấn liên hệ tư vấn và giữ khoang sửa chữa.</p>
          </header>

          <form className="home-form" onSubmit={submit}>
            <Field label="Họ và tên" required>
              <input
                className="input"
                name="fullName"
                placeholder="vd: Nguyễn Văn Hùng"
                autoComplete="name"
                required
              />
            </Field>

            <Field label="Số điện thoại" required>
              <input
                className="input"
                name="phone"
                type="tel"
                placeholder="vd: 0963 943 872"
                autoComplete="tel"
                required
              />
            </Field>

            <Field label="Biển số xe" hint="Không bắt buộc">
              <input
                className="input"
                name="plate"
                placeholder="vd: 30H-888.68"
                autoCapitalize="characters"
              />
            </Field>

            <Field label="Ngày hẹn mong muốn">
              <input
                className="input"
                name="date"
                type="date"
                defaultValue={new Date().toISOString().split('T')[0]}
              />
            </Field>

            <div className="home-form-full">
              <Field label="Dịch vụ cần làm">
                <select
                  className="input"
                  name="service"
                  value={serviceId}
                  onChange={(e) => setServiceId(e.target.value)}
                >
                  <option value="">Khám và chẩn đoán tổng quát xe</option>
                  {SERVICES.map((service) => (
                    <option key={service.id} value={service.id}>
                      {service.name} ({service.priceDisplay})
                    </option>
                  ))}
                </select>
              </Field>
            </div>

            <div className="home-form-full">
              <Button
                type="submit"
                variant="primary"
                style={{ width: '100%', justifyContent: 'center' }}
              >
                <CalendarCheck size={18} aria-hidden />
                <span>Tiếp tục đặt lịch hẹn</span>
              </Button>
              <p className="home-form-footer-note">
                🛡️ Miễn phí kiểm tra 56 hạng mục · Cố vấn dịch vụ liên hệ xác nhận trong vòng 10 phút
              </p>
            </div>
          </form>
        </div>
      </div>
    </section>
  );
}
