import api from './api';

export interface PresignGetRequest {
  key: string;
}

export interface PresignGetResponse {
  url: string | null;
}

class S3Service {
  async presignGet(key: string): Promise<string | null> {
    if (!key) return null;
    const response = await api.post<PresignGetResponse>('/api/s3/presign-get', { key } satisfies PresignGetRequest);
    return response.data.url;
  }
}

export default new S3Service();

