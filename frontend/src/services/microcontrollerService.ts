import api from './api';

export interface MicrocontrollerDto {
  id: number;
  mcCode: string;
  name: string;
  online: boolean;
  uptimeSec: number;
  lastSeen: string;
  parkingSpaceId: number;
}

export interface CreateMicrocontrollerRequest {
  mcCode: string;
  name: string;
  parkingSpaceId: number;
}

class MicrocontrollerService {
  async getAllMicrocontrollers(): Promise<MicrocontrollerDto[]> {
    const response = await api.get<MicrocontrollerDto[]>('/api/microcontrollers');
    return response.data;
  }

  async getMicrocontrollerById(id: number): Promise<MicrocontrollerDto> {
    const response = await api.get<MicrocontrollerDto>(`/api/microcontrollers/${id}`);
    return response.data;
  }

  async createMicrocontroller(data: CreateMicrocontrollerRequest): Promise<MicrocontrollerDto> {
    const response = await api.post<MicrocontrollerDto>('/api/microcontrollers', data);
    return response.data;
  }

  async deleteMicrocontroller(id: number): Promise<void> {
    await api.delete(`/api/microcontrollers/${id}`);
  }
}

export default new MicrocontrollerService();

