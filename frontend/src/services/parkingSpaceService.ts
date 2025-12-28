import api from './api';

export interface ParkingSpaceDto {
  id: number;
  name: string;
  location: string;
  owner: string;
}

export interface CreateParkingSpaceRequest {
  name: string;
  location: string;
}

export interface UpdateParkingSpaceRequest {
  name: string;
  location: string;
}

class ParkingSpaceService {
  async getAllParkingSpaces(): Promise<ParkingSpaceDto[]> {
    const response = await api.get<ParkingSpaceDto[]>('/api/parking-spaces');
    return response.data;
  }

  async getParkingSpaceById(id: number): Promise<ParkingSpaceDto> {
    const response = await api.get<ParkingSpaceDto>(`/api/parking-spaces/${id}`);
    return response.data;
  }

  async createParkingSpace(data: CreateParkingSpaceRequest): Promise<ParkingSpaceDto> {
    const response = await api.post<ParkingSpaceDto>('/api/parking-spaces', data);
    return response.data;
  }

  async updateParkingSpace(id: number, data: UpdateParkingSpaceRequest): Promise<ParkingSpaceDto> {
    const response = await api.put<ParkingSpaceDto>(`/api/parking-spaces/${id}`, data);
    return response.data;
  }

  async deleteParkingSpace(id: number): Promise<void> {
    await api.delete(`/api/parking-spaces/${id}`);
  }
}

export default new ParkingSpaceService();

