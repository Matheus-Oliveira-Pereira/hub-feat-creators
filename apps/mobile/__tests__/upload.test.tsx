import { uploadEntregavel, FileTooLargeError } from '@/lib/upload';
import * as FileSystem from 'expo-file-system';

jest.mock('expo-file-system', () => ({
  getInfoAsync: jest.fn(),
  uploadAsync: jest.fn(),
  FileSystemUploadType: { MULTIPART: 'MULTIPART' },
}));

jest.mock('@/store/auth', () => ({
  useAuthStore: {
    getState: () => ({ token: 'test-token' }),
  },
}));

const mockGetInfo = FileSystem.getInfoAsync as jest.Mock;
const mockUpload = FileSystem.uploadAsync as jest.Mock;

describe('uploadEntregavel', () => {
  beforeEach(() => jest.clearAllMocks());

  it('throws FileTooLargeError for oversized video', async () => {
    mockGetInfo.mockResolvedValue({ exists: true, size: 600 * 1024 * 1024 });
    await expect(
      uploadEntregavel('tarefa-id', 'file://video.mp4', 'video/mp4', 'video.mp4'),
    ).rejects.toBeInstanceOf(FileTooLargeError);
  });

  it('throws FileTooLargeError for oversized image', async () => {
    mockGetInfo.mockResolvedValue({ exists: true, size: 60 * 1024 * 1024 });
    await expect(
      uploadEntregavel('tarefa-id', 'file://img.jpg', 'image/jpeg', 'img.jpg'),
    ).rejects.toBeInstanceOf(FileTooLargeError);
  });

  it('returns parsed body on success', async () => {
    mockGetInfo.mockResolvedValue({ exists: true, size: 1024 });
    mockUpload.mockResolvedValue({ status: 201, body: '{"id":"ent-uuid"}' });

    const result = await uploadEntregavel('tarefa-id', 'file://img.jpg', 'image/jpeg', 'img.jpg');
    expect(result).toEqual({ id: 'ent-uuid' });
  });

  it('throws on HTTP error status', async () => {
    mockGetInfo.mockResolvedValue({ exists: true, size: 1024 });
    mockUpload.mockResolvedValue({ status: 422, body: '{"message":"TAREFA_NOT_FOUND"}' });

    await expect(
      uploadEntregavel('tarefa-id', 'file://img.jpg', 'image/jpeg', 'img.jpg'),
    ).rejects.toThrow('TAREFA_NOT_FOUND');
  });
});
