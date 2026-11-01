import { Link } from 'react-router-dom';
import { GARAGE_INFO, GARAGE_STATS, HERO_IMAGE } from '../constants/homeData';
import { HomeImage } from './HomeImage';

const stats = [
  { value: GARAGE_STATS.carsServiced, label: 'Lượt xe chăm sóc và xuất xưởng' },
  { value: `${GARAGE_STATS.technicalBays} khoang`, label: 'Dịch vụ kỹ thuật chuyên biệt' },
  { value: GARAGE_STATS.genuinePartsRatio, label: 'Phụ tùng chính hãng' },
  { value: GARAGE_STATS.ratingScore, label: `Hài lòng từ hơn ${GARAGE_STATS.reviewCount} chủ xe` },
];

export function HeroSection() {
  return (
    <section className="home-hero" aria-labelledby="home-hero-title">
      <HomeImage className="home-hero-image" src={HERO_IMAGE} alt="" loading="eager" />
      <div className="home-hero-overlay" aria-hidden />
      <div className="home-container home-hero-content">
        <p className="home-eyebrow">{GARAGE_INFO.name} · Dịch vụ ô tô tiêu chuẩn</p>
        <h1 id="home-hero-title">Trung tâm bảo dưỡng và sửa chữa ô tô tiêu chuẩn hãng</h1>
        <p className="home-hero-sub">
          Chẩn đoán kỹ thuật số OBD-II · Cân chỉnh góc đặt bánh xe 3D Hunter · 100% linh kiện phụ
          tùng chính hãng · Bảo hành lên đến 36 tháng
        </p>
        <div className="home-hero-cta">
          <a className="button button-primary" href="#booking">
            Đặt lịch hẹn dịch vụ
          </a>
          <Link className="button home-button-light" to="/repair-orders">
            Tra cứu tiến độ sửa chữa
          </Link>
        </div>
        <dl className="home-stats">
          {stats.map((stat) => (
            <div key={stat.label}>
              <dt>{stat.value}</dt>
              <dd>{stat.label}</dd>
            </div>
          ))}
        </dl>
      </div>
    </section>
  );
}
