import { request } from './client';

export async function downloadAttachment(id: string, filename: string): Promise<void> {
  const blob = await request<Blob>(`/attachments/${id}/content`, { binary: true });
  const url = URL.createObjectURL(blob);
  try {
    const link = document.createElement('a');
    link.href = url;
    link.download = filename;
    document.body.appendChild(link);
    link.click();
    link.remove();
  } finally {
    URL.revokeObjectURL(url);
  }
}
