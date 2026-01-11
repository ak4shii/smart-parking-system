import api from './api';

export interface DoorDto {
  id: number;
  name: string;
  isOpened: boolean;
  microcontrollerId: number;
  parkingSpaceId?: number;
  parkingSpaceName?: string;
}

export interface CreateDoorRequest {
  name: string;
  isOpened: boolean;
  microcontrollerId: number;
}

export type UpdateDoorRequest = Partial<CreateDoorRequest>;

class DoorService {
  async getAllDoors(): Promise<DoorDto[]> {
    const res = await api.get<DoorDto[]>('/api/doors');
    return res.data;
  }

  async createDoor(data: CreateDoorRequest): Promise<DoorDto> {
    const res = await api.post<DoorDto>('/api/doors', data);
    return res.data;
  }

  async updateDoor(id: number, data: UpdateDoorRequest): Promise<DoorDto> {
    const res = await api.put<DoorDto>(`/api/doors/${id}`, data);
    return res.data;
  }

  async deleteDoor(id: number): Promise<void> {
    await api.delete(`/api/doors/${id}`);
  }
}

export default new DoorService();

