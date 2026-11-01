import { useEffect, useMemo } from 'react';
import { FileText, Upload, X } from 'lucide-react';

export const ATTACHMENT_ACCEPT = 'image/jpeg,image/png,image/webp,image/heic,application/pdf';
const PREVIEWABLE = new Set(['image/jpeg', 'image/png', 'image/webp']);

export function formatFileSize(bytes?: number) {
  if (bytes == null) return '';
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(0)} KB`;
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`;
}

/** Chọn nhiều ảnh/PDF, xem trước thumbnail và bỏ bớt tệp trước khi tải lên. */
export function AttachmentFilePicker({
  files,
  onChange,
  disabled,
  label = 'Chọn ảnh hoặc PDF',
}: {
  files: File[];
  onChange: (files: File[]) => void;
  disabled?: boolean;
  label?: string;
}) {
  const previews = useMemo(
    () => files.map((file) => (PREVIEWABLE.has(file.type) ? URL.createObjectURL(file) : null)),
    [files],
  );
  useEffect(() => () => previews.forEach((url) => url && URL.revokeObjectURL(url)), [previews]);

  return (
    <div className="attachment-picker">
      <label className={`button button-secondary${disabled ? ' is-disabled' : ''}`}>
        <Upload size={16} />
        <span>{label}</span>
        <input
          type="file"
          multiple
          accept={ATTACHMENT_ACCEPT}
          className="sr-only"
          disabled={disabled}
          onChange={(event) => {
            const picked = Array.from(event.target.files ?? []);
            if (picked.length) onChange([...files, ...picked]);
            event.target.value = '';
          }}
        />
      </label>
      {files.length > 0 && (
        <ul className="attachment-thumbs" aria-label="Tệp đã chọn">
          {files.map((file, index) => (
            <li key={`${file.name}-${index}`} className="attachment-thumb">
              {previews[index] ? (
                <img src={previews[index]!} alt={file.name} />
              ) : (
                <span className="attachment-thumb-icon">
                  <FileText size={22} />
                </span>
              )}
              <span className="attachment-thumb-meta">
                <span className="attachment-thumb-name">{file.name}</span>
                <span>{formatFileSize(file.size)}</span>
              </span>
              <button
                type="button"
                className="attachment-thumb-remove"
                aria-label={`Bỏ ${file.name}`}
                disabled={disabled}
                onClick={() => onChange(files.filter((_, i) => i !== index))}
              >
                <X size={14} />
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
