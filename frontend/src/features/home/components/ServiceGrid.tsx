import { Clock, Cog, Disc3, Gauge, PaintBucket, ScanLine, Wrench, type LucideIcon } from 'lucide-react';
import { SERVICES } from '../constants/homeData';
import { HomeImage } from './HomeImage';

const icons: Record<string, LucideIcon> = { Wrench, Gauge, Cog, ScanLine, PaintBucket, Disc3 };

export function ServiceGrid() {
  return (
    <section id="services" className="home-section" aria-labelledby="home-services-title">
      <div className="home-container">
        <p className="home-eyebrow">Dịch vụ chủ lực</p>
        <h2 id="home-services-title">Sáu nhóm dịch vụ kỹ thuật mũi nhọn</h2>
        <div id="pricing" className="home-grid home-grid-3">
          {SERVICES.map((service) => {
            const Icon = icons[service.iconName] ?? Wrench;
            return (
              <article key={service.id} className="home-card">
                <HomeImage src={service.image} alt={service.name} />
                <div className="home-card-body">
                  <span className="home-tag">
                    <Icon size={14} aria-hidden /> {service.tag}
                  </span>
                  <h3>{service.name}</h3>
                  <p>{service.description}</p>
                  <footer>
                    <strong>{service.priceDisplay}</strong>
                    <span>
                      <Clock size={14} aria-hidden /> {service.durationDisplay}
                    </span>
                  </footer>
                </div>
              </article>
            );
          })}
        </div>
      </div>
    </section>
  );
}
