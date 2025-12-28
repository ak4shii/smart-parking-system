import api from './api';

export interface SensorDto {
  id: number;
  name: string;
  type: string;
  slotId: number;
  microcontrollerId: number;
  parkingSpaceId: number;
}

export interface CreateSensorRequest {
  name: string;
  type: string;
  slotId: number;
  microcontrollerId: number;
}

class SensorService {
  async getAllSensors(): Promise<SensorDto[]> {
    const response = await api.get<SensorDto[]>('/api/sensors');
    return response.data;
  }

  async getSensorById(id: number): Promise<SensorDto> {
    const response = await api.get<SensorDto>(`/api/sensors/${id}`);
    return response.data;
  }

  async createSensor(data: CreateSensorRequest): Promise<SensorDto> {
    const response = await api.post<SensorDto>('/api/sensors', data);
    return response.data;
  }

  async deleteSensor(id: number): Promise<void> {
    await api.delete(`/api/sensors/${id}`);
  }
}

export default new SensorService();

