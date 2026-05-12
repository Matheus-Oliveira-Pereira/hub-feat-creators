import * as FileSystem from 'expo-file-system';
import { useAuthStore } from '@/store/auth';

const API_URL = process.env.EXPO_PUBLIC_API_URL ?? 'http://localhost:8080';

// Retry delays: 1min, 5min, 30min, 2h
const RETRY_DELAYS_MS = [60_000, 300_000, 1_800_000, 7_200_000];
const MAX_FILE_SIZE_VIDEO = 500 * 1024 * 1024; // 500MB
const MAX_FILE_SIZE_IMAGE = 50 * 1024 * 1024;  // 50MB

export interface UploadProgress {
  loaded: number;
  total: number;
  percent: number;
}

export class FileTooLargeError extends Error {
  constructor(sizeBytes: number, limitBytes: number) {
    super(`Arquivo ${Math.round(sizeBytes / 1024 / 1024)}MB excede limite de ${Math.round(limitBytes / 1024 / 1024)}MB`);
  }
}

export async function uploadEntregavel(
  tarefaId: string,
  fileUri: string,
  mimeType: string,
  filename: string,
  onProgress?: (p: UploadProgress) => void,
): Promise<{ id: string }> {
  const info = await FileSystem.getInfoAsync(fileUri, { size: true });
  if (!info.exists || !('size' in info)) throw new Error('Arquivo não encontrado');

  const isVideo = mimeType.startsWith('video/');
  const limit = isVideo ? MAX_FILE_SIZE_VIDEO : MAX_FILE_SIZE_IMAGE;
  if (info.size > limit) throw new FileTooLargeError(info.size, limit);

  const token = useAuthStore.getState().token;

  return withRetry(async () => {
    const result = await FileSystem.uploadAsync(
      `${API_URL}/api/v1/portal/me/tarefas/${tarefaId}/entregaveis`,
      fileUri,
      {
        httpMethod: 'POST',
        uploadType: FileSystem.FileSystemUploadType.MULTIPART,
        fieldName: 'file',
        mimeType,
        headers: {
          Authorization: `Bearer ${token ?? ''}`,
        },
        parameters: { filename },
      },
    );
    if (result.status >= 400) {
      const err = JSON.parse(result.body);
      throw new Error(err.message ?? `HTTP ${result.status}`);
    }
    return JSON.parse(result.body);
  });
}

async function withRetry<T>(fn: () => Promise<T>, attempt = 0): Promise<T> {
  try {
    return await fn();
  } catch (err) {
    if (attempt >= RETRY_DELAYS_MS.length) throw err;
    await sleep(RETRY_DELAYS_MS[attempt]);
    return withRetry(fn, attempt + 1);
  }
}

function sleep(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms));
}
