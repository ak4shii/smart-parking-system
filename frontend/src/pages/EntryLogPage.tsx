import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Car,
  LayoutDashboard,
  KeyRound,
  Radio,
  ShieldCheck,
  ParkingSquare,
  LogOut,
  DoorOpen,
  Monitor,
  ChevronDown,
  Clock,
  LogIn,
  LogOut as LogOutIcon,
  Calendar,
  Camera, // Import Camera icon
} from 'lucide-react';
import toast from 'react-hot-toast';

import { useAuth } from '../context/AuthContext';
import entryLogService, { type EntryLogDto } from '../services/entryLogService';
import parkingSpaceService, { type ParkingSpaceDto } from '../services/parkingSpaceService';
import s3Service from '../services/s3Service'; // Import S3 service
import { useWebSocket } from '../services/websocket';

export default function EntryLogPage() {
  const { user, logout } = useAuth();
  const { subscribe } = useWebSocket();
  const navigate = useNavigate();
  const [entryLogs, setEntryLogs] = useState<EntryLogDto[]>([]);
  const [parkingSpaces, setParkingSpaces] = useState<ParkingSpaceDto[]>([]);
  const [selectedParkingSpaceId, setSelectedParkingSpaceId] = useState<number | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    fetchParkingSpaces();
  }, []);

  useEffect(() => {
    if (selectedParkingSpaceId) {
      fetchEntryLogs(selectedParkingSpaceId);
    }
  }, [selectedParkingSpaceId]);

  // Subscribe to real-time entry log updates
  useEffect(() => {
    if (!selectedParkingSpaceId) return;

    const unsubscribe = subscribe('/topic/entrylog_new_events', (event: any) => {
      console.log('[WebSocket] Entry log event received:', event);
      // Only process events for the currently selected parking space
      if (event?.type === 'entrylog_event' && event.parkingSpaceId === selectedParkingSpaceId) {
        // Refresh the entry logs when a new event occurs
        fetchEntryLogs(selectedParkingSpaceId);

        // Show toast notification
        const action = event.action === 'vehicle_entered' ? 'entered' : 'exited';
        toast.success(`Vehicle ${event.licensePlate} ${action}`);
      }
    });

    return () => {
      unsubscribe?.();
    };
  }, [subscribe, selectedParkingSpaceId]);

  const fetchParkingSpaces = async () => {
    try {
      const data = await parkingSpaceService.getAllParkingSpaces();
      setParkingSpaces(data);
      if (data.length > 0) {
        setSelectedParkingSpaceId(data[0].id);
      }
    } catch (error) {
      console.error('Error fetching parking spaces:', error);
      toast.error('Failed to load parking spaces');
    }
  };

  const fetchEntryLogs = async (parkingSpaceId: number) => {
    try {
      setIsLoading(true);
      const data = await entryLogService.getEntryLogsByParkingSpace(parkingSpaceId);
      setEntryLogs(data);
    } catch (error) {
      console.error('Error fetching entry logs:', error);
      toast.error('Failed to load entry logs');
      setEntryLogs([]);
    } finally {
      setIsLoading(false);
    }
  };

  const formatDateTime = (dateString: string | null) => {
    if (!dateString) return '—';
    const date = new Date(dateString);
    return date.toLocaleString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  const calculateDuration = (inTime: string, outTime: string | null) => {
    if (!outTime) return 'Still inside';
    const start = new Date(inTime);
    const end = new Date(outTime);
    const diff = end.getTime() - start.getTime();
    const hours = Math.floor(diff / (1000 * 60 * 60));
    const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60));
    return `${hours}h ${minutes}m`;
  };

  const handleViewLicensePlateImage = async (log: EntryLogDto) => {
    const key = log.licensePlateImageKey;
    if (!key) {
      toast.error('No license plate image available for this log');
      return;
    }

    try {
      const url = await s3Service.presignGet(key);
      if (!url) {
        toast.error('Could not generate image URL');
        return;
      }
      window.open(url, '_blank', 'noopener,noreferrer');
    } catch (error) {
      console.error('Error getting S3 image URL:', error);
      toast.error('Failed to open license plate image');
    }
  };

  const selectedParkingSpace = parkingSpaces.find(ps => ps.id === selectedParkingSpaceId);

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
              <div className="text-sm font-semibold text-slate-900">Entry Logs</div>
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
            <button
              onClick={() => navigate('/devices')}
              className="flex w-full items-center gap-3 rounded-2xl px-3 py-2.5 text-sm text-slate-600 hover:bg-slate-100"
            >
              <Radio className="h-4 w-4" />
              <span>Devices</span>
            </button>
            <div className="flex items-center gap-3 rounded-2xl bg-slate-100 px-3 py-2.5 text-sm ring-1 ring-slate-200">
              <Car className="h-4 w-4 text-slate-600" />
              <span className="text-slate-900">Entry Logs</span>
            </div>
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
          {/* Topbar */}
          <div className="rounded-3xl border border-slate-200 bg-white px-5 py-4 shadow-sm">
            <div className="flex flex-wrap items-center justify-between gap-4">
              <div>
                <div className="text-xs font-semibold tracking-widest text-slate-500">ENTRY & EXIT TRACKING</div>
                <h1 className="mt-1 text-2xl font-bold text-slate-900">Entry Logs</h1>
              </div>

              <div className="flex items-center gap-3">
                <div className="hidden rounded-2xl border border-slate-200 bg-slate-50 px-3 py-2 text-sm text-slate-600 sm:block">
                  {new Date().toLocaleString()}
                </div>

                <button
                  onClick={logout}
                  className="inline-flex items-center gap-2 rounded-2xl bg-slate-900 px-3 py-2 text-sm font-semibold text-white hover:bg-slate-800 lg:hidden"
                >
                  <LogOut className="h-4 w-4" /> Logout
                </button>
              </div>
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
              </div>
            )}
          </div>

          {/* Entry Logs Table */}
          <div className="mt-6 rounded-3xl border border-slate-200 bg-white shadow-sm">
            <div className="p-5">
              <div className="flex items-center justify-between">
                <h2 className="text-lg font-semibold text-slate-900">
                  {selectedParkingSpace ? `${selectedParkingSpace.name} - Entry Logs` : 'Entry Logs'}
                </h2>
                <div className="flex items-center gap-2 text-sm text-slate-600">
                  <Calendar className="h-4 w-4" />
                  <span>{entryLogs.length} record{entryLogs.length !== 1 ? 's' : ''}</span>
                </div>
              </div>

              {isLoading ? (
                <div className="mt-6 flex items-center justify-center py-12">
                  <div className="text-slate-500">Loading entry logs...</div>
                </div>
              ) : entryLogs.length === 0 ? (
                <div className="mt-6 flex flex-col items-center justify-center py-12">
                  <Car className="h-12 w-12 text-slate-300" />
                  <div className="mt-3 text-slate-900 font-semibold">No Entry Logs</div>
                  <div className="mt-1 text-sm text-slate-500">No vehicles have entered or exited yet</div>
                </div>
              ) : (
                <div className="mt-6 overflow-x-auto">
                  <table className="w-full">
                    <thead>
                      <tr className="border-b border-slate-200">
                        <th className="pb-3 text-left text-xs font-semibold text-slate-600">ID</th>
                        <th className="pb-3 text-left text-xs font-semibold text-slate-600">License Plate</th>
                        <th className="pb-3 text-left text-xs font-semibold text-slate-600">Plate Image</th>
                        <th className="pb-3 text-left text-xs font-semibold text-slate-600">RFID Code</th>
                        <th className="pb-3 text-left text-xs font-semibold text-slate-600">Entry Time</th>
                        <th className="pb-3 text-left text-xs font-semibold text-slate-600">Exit Time</th>
                        <th className="pb-3 text-left text-xs font-semibold text-slate-600">Duration</th>
                        <th className="pb-3 text-left text-xs font-semibold text-slate-600">Status</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100">
                      {entryLogs
                        .sort((a, b) => new Date(b.inTime).getTime() - new Date(a.inTime).getTime())
                        .map((log) => (
                          <tr key={log.id} className="hover:bg-slate-50">
                            <td className="py-3 text-sm text-slate-600">#{log.id}</td>
                            <td className="py-3">
                              <div className="flex items-center gap-2">
                                <div className="grid h-8 w-8 place-items-center rounded-lg bg-indigo-50 text-indigo-600">
                                  <Car className="h-4 w-4" />
                                </div>
                                <span className="font-medium text-slate-900">{log.licensePlate}</span>
                              </div>
                            </td>
                            <td className="py-3">
                              {log.licensePlateImageKey ? (
                                <button
                                  type="button"
                                  onClick={() => handleViewLicensePlateImage(log)}
                                  className="inline-flex items-center gap-2 rounded-lg border border-slate-200 bg-white px-3 py-1.5 text-sm font-semibold text-slate-700 hover:bg-slate-50"
                                  title="Open license plate image"
                                >
                                  <Camera className="h-4 w-4 text-slate-600" />
                                  View
                                </button>
                              ) : (
                                <span className="text-sm text-slate-400">—</span>
                              )}
                            </td>
                            <td className="py-3 text-sm text-slate-600">{log.rfidCode}</td>
                            <td className="py-3">
                              <div className="flex items-center gap-2 text-sm text-slate-600">
                                <LogIn className="h-3.5 w-3.5 text-emerald-600" />
                                {formatDateTime(log.inTime)}
                              </div>
                            </td>
                            <td className="py-3">
                              <div className="flex items-center gap-2 text-sm text-slate-600">
                                {log.outTime ? (
                                  <>
                                    <LogOutIcon className="h-3.5 w-3.5 text-rose-600" />
                                    {formatDateTime(log.outTime)}
                                  </>
                                ) : (
                                  <span className="text-slate-400">—</span>
                                )}
                              </div>
                            </td>
                            <td className="py-3">
                              <div className="flex items-center gap-2 text-sm text-slate-600">
                                <Clock className="h-3.5 w-3.5" />
                                {calculateDuration(log.inTime, log.outTime)}
                              </div>
                            </td>
                            <td className="py-3">
                              {log.outTime ? (
                                <span className="inline-flex items-center gap-1.5 rounded-full bg-slate-100 px-2.5 py-1 text-xs font-medium text-slate-700">
                                  Completed
                                </span>
                              ) : (
                                <span className="inline-flex items-center gap-1.5 rounded-full bg-emerald-100 px-2.5 py-1 text-xs font-medium text-emerald-700">
                                  <span className="h-1.5 w-1.5 rounded-full bg-emerald-600"></span>
                                  Inside
                                </span>
                              )}
                            </td>
                          </tr>
                        ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          </div>

          <div className="mt-6 pb-8 text-xs text-slate-500">Prototype UI • Connected to live database</div>
        </main>
      </div>
    </div>
  );
}


