import { WORKSHOP_BAYS } from '../constants/homeData';
import { HomeImage } from './HomeImage';

export function WorkshopShowcase() {
  return (
    <section
      id="workshop"
      className="home-section home-section-alt"
      aria-labelledby="home-workshop-title"
    >
      <div className="home-container">
        <p className="home-eyebrow">Cơ sở vật chất</p>
        <h2 id="home-workshop-title">Sáu khoang kỹ thuật số hóa</h2>
        <div className="home-grid home-grid-3">
          {WORKSHOP_BAYS.map((bay) => (
            <article key={bay.bayCode} className="home-card home-bay">
              <HomeImage src={bay.image} alt={`${bay.bayCode} - ${bay.bayName}`} />
              <div className="home-card-body">
                <span className="home-tag">
                  {bay.bayCode} · {bay.bayType}
                </span>
                <h3>{bay.bayName}</h3>
                <p>{bay.specialty}</p>
                <small>{bay.equipment}</small>
              </div>
            </article>
          ))}
        </div>
      </div>
    </section>
  );
}
