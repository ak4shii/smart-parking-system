import api from './api';

export interface SlotDto {
  id: number;
  parkingSpaceId: number;
  isOccupied: boolean;
}

export interface CreateSlotRequest {
  parkingSpaceId: number;
}

class SlotService {
    
  async getAllSlots(): Promise<SlotDto[]> {
    const response = await api.get<SlotDto[]>('/api/slots');
    return response.data;
  }

  async getSlotById(id: number): Promise<SlotDto> {
    const response = await api.get<SlotDto>(`/api/slots/${id}`);
    return response.data;
  }

  async createSlot(data: CreateSlotRequest): Promise<SlotDto> {
    const response = await api.post<SlotDto>('/api/slots', data);
    return response.data;
  }

  async deleteSlot(id: number): Promise<void> {
    await api.delete(`/api/slots/${id}`);
  }
}

export default new SlotService();

