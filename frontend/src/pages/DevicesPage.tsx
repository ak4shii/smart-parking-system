import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Radio,
  Search,
  LayoutDashboard,
  Car,
  KeyRound,
  DoorOpen,
  Monitor,
  ShieldCheck,
  ParkingSquare,
  LogOut,
  ChevronDown,
  Activity,
  Wifi,
  WifiOff,
} from 'lucide-react';
import toast from 'react-hot-toast';

import { useAuth } from '../context/AuthContext';
import sensorService, { type SensorDto } from '../services/sensorService';
import parkingSpaceService, { type ParkingSpaceDto } from '../services/parkingSpaceService';
import slotService, { type SlotDto } from '../services/slotService';
import microcontrollerService, { type MicrocontrollerDto } from '../services/microcontrollerService';

export default function DevicesPage() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [allSensors, setAllSensors] = useState<SensorDto[]>([]);
  const [slots, setSlots] = useState<SlotDto[]>([]);
  const [microcontrollers, setMicrocontrollers] = useState<MicrocontrollerDto[]>([]);
  const [parkingSpaces, setParkingSpaces] = useState<ParkingSpaceDto[]>([]);
  const [selectedParkingSpaceId, setSelectedParkingSpaceId] = useState<number | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');

  useEffect(() => {
    fetchData();
  }, []);

  const fetchData = async () => {
    try {
      setIsLoading(true);
      const [sensorsData, parkingSpacesData, slotsData, microcontrollersData] = await Promise.all([
        sensorService.getAllSensors(),
        parkingSpaceService.getAllParkingSpaces(),
        slotService.getAllSlots(),
        microcontrollerService.getAllMicrocontrollers(),
      ]);
      setAllSensors(sensorsData);
      setParkingSpaces(parkingSpacesData);
      setSlots(slotsData);
      setMicrocontrollers(microcontrollersData);

      // Auto-select first parking space if available
      if (parkingSpacesData.length > 0) {
        setSelectedParkingSpaceId(parkingSpacesData[0].id);
      }
    } catch (error: any) {
      console.error('Error fetching data:', error);
      toast.error('Failed to load sensors');
    } finally {
      setIsLoading(false);
    }
  };

  const getSlotLabel = (slotId: number) => {
    const slot = slots.find((s) => s.id === slotId);
    if (!slot) return `Slot #${slotId}`;

    // Get all slots for this parking space and find the index
    const parkingSpaceSlots = slots
      .filter((s) => s.parkingSpaceId === slot.parkingSpaceId)
      .sort((a, b) => a.id - b.id);

    const index = parkingSpaceSlots.findIndex((s) => s.id === slotId);
    return `S${String(index + 1).padStart(2, '0')}`;
  };

  const getMicrocontrollerName = (mcId: number) => {
    const mc = microcontrollers.find((m) => m.id === mcId);
    return mc ? mc.name : `MC-${mcId}`;
  };

  const getMicrocontrollerStatus = (mcId: number) => {
    const mc = microcontrollers.find((m) => m.id === mcId);
    return mc ? mc.online : false;
  };

  // Filter sensors by selected parking space
  const sensors = selectedParkingSpaceId
    ? allSensors.filter((sensor) => sensor.parkingSpaceId === selectedParkingSpaceId)
    : allSensors;

  // Then filter by search term
  const filteredSensors = sensors.filter(
    (sensor) =>
      sensor.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
      sensor.type.toLowerCase().includes(searchTerm.toLowerCase()) ||
      getSlotLabel(sensor.slotId).toLowerCase().includes(searchTerm.toLowerCase())
  );

  const selectedParkingSpace = parkingSpaces.find((ps) => ps.id === selectedParkingSpaceId);

  // Calculate stats
  const totalSensors = sensors.length;
  const activeSensors = sensors.filter((s) => getMicrocontrollerStatus(s.microcontrollerId)).length;
  const ultrasonicCount = sensors.filter((s) => s.type === 'ultrasonic').length;
  const infraredCount = sensors.filter((s) => s.type === 'infrared').length;

  return (
    <div className="min-h-screen bg-slate-50 text-slate-900">
      <div className="mx-auto flex min-h-screen max-w-[1400px] gap-6 p-4 sm:p-6">
        {/* Sidebar */}
        <aside className="hidden w-72 shrink-0 rounded-3xl border border-slate-200 bg-white p-5 shadow-sm lg:block">
          <div className="flex items-center gap-3">
            <div className="grid h-10 w-10 place-items-center rounded-2xl bg-indigo-50 text-indigo-700 ring-1 ring-indigo-200">
              <ParkingSquare className="h-5 w-5" />
            </div>
            <div className="flex-1">
              <div className="text-xs font-semibold tracking-widest text-slate-500">SMART CAR PARKING</div>
              <div className="text-sm font-semibold text-slate-900">Devices</div>
            </div>
          </div>

          {/* Current Parking Space */}
          {selectedParkingSpace && (
            <div className="mt-4 rounded-2xl border border-indigo-200 bg-indigo-50 p-3">
              <div className="text-xs font-semibold text-indigo-600">Viewing</div>
              <div className="mt-1 text-sm font-bold text-indigo-900">{selectedParkingSpace.name}</div>
              <div className="mt-0.5 text-xs text-indigo-600">{selectedParkingSpace.location}</div>
            </div>
          )}

          <div className="mt-7 space-y-1">
            <button
              onClick={() => navigate('/')}
              className="flex w-full items-center gap-3 rounded-2xl px-3 py-2.5 text-sm text-slate-600 hover:bg-slate-100"
            >
              <LayoutDashboard className="h-4 w-4" />
              <span>Dashboard</span>
            </button>
            <div className="flex items-center gap-3 rounded-2xl bg-slate-100 px-3 py-2.5 text-sm ring-1 ring-slate-200">
              <Radio className="h-4 w-4 text-slate-600" />
              <span className="text-slate-900">Devices</span>
            </div>
            <button
              onClick={() => navigate('/entry-logs')}
              className="flex w-full items-center gap-3 rounded-2xl px-3 py-2.5 text-sm text-slate-600 hover:bg-slate-100"
            >
              <Car className="h-4 w-4" />
              <span>Entry Logs</span>
            </button>
            <button
              onClick={() => navigate('/rfid')}
              className="flex w-full items-center gap-3 rounded-2xl px-3 py-2.5 text-sm text-slate-600 hover:bg-slate-100"
            >
              <KeyRound className="h-4 w-4" />
              <span>RFID</span>
            </button>
            <button
              onClick={() => navigate('/doors')}
              className="flex w-full items-center gap-3 rounded-2xl px-3 py-2.5 text-sm text-slate-600 hover:bg-slate-100"
            >
              <DoorOpen className="h-4 w-4" />
              <span>Doors</span>
            </button>
            <button
              onClick={() => navigate('/lcds')}
              className="flex w-full items-center gap-3 rounded-2xl px-3 py-2.5 text-sm text-slate-600 hover:bg-slate-100"
            >
              <Monitor className="h-4 w-4" />
              <span>LCDs</span>
            </button>
            <button
              onClick={() => navigate('/admin')}
              className="flex w-full items-center gap-3 rounded-2xl px-3 py-2.5 text-sm text-slate-600 hover:bg-slate-100"
            >
              <ShieldCheck className="h-4 w-4" />
              <span>Admin</span>
            </button>
          </div>

          <div className="mt-8 rounded-2xl border border-slate-200 bg-slate-50 p-4">
            <div className="text-xs font-semibold text-slate-500">Signed in as</div>
            <div className="mt-2 text-sm font-semibold text-slate-900">{user?.username ?? '—'}</div>
            <div className="mt-1 text-xs text-slate-500">{user?.email ?? ''}</div>

            <button
              onClick={logout}
              className="mt-4 inline-flex w-full items-center justify-center gap-2 rounded-xl bg-rose-600 px-3 py-2 text-sm font-semibold text-white hover:bg-rose-700"
            >
              <LogOut className="h-4 w-4" />
              Logout
            </button>
          </div>
        </aside>

        {/* Main Content */}
        <main className="flex-1">
          {/* Header */}
          <div className="rounded-3xl border border-slate-200 bg-white px-5 py-4 shadow-sm">
            <div className="flex flex-wrap items-center justify-between gap-4">
              <div>
                <div className="text-xs font-semibold tracking-widest text-slate-500">SMART CAR PARKING SYSTEM</div>
                <h1 className="mt-1 text-2xl font-bold text-slate-900">Devices & Sensors</h1>
              </div>

              <button
                onClick={logout}
                className="inline-flex items-center gap-2 rounded-2xl bg-slate-900 px-3 py-2 text-sm font-semibold text-white hover:bg-slate-800 lg:hidden"
              >
                <LogOut className="h-4 w-4" /> Logout
              </button>
            </div>

            {/* Parking Space Selector */}
            {parkingSpaces.length > 0 && (
              <div className="mt-4 flex items-center gap-3">
                <label className="text-sm font-semibold text-slate-700">Parking Space:</label>
                <div className="relative">
                  <select
                    value={selectedParkingSpaceId || ''}
                    onChange={(e) => setSelectedParkingSpaceId(Number(e.target.value))}
                    className="appearance-none rounded-xl border border-slate-200 bg-white pl-4 pr-10 py-2 text-sm font-medium text-slate-900 outline-none focus:border-indigo-300 focus:ring-2 focus:ring-indigo-100"
                  >
                    {parkingSpaces.map((space) => (
                      <option key={space.id} value={space.id}>
                        {space.name} - {space.location}
                      </option>
                    ))}
                  </select>
                  <ChevronDown className="pointer-events-none absolute right-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
                </div>
                <div className="text-xs text-slate-500">
                  ({parkingSpaces.length} space{parkingSpaces.length !== 1 ? 's' : ''} available)
                </div>
              </div>
            )}
          </div>

          {/* Stats */}
          <div className="mt-6 grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
            <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
              <div className="flex items-start justify-between">
                <div>
                  <div className="text-xs font-semibold text-slate-500">Total Sensors</div>
                  <div className="mt-2 text-3xl font-bold text-slate-900">{totalSensors}</div>
                  <div className="mt-1 text-xs text-slate-500">
                    {selectedParkingSpace ? selectedParkingSpace.name : 'All spaces'}
                  </div>
                </div>
                <div className="grid h-12 w-12 place-items-center rounded-2xl bg-indigo-50 text-indigo-700 ring-1 ring-indigo-200">
                  <Radio className="h-6 w-6" />
                </div>
              </div>
            </div>

            <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
              <div className="flex items-start justify-between">
                <div>
                  <div className="text-xs font-semibold text-slate-500">Active Sensors</div>
                  <div className="mt-2 text-3xl font-bold text-emerald-600">{activeSensors}</div>
                  <div className="mt-1 text-xs text-slate-500">Online & reporting</div>
                </div>
                <div className="grid h-12 w-12 place-items-center rounded-2xl bg-emerald-50 text-emerald-700 ring-1 ring-emerald-200">
                  <Activity className="h-6 w-6" />
                </div>
              </div>
            </div>

            <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
              <div className="flex items-start justify-between">
                <div>
                  <div className="text-xs font-semibold text-slate-500">Ultrasonic</div>
                  <div className="mt-2 text-3xl font-bold text-blue-600">{ultrasonicCount}</div>
                  <div className="mt-1 text-xs text-slate-500">Distance sensors</div>
                </div>
                <div className="grid h-12 w-12 place-items-center rounded-2xl bg-blue-50 text-blue-700 ring-1 ring-blue-200">
                  <Radio className="h-6 w-6" />
                </div>
              </div>
            </div>

            <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
              <div className="flex items-start justify-between">
                <div>
                  <div className="text-xs font-semibold text-slate-500">Infrared</div>
                  <div className="mt-2 text-3xl font-bold text-purple-600">{infraredCount}</div>
                  <div className="mt-1 text-xs text-slate-500">Proximity sensors</div>
                </div>
                <div className="grid h-12 w-12 place-items-center rounded-2xl bg-purple-50 text-purple-700 ring-1 ring-purple-200">
                  <Radio className="h-6 w-6" />
                </div>
              </div>
            </div>
          </div>

          {/* Sensors Table */}
          <div className="mt-6 rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
            <div className="flex flex-wrap items-center justify-between gap-4 mb-5">
              <h2 className="text-lg font-semibold text-slate-900">Sensor Devices</h2>

              <div className="relative">
                <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
                <input
                  type="text"
                  placeholder="Search sensors..."
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                  className="rounded-xl border border-slate-200 bg-slate-50 py-2 pl-9 pr-3 text-sm outline-none focus:border-indigo-300 focus:ring-2 focus:ring-indigo-100"
                />
              </div>
            </div>

            {isLoading ? (
              <div className="flex items-center justify-center py-12">
                <div className="text-slate-500">Loading sensors...</div>
              </div>
            ) : parkingSpaces.length === 0 ? (
              <div className="flex flex-col items-center justify-center py-12">
                <ParkingSquare className="h-12 w-12 text-slate-300" />
                <div className="mt-3 text-slate-900 font-semibold">No Parking Spaces</div>
                <div className="mt-1 text-sm text-slate-500">Create a parking space to view sensors</div>
              </div>
            ) : filteredSensors.length === 0 ? (
              <div className="flex flex-col items-center justify-center py-12">
                <Radio className="h-12 w-12 text-slate-300" />
                <div className="mt-3 text-slate-900 font-semibold">
                  {searchTerm ? 'No sensors found' : 'No sensors yet'}
                </div>
                <div className="mt-1 text-sm text-slate-500">
                  {searchTerm
                    ? 'Try adjusting your search terms'
                    : selectedParkingSpace
                      ? `${selectedParkingSpace.name} has no sensors yet`
                      : 'Create parking slots to add sensors'}
                </div>
              </div>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full">
                  <thead>
                    <tr className="border-b border-slate-200">
                      <th className="pb-3 text-left text-xs font-semibold text-slate-500">ID</th>
                      <th className="pb-3 text-left text-xs font-semibold text-slate-500">Sensor Name</th>
                      <th className="pb-3 text-left text-xs font-semibold text-slate-500">Type</th>
                      <th className="pb-3 text-left text-xs font-semibold text-slate-500">Slot</th>
                      <th className="pb-3 text-left text-xs font-semibold text-slate-500">Microcontroller</th>
                      <th className="pb-3 text-left text-xs font-semibold text-slate-500">Status</th>
                    </tr>
                  </thead>
                  <tbody>
                    {filteredSensors.map((sensor) => {
                      const isOnline = getMicrocontrollerStatus(sensor.microcontrollerId);
                      return (
                        <tr key={sensor.id} className="border-b border-slate-100 hover:bg-slate-50">
                          <td className="py-4 text-sm text-slate-600">#{sensor.id}</td>
                          <td className="py-4">
                            <div className="flex items-center gap-2">
                              <Radio className="h-4 w-4 text-indigo-600" />
                              <span className="text-sm font-semibold text-slate-900">{sensor.name}</span>
                            </div>
                          </td>
                          <td className="py-4">
                            <span
                              className={`inline-flex items-center rounded-full px-2.5 py-1 text-xs font-semibold ${sensor.type === 'ultrasonic'
                                  ? 'bg-blue-100 text-blue-700'
                                  : 'bg-purple-100 text-purple-700'
                                }`}
                            >
                              {sensor.type}
                            </span>
                          </td>
                          <td className="py-4 text-sm text-slate-600">{getSlotLabel(sensor.slotId)}</td>
                          <td className="py-4 text-sm text-slate-600">
                            {getMicrocontrollerName(sensor.microcontrollerId)}
                          </td>
                          <td className="py-4">
                            {isOnline ? (
                              <span className="inline-flex items-center gap-1.5 rounded-full bg-emerald-100 px-2.5 py-1 text-xs font-semibold text-emerald-700">
                                <Wifi className="h-3 w-3" />
                                Online
                              </span>
                            ) : (
                              <span className="inline-flex items-center gap-1.5 rounded-full bg-slate-100 px-2.5 py-1 text-xs font-semibold text-slate-600">
                                <WifiOff className="h-3 w-3" />
                                Offline
                              </span>
                            )}
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </main>
      </div>
    </div>
  );
}

