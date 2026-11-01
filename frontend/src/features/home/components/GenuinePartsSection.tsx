import { Check } from 'lucide-react';
import { PARTNER_BRANDS, PARTS_COMMITMENTS, PARTS_IMAGE, TESTIMONIALS } from '../constants/homeData';
import { HomeImage } from './HomeImage';

export function GenuinePartsSection() {
  return (
    <>
      <section
        id="parts"
        className="home-section home-section-alt"
        aria-labelledby="home-parts-title"
      >
        <div className="home-container home-split">
          <HomeImage className="home-split-image" src={PARTS_IMAGE} alt="Kệ phụ tùng chính hãng" />
          <div>
            <p className="home-eyebrow">Phụ tùng chính hãng</p>
            <h2 id="home-parts-title">Cam kết chất lượng từ nhà cung cấp uy tín</h2>
            <ul className="home-checklist">
              {PARTS_COMMITMENTS.map((item) => (
                <li key={item}>
                  <Check size={16} aria-hidden /> {item}
                </li>
              ))}
            </ul>
            <ul className="home-brands" aria-label="Thương hiệu đối tác">
              {PARTNER_BRANDS.map((brand) => (
                <li key={brand}>{brand}</li>
              ))}
            </ul>
          </div>
        </div>
      </section>
      <section className="home-section" aria-labelledby="home-testimonials-title">
        <div className="home-container">
          <p className="home-eyebrow">Khách hàng nói gì</p>
          <h2 id="home-testimonials-title">Đánh giá từ chủ xe</h2>
          <div className="home-grid home-grid-3">
            {TESTIMONIALS.map((item) => (
              <figure key={item.name} className="home-quote">
                <blockquote>{item.quote}</blockquote>
                <figcaption>
                  <strong>{item.name}</strong>
                  <span>{item.vehicle}</span>
                </figcaption>
              </figure>
            ))}
          </div>
        </div>
      </section>
    </>
  );
}
