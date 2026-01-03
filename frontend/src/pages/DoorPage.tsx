import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { DoorClosed, DoorOpen, LayoutDashboard, KeyRound, Radio, ShieldCheck, ParkingSquare, LogOut, Car, Monitor, ChevronDown, Plus, Edit, Trash2, RefreshCcw } from 'lucide-react';
import toast from 'react-hot-toast';

import { useAuth } from '../context/AuthContext';
import doorService, { type DoorDto } from '../services/doorService';
import parkingSpaceService, { type ParkingSpaceDto } from '../services/parkingSpaceService';
import microcontrollerService, { type MicrocontrollerDto } from '../services/microcontrollerService';
import { useWebSocket } from '../services/websocket';

export default function DoorPage() {
  const { user, logout } = useAuth();
  const { subscribe } = useWebSocket();
  const navigate = useNavigate();

  const [doors, setDoors] = useState<DoorDto[]>([]);
  const [parkingSpaces, setParkingSpaces] = useState<ParkingSpaceDto[]>([]);
  const [microcontrollers, setMicrocontrollers] = useState<MicrocontrollerDto[]>([]);
  const [selectedParkingSpaceId, setSelectedParkingSpaceId] = useState<number | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [busyDoorId, setBusyDoorId] = useState<number | null>(null);
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const [currentDoor, setCurrentDoor] = useState<Partial<DoorDto> | null>(null);

  const selectedParkingSpace = parkingSpaces.find(ps => ps.id === selectedParkingSpaceId);

  const byMc = useMemo(() => {
    const map = new Map<number, DoorDto[]>();
    doors.forEach((d) => {
      const arr = map.get(d.microcontrollerId) ?? [];
      arr.push(d);
      map.set(d.microcontrollerId, arr);
    });
    return Array.from(map.entries()).sort((a, b) => a[0] - b[0]);
  }, [doors]);

  const availableMicrocontrollers = useMemo(() => {
    if (!selectedParkingSpaceId) return [];
    return microcontrollers.filter(mc => mc.parkingSpaceId === selectedParkingSpaceId);
  }, [microcontrollers, selectedParkingSpaceId]);

  const fetchDoors = async () => {
    try {
      setIsLoading(true);
      const [parkingSpacesData, doorsData, microcontrollersData] = await Promise.all([
        parkingSpaceService.getAllParkingSpaces(),
        doorService.getAllDoors(),
        microcontrollerService.getAllMicrocontrollers(),
      ]);

      setParkingSpaces(parkingSpacesData);
      setDoors(doorsData);
      setMicrocontrollers(microcontrollersData);

      // Auto-select first parking space if available
      if (parkingSpacesData.length > 0) {
        setSelectedParkingSpaceId(parkingSpacesData[0].id);
      }
    } catch (e: any) {
      console.error(e);
      toast.error('Failed to load doors');
      setParkingSpaces([]);
      setDoors([]);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchDoors();
  }, []);

  // Subscribe to real-time door updates
  useEffect(() => {
    if (!selectedParkingSpaceId) return;

    const unsubscribe = subscribe('/topic/door_updates', (event: any) => {
      // Only process events for the currently selected parking space
      if (event?.type === 'door_changed' && event.parkingSpaceId === selectedParkingSpaceId) {
        // Update the door in the list
        setDoors((prevDoors) =>
          prevDoors.map((door) =>
            door.id === event.doorId
              ? { ...door, isOpened: event.isOpened }
              : door
          )
        );
        
        // Show toast notification
        const status = event.isOpened ? 'opened' : 'closed';
        toast.success(`Door "${event.doorName}" ${status}`);
      }
    });

    return () => {
      unsubscribe?.();
    };
  }, [subscribe, selectedParkingSpaceId]);

  const toggleDoor = async (door: DoorDto) => {
    try {
      setBusyDoorId(door.id);
      const updated = await doorService.updateDoor(door.id, { isOpened: !door.isOpened });
      setDoors((prev) => prev.map((d) => (d.id === door.id ? updated : d)));
      toast.success(`Door "${updated.name || '#' + updated.id}" is now ${updated.isOpened ? 'OPEN' : 'CLOSED'}`);
    } catch (e: any) {
      console.error(e);
      toast.error(e.response?.data?.message || 'Failed to update door');
    } finally {
      setBusyDoorId(null);
    }
  };

  const handleCreate = async () => {
    // Get fresh list of microcontrollers in case it changed
    const microcontrollers = await microcontrollerService.getAllMicrocontrollers();
    setMicrocontrollers(microcontrollers);

    // Set initial state for new door
    setCurrentDoor({
      name: '',
      isOpened: false,
      microcontrollerId: selectedParkingSpaceId || undefined
    });

    setIsCreateModalOpen(true);
  };

  const handleEdit = (door: DoorDto) => {
    setCurrentDoor(door);
    setIsEditModalOpen(true);
  };

  const handleDelete = async (id: number) => {
    if (window.confirm('Are you sure you want to delete this door?')) {
      try {
        await doorService.deleteDoor(id);
        setDoors((prev) => prev.filter((d) => d.id !== id));
        toast.success('Door deleted successfully');
      } catch (e: any) {
        console.error(e);
        toast.error(e.response?.data?.message || 'Failed to delete door');
      }
    }
  };

  const handleSave = async () => {
    if (!currentDoor) return;

    try {
      if ('id' in currentDoor && currentDoor.id) {
        // Update
        const updated = await doorService.updateDoor(currentDoor.id, currentDoor);
        setDoors((prev) => prev.map((d) => (d.id === updated.id ? updated : d)));
        toast.success('Door updated successfully');
      } else {
        // Create - ensure required fields are present
        if (!currentDoor.name || currentDoor.microcontrollerId === undefined) {
          toast.error('Name and Microcontroller ID are required');
          return;
        }
        const createData = {
          name: currentDoor.name,
          isOpened: currentDoor.isOpened || false,
          microcontrollerId: currentDoor.microcontrollerId
        };
        const created = await doorService.createDoor(createData);
        setDoors((prev) => [...prev, created]);
        toast.success('Door created successfully');
      }
      setIsCreateModalOpen(false);
      setIsEditModalOpen(false);
      setCurrentDoor(null);
    } catch (e: any) {
      console.error(e);
      toast.error(e.response?.data?.message || 'Failed to save door');
    }
  };

  return (
    <div className="min-h-screen bg-slate-50 text-slate-900">
      <div className="mx-auto flex min-h-screen max-w-[1400px] gap-6 p-4 sm:p-6">
        {/* Sidebar (kept consistent with other pages) */}
        <aside className="hidden w-72 shrink-0 rounded-3xl border border-slate-200 bg-white p-5 shadow-sm lg:block">
          <div className="flex items-center gap-3">
            <div className="grid h-10 w-10 place-items-center rounded-2xl bg-indigo-50 text-indigo-700 ring-1 ring-indigo-200">
              <ParkingSquare className="h-5 w-5" />
            </div>
            <div className="flex-1">
              <div className="text-xs font-semibold tracking-widest text-slate-500">SMART CAR PARKING</div>
              <div className="text-sm font-semibold text-slate-900">Door Control</div>
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
            <div className="flex items-center gap-3 rounded-2xl bg-slate-100 px-3 py-2.5 text-sm ring-1 ring-slate-200">
              <DoorOpen className="h-4 w-4 text-slate-600" />
              <span className="text-slate-900">Doors</span>
            </div>
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
            <button onClick={logout} className="mt-4 inline-flex w-full items-center justify-center gap-2 rounded-xl bg-rose-600 px-3 py-2 text-sm font-semibold text-white hover:bg-rose-700">
              <LogOut className="h-4 w-4" /> Logout
            </button>
          </div>
        </aside>

        <main className="flex-1">
          <div className="rounded-3xl border border-slate-200 bg-white px-5 py-4 shadow-sm">
            <div className="flex flex-wrap items-center justify-between gap-4">
              <div>
                <div className="text-xs font-semibold tracking-widest text-slate-500">SMART CAR PARKING SYSTEM</div>
                <h1 className="mt-1 text-2xl font-bold text-slate-900">Door Control</h1>
              </div>
              <div className="flex items-center gap-3">
                <button
                  onClick={fetchDoors}
                  className="flex items-center gap-2 rounded-xl border border-slate-200 bg-white px-4 py-2 text-sm font-semibold text-slate-700 hover:bg-slate-50"
                >
                  <RefreshCcw className="h-4 w-4" />
                  <span>Refresh</span>
                </button>
                <button
                  onClick={handleCreate}
                  className="flex items-center gap-2 rounded-xl bg-indigo-600 px-4 py-2 text-sm font-semibold text-white hover:bg-indigo-700"
                >
                  <Plus className="h-4 w-4" />
                  <span>Add Door</span>
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

          <div className="mt-6 rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
            {isLoading ? (
              <div className="py-12 text-center text-slate-500">Loading doors...</div>
            ) : doors.length === 0 ? (
              <div className="py-12 text-center text-slate-500">No doors found.</div>
            ) : (
              <div className="space-y-6">
                {byMc.map(([mcId, list]) => (

                  <div key={mcId}>
                    <div className="mb-3 text-sm font-semibold text-slate-800">Microcontroller #{mcId}</div>
                    <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3">
                      {list.map((d) => (
                        <div key={d.id} className="rounded-2xl border border-slate-200 p-4">
                          <div className="flex items-start justify-between gap-3">
                            <div>
                              <div className="text-sm font-semibold text-slate-900">{d.name || `Door #${d.id}`}</div>
                              <div className="mt-1 text-xs text-slate-500">ID: {d.id}</div>
                            </div>
                            <div className={`grid h-10 w-10 place-items-center rounded-2xl ${d.isOpened ? 'bg-emerald-50 text-emerald-700 ring-1 ring-emerald-200' : 'bg-slate-50 text-slate-700 ring-1 ring-slate-200'}`}>
                              {d.isOpened ? <DoorOpen className="h-5 w-5" /> : <DoorClosed className="h-5 w-5" />}
                            </div>
                          </div>

                          <div className="mt-3 flex items-center justify-between">
                            <span className={`text-xs font-semibold ${d.isOpened ? 'text-emerald-700' : 'text-slate-600'}`}>
                              {d.isOpened ? 'OPEN' : 'CLOSED'}
                            </span>
                            <div className="flex items-center gap-2">
                              <button
                                onClick={() => handleEdit(d)}
                                className="rounded-lg p-2 text-slate-400 hover:bg-slate-100 hover:text-slate-600"
                                title="Edit door"
                              >
                                <Edit className="h-4 w-4" />
                              </button>
                              <button
                                onClick={() => handleDelete(d.id)}
                                className="rounded-lg p-2 text-slate-400 hover:bg-red-50 hover:text-red-500"
                                title="Delete door"
                              >
                                <Trash2 className="h-4 w-4" />
                              </button>
                              <button
                                disabled={busyDoorId === d.id}
                                onClick={() => toggleDoor(d)}
                                className={`rounded-xl px-4 py-2 text-xs font-semibold text-white disabled:opacity-50 ${d.isOpened ? 'bg-slate-700 hover:bg-slate-800' : 'bg-indigo-600 hover:bg-indigo-700'}`}
                              >
                                {busyDoorId === d.id ? 'Saving...' : d.isOpened ? 'Close' : 'Open'}
                              </button>
                            </div>
                          </div>
                        </div>
                      ))}
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </main>
      </div>

      {/* Create/Edit Modal */}
      {(isCreateModalOpen || isEditModalOpen) && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm">
          <div className="w-full max-w-md rounded-2xl bg-white p-6 shadow-xl">
            <h2 className="text-lg font-bold text-slate-900">{isCreateModalOpen ? 'Add New Door' : 'Edit Door'}</h2>
            <div className="mt-4 space-y-4">
              <div>
                <label htmlFor="name" className="block text-sm font-medium text-slate-700">Name</label>
                <input
                  type="text"
                  id="name"
                  value={currentDoor?.name || ''}
                  onChange={(e) => setCurrentDoor({ ...currentDoor, name: e.target.value })}
                  className="mt-1 block w-full rounded-md border-slate-300 shadow-sm focus:border-indigo-500 focus:ring-indigo-500 sm:text-sm"
                />
              </div>
              <div>
                <label htmlFor="microcontrollerId" className="block text-sm font-medium text-slate-700">Microcontroller</label>
                <select
                  id="microcontrollerId"
                  value={currentDoor?.microcontrollerId || ''}
                  onChange={(e) => setCurrentDoor({ ...currentDoor, microcontrollerId: Number(e.target.value) })}
                  className="mt-1 block w-full rounded-md border-slate-300 py-2 pl-3 pr-10 text-base focus:border-indigo-500 focus:outline-none focus:ring-indigo-500 sm:text-sm"
                  required
                >
                  <option value="">Select a microcontroller</option>
                  {availableMicrocontrollers.map((mc) => (
                    <option key={mc.id} value={mc.id}>
                      {mc.name || `MC-${mc.mcCode}`} (ID: {mc.id})
                    </option>
                  ))}
                  {availableMicrocontrollers.length === 0 && (
                    <option disabled>No microcontrollers available in this parking space</option>
                  )}
                </select>
                {availableMicrocontrollers.length === 0 && (
                  <p className="mt-1 text-xs text-amber-600">
                    No microcontrollers found in the selected parking space.
                  </p>
                )}
              </div>
              <div className="flex items-center">
                <input
                  type="checkbox"
                  id="isOpened"
                  checked={currentDoor?.isOpened || false}
                  onChange={(e) => setCurrentDoor({ ...currentDoor, isOpened: e.target.checked })}
                  className="h-4 w-4 rounded border-slate-300 text-indigo-600 focus:ring-indigo-500"
                />
                <label htmlFor="isOpened" className="ml-2 block text-sm text-slate-900">Is Opened</label>
              </div>
            </div>
            <div className="mt-6 flex justify-end gap-3">
              <button
                onClick={() => {
                  setIsCreateModalOpen(false);
                  setIsEditModalOpen(false);
                  setCurrentDoor(null);
                }}
                className="rounded-xl border border-slate-200 bg-white px-4 py-2 text-sm font-semibold text-slate-700 hover:bg-slate-50"
              >
                Cancel
              </button>
              <button
                onClick={handleSave}
                className="rounded-xl bg-indigo-600 px-4 py-2 text-sm font-semibold text-white hover:bg-indigo-700"
              >
                Save
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}


