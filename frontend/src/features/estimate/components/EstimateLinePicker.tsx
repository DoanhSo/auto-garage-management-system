import { useMemo } from 'react';
import { useServicesQuery } from '../../catalog/hooks/useCatalog';
import { usePartsQuery } from '../../inventory/hooks/useInventory';
import type { ItemType } from '../types/estimateTypes';

export interface PickedCatalogLine {
  serviceId: number | null;
  partId: number | null;
  itemName: string;
  unit: string;
  unitPrice: number;
}

/** Chọn dịch vụ hoặc phụ tùng từ danh mục và tự điền tên, đơn vị, đơn giá cho dòng báo giá. */
export function EstimateLinePicker({
  itemType,
  serviceId,
  partId,
  disabled,
  onPick,
}: {
  itemType: ItemType;
  serviceId?: string | number | null;
  partId?: string | number | null;
  disabled?: boolean;
  onPick: (line: PickedCatalogLine) => void;
}) {
  const services = useServicesQuery({ isActive: true });
  const parts = usePartsQuery({ size: 100, active: true });
  const isService = itemType === 'SERVICE';
  const loading = isService ? services.isLoading : parts.isLoading;

  const options = useMemo(
    () =>
      isService
        ? (services.data ?? []).map((service) => ({
            id: service.id,
            label: `${service.serviceCode} · ${service.serviceName}`,
            line: {
              serviceId: Number(service.id),
              partId: null,
              itemName: service.serviceName,
              unit: service.unit,
              unitPrice: Number(service.unitPrice),
            },
          }))
        : (parts.data?.content ?? []).map((part) => ({
            id: part.id,
            label: `${part.sku} · ${part.partName}`,
            line: {
              serviceId: null,
              partId: Number(part.id),
              itemName: part.partName,
              unit: part.unit,
              unitPrice: Number(part.sellingPrice),
            },
          })),
    [isService, services.data, parts.data],
  );

  const selected = String((isService ? serviceId : partId) ?? '');

  return (
    <select
      className="select"
      value={selected}
      disabled={disabled}
      aria-label={isService ? 'Chọn dịch vụ' : 'Chọn phụ tùng'}
      onChange={(event) => {
        const option = options.find((item) => item.id === event.target.value);
        if (option) onPick(option.line);
      }}
    >
      <option value="">
        {loading ? 'Đang tải danh mục…' : isService ? 'Chọn dịch vụ' : 'Chọn phụ tùng'}
      </option>
      {options.map((option) => (
        <option key={option.id} value={option.id}>
          {option.label}
        </option>
      ))}
    </select>
  );
}
