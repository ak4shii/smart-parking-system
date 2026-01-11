import api from './api';

export interface RfidDto {
  id: number;
  rfidCode: string;
  currentlyUsed: boolean;
  parkingSpaceId: number;
}

export interface CreateRfidRequest {
  rfidCode: string;
  parkingSpaceId: number;
}

class RfidService {
  async getAllRfids(): Promise<RfidDto[]> {
    const response = await api.get<RfidDto[]>('/api/rfids');
    return response.data;
  }

  async getRfidById(id: number): Promise<RfidDto> {
    const response = await api.get<RfidDto>(`/api/rfids/${id}`);
    return response.data;
  }

  async createRfid(data: CreateRfidRequest): Promise<RfidDto> {
    const response = await api.post<RfidDto>('/api/rfids', data);
    return response.data;
  }

  async deleteRfid(id: number): Promise<void> {
    await api.delete(`/api/rfids/${id}`);
  }
}

export default new RfidService();

