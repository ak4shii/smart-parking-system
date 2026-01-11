import api from './api';

export interface EntryLogDto {
  id: number;
  rfidId: number;
  rfidCode: string;
  licensePlate: string;
  /** S3 object key/path (or CloudFront origin path) for the plate image */
  licensePlateImageKey?: string | null;
  inTime: string;
  outTime: string | null;
  parkingSpaceId: number;
}

class EntryLogService {
  async getEntryLogById(id: number): Promise<EntryLogDto> {
    const response = await api.get<EntryLogDto>(`/api/entry-logs/${id}`);
    return response.data;
  }

  async getEntryLogsByParkingSpace(parkingSpaceId: number): Promise<EntryLogDto[]> {
    const response = await api.get<EntryLogDto[]>('/api/entry-logs', {
      params: { parkingSpaceId }
    });
    return response.data;
  }
}

export default new EntryLogService();


