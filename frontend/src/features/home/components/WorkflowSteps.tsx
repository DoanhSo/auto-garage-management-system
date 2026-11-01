import { WORKFLOW_STEPS } from '../constants/homeData';

export function WorkflowSteps() {
  return (
    <section id="workflow" className="home-section" aria-labelledby="home-workflow-title">
      <div className="home-container">
        <p className="home-eyebrow">Quy trình minh bạch</p>
        <h2 id="home-workflow-title">Năm bước từ tiếp nhận đến xuất xưởng</h2>
        <ol className="home-steps">
          {WORKFLOW_STEPS.map((step, index) => (
            <li key={step.title}>
              <span className="home-step-number">{index + 1}</span>
              <h3>{step.title}</h3>
              <p>{step.detail}</p>
            </li>
          ))}
        </ol>
      </div>
    </section>
  );
}
