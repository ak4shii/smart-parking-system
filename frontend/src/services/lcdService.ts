import api from './api';

export interface LcdDto {
  id: number;
  name: string;
  displayText: string;
  microcontrollerId: number;
  parkingSpaceId?: number;
  parkingSpaceName?: string;
}

export interface CreateLcdRequest {
  name: string;
  displayText?: string;
  microcontrollerId: number;
}

export type UpdateLcdRequest = Partial<CreateLcdRequest>;

class LcdService {
  async getAllLcds(): Promise<LcdDto[]> {
    const res = await api.get<LcdDto[]>('/api/lcds');
    return res.data;
  }

  async createLcd(data: CreateLcdRequest): Promise<LcdDto> {
    const res = await api.post<LcdDto>('/api/lcds', data);
    return res.data;
  }

  async updateLcd(id: number, data: UpdateLcdRequest): Promise<LcdDto> {
    const res = await api.put<LcdDto>(`/api/lcds/${id}`, data);
    return res.data;
  }

  async deleteLcd(id: number): Promise<void> {
    await api.delete(`/api/lcds/${id}`);
  }
}

export default new LcdService();

