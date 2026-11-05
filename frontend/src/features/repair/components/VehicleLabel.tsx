import { useVehicleQuery } from '../../vehicle/hooks/useVehicles';
import { formatPlate } from '../../../shared/utils/format';

export function VehicleLabel({
  vehicleId,
  showBrand = true,
}: {
  vehicleId: string;
  showBrand?: boolean;
}) {
  const { data } = useVehicleQuery(vehicleId);
  if (!data) return <span className="muted">Đang tải xe…</span>;
  return (
    <span>
      <span className="license-plate-badge">{formatPlate(data.licensePlate)}</span>
      {showBrand && (
        <span className="muted">
          {' · '}
          {data.brand} {data.model}
        </span>
      )}
    </span>
  );
}
