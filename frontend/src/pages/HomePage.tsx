import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Car, Gauge, KeyRound, LayoutDashboard, LogOut, ParkingSquare, Radio, ShieldCheck, ChevronDown, DoorOpen, Monitor } from 'lucide-react';
import toast from 'react-hot-toast';

import OccupancyGrid from '../components/home/OccupancyGrid';
import StatCard from '../components/home/StatCard';

import { useAuth } from '../context/AuthContext';
import slotService, { type SlotDto } from '../services/slotService';
import parkingSpaceService, { type ParkingSpaceDto } from '../services/parkingSpaceService';
import { useWebSocket } from '../services/websocket';

interface SlotDisplay {
  id: string;
  label: string;
  occupied: boolean;
  parkingSpaceId: number;
}

interface Activity {
  eventId: number;
  label: string;
  value: string;
  time: string;
  timestamp: number;
}

export default function HomePage() {
  const { user, logout } = useAuth();
  const { subscribe } = useWebSocket();
  const navigate = useNavigate();
  const [allSlots, setAllSlots] = useState<SlotDisplay[]>([]);
  const [parkingSpaces, setParkingSpaces] = useState<ParkingSpaceDto[]>([]);
  const [selectedParkingSpaceId, setSelectedParkingSpaceId] = useState<number | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [activities, setActivities] = useState<Activity[]>([]);

  // Clear activities when parking space changes
  useEffect(() => {
    setActivities([]);
  }, [selectedParkingSpaceId]);

  // Fetch parking spaces and slots on mount
  // Fetch parking spaces and slots on mount
  useEffect(() => {
    const fetchData = async () => {
      try {
        setIsLoading(true);
        const [parkingSpacesData, slotsData] = await Promise.all([
          parkingSpaceService.getAllParkingSpaces(),
          slotService.getAllSlots(),
        ]);

        setParkingSpaces(parkingSpacesData);

        // Transform backend data to UI format (labels will be assigned when filtering)
        const transformedSlots: SlotDisplay[] = slotsData.map((slot: SlotDto) => ({
          id: `S-${slot.id}`,
          label: '', // Will be set when filtered by parking space
          occupied: slot.isOccupied || false,
          parkingSpaceId: slot.parkingSpaceId,
        }));

        setAllSlots(transformedSlots);

        // Auto-select first parking space if available
        if (parkingSpacesData.length > 0) {
          setSelectedParkingSpaceId(parkingSpacesData[0].id);
        }
      } catch (error: any) {
        console.error('Error fetching data:', error);
        toast.error('Failed to load parking data');
        setAllSlots([]);
        setParkingSpaces([]);
      } finally {
        setIsLoading(false);
      }
    };

    fetchData();
  }, []);

  // Subscribe to real-time slot updates
  useEffect(() => {
    const unsubscribe = subscribe('/topic/overview_updates', (event: any) => {
      if (event?.type !== 'slot_changed') return;

      // Backend sends slotId as number; our UI stores id as `S-<number>`
      const slotKey = `S-${event.slotId}`;
      setAllSlots((prev) =>
        prev.map((s) =>
          s.id === slotKey
            ? {
                ...s,
                occupied: Boolean(event.isOccupied),
              }
            : s,
        ),
      );
    });

    return () => {
      unsubscribe?.();
    };
  }, [subscribe]);

  // Subscribe to real-time activity updates
  useEffect(() => {
    if (!selectedParkingSpaceId) return;

    const unsubscribe = subscribe('/topic/entrylog_new_events', (event: any) => {
      // Only process events for the currently selected parking space
      if (event?.type === 'entrylog_event' && event.parkingSpaceId === selectedParkingSpaceId) {
        const action = event.action === 'vehicle_entered' ? 'entered' : 'exited';
        const actionText = action === 'entered' ? 'entered' : 'exited';

        setActivities((prev) =>
          [
            {
              eventId: event.eventId,
              label: `Vehicle ${actionText}`,
              value: `License: ${event.licensePlate}`,
              time: 'just now',
              timestamp: Date.now(),
            },
            ...prev,
          ].slice(0, 10), // Keep only the last 10 activities
        );
      }
    });

    return () => {
      unsubscribe?.();
    };
  }, [subscribe, selectedParkingSpaceId]);

  // Update activity timestamps
  useEffect(() => {
    const formatTimeAgo = (timestamp: number) => {
      const seconds = Math.floor((Date.now() - timestamp) / 1000);
      if (seconds < 60) return 'just now';
      if (seconds < 3600) return `${Math.floor(seconds / 60)}m ago`;
      if (seconds < 86400) return `${Math.floor(seconds / 3600)}h ago`;
      return `${Math.floor(seconds / 86400)}d ago`;
    };

    const interval = setInterval(() => {
      setActivities(prev =>
        prev.map(activity => ({
          ...activity,
          time: formatTimeAgo(activity.timestamp)
        }))
      );
    }, 60000); // Update every minute

    return () => clearInterval(interval);
  }, []);

  const slots = selectedParkingSpaceId
    ? (() => {
        const filtered = allSlots
          .filter((slot) => slot.parkingSpaceId === selectedParkingSpaceId)
          .slice()
          .sort(
            (a, b) =>
              parseInt(a.id.replace('S-', ''), 10) -
              parseInt(b.id.replace('S-', ''), 10),
          );

        // Map display labels so that the lowest slot id in this parking space becomes S01, then S02...
        return filtered.map((slot, idx) => ({
          ...slot,
          label: `S${String(idx + 1).padStart(2, '0')}`,
        }));
      })()
    : allSlots
        .slice()
        .sort(
          (a, b) =>
            parseInt(a.id.replace('S-', ''), 10) - parseInt(b.id.replace('S-', ''), 10),
        )
        .map((slot) => ({
          ...slot,
          // When no parking space is selected, show global numbering by real id
          label: `S${String(parseInt(slot.id.replace('S-', ''), 10)).padStart(2, '0')}`,
        }));

  const total = slots.length;
  const occupied = slots.filter((s) => s.occupied).length;
  const available = total - occupied;

  const selectedParkingSpace = parkingSpaces.find(ps => ps.id === selectedParkingSpaceId);

  return (
    <div className="min-h-screen bg-slate-50 text-slate-900">

      <div className="mx-auto flex min-h-screen max-w-[1400px] gap-6 p-4 sm:p-6">
        {/* Sidebar */}
        <aside className="hidden w-72 shrink-0 rounded-3xl border border-slate-200 bg-white p-5 shadow-sm lg:block">
          <div className="flex items-center gap-3">
            <div className="grid h-10 w-10 place-items-center rounded-2xl bg-indigo-50 text-indigo-700 ring-1 ring-indigo-200 dark:bg-indigo-500/15 dark:text-indigo-200 dark:ring-indigo-500/25">
              <ParkingSquare className="h-5 w-5" />
            </div>
            <div className="flex-1">
              <div className="text-xs font-semibold tracking-widest text-slate-500">SMART CAR PARKING</div>
              <div className="text-sm font-semibold text-slate-900">Live Overview</div>
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
            <div className="flex items-center gap-3 rounded-2xl bg-slate-100 px-3 py-2.5 text-sm ring-1 ring-slate-200">
              <LayoutDashboard className="h-4 w-4 text-slate-600" />
              <span className="text-slate-900">Dashboard</span>
            </div>
            <button
              onClick={() => navigate('/devices')}
              className="flex w-full items-center gap-3 rounded-2xl px-3 py-2.5 text-sm text-slate-600 hover:bg-slate-100"
            >
              <Radio className="h-4 w-4" />
              <span>Devices</span>
            </button>
            <div className="flex items-center gap-3 rounded-2xl px-3 py-2.5 text-sm text-slate-600 hover:bg-slate-100">
              <Car className="h-4 w-4" />
              <span>Vehicles</span>
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

        {/* Main */}
        <main className="flex-1">
          {/* Topbar */}
          <div className="rounded-3xl border border-slate-200 bg-white px-5 py-4 shadow-sm">
            <div className="flex flex-wrap items-center justify-between gap-4">
              <div>
                <div className="text-xs font-semibold tracking-widest text-slate-500">SMART CAR PARKING SYSTEM</div>
                <h1 className="mt-1 text-2xl font-bold text-slate-900">Live Overview</h1>
              </div>

              <div className="flex items-center gap-3">
                <div className="hidden rounded-2xl border border-slate-200 bg-slate-50 px-3 py-2 text-sm text-slate-600 sm:block">
                  {new Date().toLocaleString()}
                </div>

                {/* Theme is forced to Light for now (dark classes are kept for later enablement) */}

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
                <div className="text-xs text-slate-500">
                  ({parkingSpaces.length} space{parkingSpaces.length !== 1 ? 's' : ''} available)
                </div>
              </div>
            )}
          </div>

          {/* Stats */}
          <div className="mt-6 grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
            <StatCard
              title="Total slots"
              value={String(total)}
              delta={selectedParkingSpace ? selectedParkingSpace.name : 'No space selected'}
              tone="indigo"
              icon={<ParkingSquare className="h-5 w-5" />}
            />
            <StatCard
              title="Available"
              value={String(available)}
              delta="Ready to park"
              tone="emerald"
              icon={<Gauge className="h-5 w-5" />}
            />
            <StatCard
              title="Occupied"
              value={String(occupied)}
              delta="Currently in use"
              tone="rose"
              icon={<Car className="h-5 w-5" />}
            />
            <StatCard
              title="System"
              value="Online"
              delta="Devices reporting"
              tone="amber"
              icon={<Radio className="h-5 w-5" />}
            />
          </div>

          {/* Content */}
          <div className="mt-6 grid grid-cols-1 gap-4 lg:grid-cols-3">
            <div className="lg:col-span-2">
              {isLoading ? (
                <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
                  <div className="flex items-center justify-center py-12">
                    <div className="text-slate-500">Loading parking slots...</div>
                  </div>
                </div>
              ) : parkingSpaces.length === 0 ? (
                <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
                  <div className="flex flex-col items-center justify-center py-12">
                    <ParkingSquare className="h-12 w-12 text-slate-300" />
                    <div className="mt-3 text-slate-900 font-semibold">No Parking Spaces</div>
                    <div className="mt-1 text-sm text-slate-500">Create a parking space to get started</div>
                  </div>
                </div>
              ) : slots.length === 0 ? (
                <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
                  <div className="flex flex-col items-center justify-center py-12">
                    <ParkingSquare className="h-12 w-12 text-slate-300" />
                    <div className="mt-3 text-slate-900 font-semibold">No Slots Available</div>
                    <div className="mt-1 text-sm text-slate-500">
                      {selectedParkingSpace?.name} has no parking slots yet
                    </div>
                  </div>
                </div>
              ) : (
                <OccupancyGrid
                  title={selectedParkingSpace ? `${selectedParkingSpace.name} - Live Overview` : 'Live Overview'}
                  slots={slots}
                />
              )}
            </div>

            <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
              <h3 className="text-sm font-semibold text-slate-900">Activity</h3>
              <div className="mt-4 space-y-3">
                {activities.length === 0 ? (
                  <div className="text-sm text-slate-500">No recent activity</div>
                ) : (
                  activities.map((a, idx) => (
                    <div
                      key={`${a.eventId ?? idx}-${a.timestamp}`}
                      className="flex items-start justify-between gap-3 rounded-xl border border-slate-200 bg-slate-50 p-3"
                    >
                      <div>
                        <div className="text-xs font-semibold text-slate-800">{a.label}</div>
                        <div className="mt-1 text-xs text-slate-500">{a.value}</div>
                      </div>
                      <div className="text-[11px] text-slate-500">{a.time}</div>
                    </div>
                  ))
                )}
              </div>

              <div className="mt-5 rounded-xl border border-slate-200 bg-slate-50 p-3">
                <div className="text-xs font-semibold text-slate-700">Account</div>
                <div className="mt-2 grid grid-cols-2 gap-2 text-xs text-slate-600">
                  <div className="rounded-lg bg-white p-2 ring-1 ring-slate-200">
                    <div className="text-[11px] text-slate-500">Role</div>
                    <div className="mt-1 font-semibold text-slate-900">{user?.role ?? '—'}</div>
                  </div>
                  <div className="rounded-lg bg-white p-2 ring-1 ring-slate-200">
                    <div className="text-[11px] text-slate-500">Status</div>
                    <div className="mt-1 font-semibold text-slate-900">{user?.enabled ? 'Active' : 'Disabled'}</div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div className="mt-6 pb-8 text-xs text-slate-500">Prototype UI • Connected to live database</div>
        </main>
      </div>
    </div>
  );
}


