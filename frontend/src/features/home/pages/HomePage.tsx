import '../../../styles/home.css';
import { HomeHeader } from '../components/HomeHeader';
import { HeroSection } from '../components/HeroSection';
import { ServiceGrid } from '../components/ServiceGrid';
import { WorkshopShowcase } from '../components/WorkshopShowcase';
import { WorkflowSteps } from '../components/WorkflowSteps';
import { GenuinePartsSection } from '../components/GenuinePartsSection';
import { BookingCtaSection } from '../components/BookingCtaSection';
import { HomeFooter } from '../components/HomeFooter';

export function HomePage() {
  return (
    <div className="home-page">
      <HomeHeader />
      <main id="main-content" tabIndex={-1}>
        <HeroSection />
        <ServiceGrid />
        <WorkflowSteps />
        <WorkshopShowcase />
        <GenuinePartsSection />
        <BookingCtaSection />
      </main>
      <HomeFooter />
    </div>
  );
}
