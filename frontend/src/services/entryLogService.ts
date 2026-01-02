import api from './api';

export interface EntryLogDto {
  id: number;
  rfidId: number;
  rfidCode: string;
  licensePlate: string;
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

