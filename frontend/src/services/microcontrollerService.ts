import api from './api';

export interface MqttCredentials {
  mqttHost: string;
  mqttPort: number;
  mqttUsername: string;
  mqttPassword: string;  // Only shown once during creation/regeneration
  baseTopic: string;
  mcCode: string;
  deviceName: string;
}

export interface MicrocontrollerDto {
  id: number;
  mcCode: string;
  name: string;
  online: boolean;
  uptimeSec: number;
  lastSeen: string;
  parkingSpaceId: number;
  mqttUsername?: string;
  mqttEnabled?: boolean;
  // Only included when creating new device (one-time display)
  mqttCredentials?: MqttCredentials;
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

  /**
   * Regenerate MQTT credentials for a device.
   * Old credentials are immediately revoked.
   * Returns new credentials (password shown once).
   */
  async regenerateMqttCredentials(id: number): Promise<MqttCredentials> {
    const response = await api.post<MqttCredentials>(`/api/microcontrollers/${id}/mqtt/regenerate`);
    return response.data;
  }

  /**
   * Revoke MQTT access for a device without deleting it.
   * The device will no longer be able to connect to MQTT broker.
   */
  async revokeMqttCredentials(id: number): Promise<void> {
    await api.post(`/api/microcontrollers/${id}/mqtt/revoke`);
  }
}

export default new MicrocontrollerService();


