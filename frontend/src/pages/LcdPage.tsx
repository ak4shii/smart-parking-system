import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Monitor, LayoutDashboard, KeyRound, Radio, ShieldCheck, ParkingSquare, LogOut, Car, DoorOpen, ChevronDown, Plus, RefreshCcw } from 'lucide-react';
import toast from 'react-hot-toast';

import { useAuth } from '../context/AuthContext';
import lcdService, { type LcdDto } from '../services/lcdService';
import parkingSpaceService, { type ParkingSpaceDto } from '../services/parkingSpaceService';
import microcontrollerService, { type MicrocontrollerDto } from '../services/microcontrollerService';

export default function LcdPage() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const [lcds, setLcds] = useState<LcdDto[]>([]);
  const [parkingSpaces, setParkingSpaces] = useState<ParkingSpaceDto[]>([]);
  const [microcontrollers, setMicrocontrollers] = useState<MicrocontrollerDto[]>([]);
  const [selectedParkingSpaceId, setSelectedParkingSpaceId] = useState<number | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [busyId, setBusyId] = useState<number | null>(null);
  const [draft, setDraft] = useState<Record<number, string>>({});
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const [currentLcd, setCurrentLcd] = useState<Partial<LcdDto> | null>(null);

  const selectedParkingSpace = parkingSpaces.find(ps => ps.id === selectedParkingSpaceId);

  const byMc = useMemo(() => {
    const map = new Map<number, LcdDto[]>();
    lcds.forEach((l) => {
      const arr = map.get(l.microcontrollerId) ?? [];
      arr.push(l);
      map.set(l.microcontrollerId, arr);
    });
    return Array.from(map.entries()).sort((a, b) => a[0] - b[0]);
  }, [lcds]);
  const availableMicrocontrollers = useMemo(() => {
    if (!selectedParkingSpaceId) return [];
    return microcontrollers.filter(mc => mc.parkingSpaceId === selectedParkingSpaceId);
  }, [microcontrollers, selectedParkingSpaceId]);

  const fetchLcds = async () => {
    try {
      setIsLoading(true);
      const [parkingSpacesData, lcdsData, microcontrollersData] = await Promise.all([
        parkingSpaceService.getAllParkingSpaces(),
        lcdService.getAllLcds(),
        microcontrollerService.getAllMicrocontrollers(),
      ]);

      setParkingSpaces(parkingSpacesData);
      setLcds(lcdsData);
      setMicrocontrollers(microcontrollersData);

      // Auto-select first parking space if available
      if (parkingSpacesData.length > 0) {
        setSelectedParkingSpaceId(parkingSpacesData[0].id);
      }

      setDraft((prev) => {
        const next = { ...prev };
        lcdsData.forEach((l) => {
          if (next[l.id] === undefined) next[l.id] = l.displayText ?? '';
        });
        return next;
      });
    } catch (e: any) {
      console.error(e);
      toast.error('Failed to load LCDs');
      setParkingSpaces([]);
      setLcds([]);
      setMicrocontrollers([]);
    } finally {
      setIsLoading(false);
    }
  }
  const handleCreate = async () => {
    // Get fresh list of microcontrollers in case it changed
    const microcontrollers = await microcontrollerService.getAllMicrocontrollers();
    setMicrocontrollers(microcontrollers);

    // Set initial state for new LCD
    setCurrentLcd({
      name: '',
      displayText: '',
      microcontrollerId: selectedParkingSpaceId || undefined
    });

    setIsCreateModalOpen(true);
  };

  const handleSave = async () => {
    if (!currentLcd) return;

    try {
      if ('id' in currentLcd && currentLcd.id) {
        // Update
        const updated = await lcdService.updateLcd(currentLcd.id, {
          ...currentLcd,
          displayText: draft[currentLcd.id] || ''
        });
        setLcds((prev) => prev.map((l) => (l.id === updated.id ? updated : l)));
        toast.success('LCD updated successfully');
      } else {
        // Create - ensure required fields are present
        if (!currentLcd.name || currentLcd.microcontrollerId === undefined) {
          toast.error('Name and Microcontroller are required');
          return;
        }
        const createData = {
          name: currentLcd.name,
          displayText: '',
          microcontrollerId: currentLcd.microcontrollerId
        };
        const created = await lcdService.createLcd(createData);
        setLcds((prev) => [...prev, created]);
        setDraft(prev => ({
          ...prev,
          [created.id]: created.displayText || ''
        }));
        toast.success('LCD created successfully');
      }
      setIsCreateModalOpen(false);
      setIsEditModalOpen(false);
      setCurrentLcd(null);
    } catch (e: any) {
      console.error(e);
      toast.error(e.response?.data?.message || 'Failed to save LCD');
    }
  };

  useEffect(() => {
    fetchLcds();
  }, []);

  const save = async (lcd: LcdDto) => {
    try {
      setBusyId(lcd.id);
      const updated = await lcdService.updateLcd(lcd.id, { displayText: draft[lcd.id] ?? '' });
      setLcds((prev) => prev.map((x) => (x.id === lcd.id ? updated : x)));
      toast.success('LCD updated');
    } catch (e: any) {
      console.error(e);
      toast.error(e.response?.data?.message || 'Failed to update LCD');
    } finally {
      setBusyId(null);
    }
  };

  return (
    <div className="min-h-screen bg-slate-50 text-slate-900">
      <div className="mx-auto flex min-h-screen max-w-[1400px] gap-6 p-4 sm:p-6">
        <aside className="hidden w-72 shrink-0 rounded-3xl border border-slate-200 bg-white p-5 shadow-sm lg:block">
          <div className="flex items-center gap-3">
            <div className="grid h-10 w-10 place-items-center rounded-2xl bg-indigo-50 text-indigo-700 ring-1 ring-indigo-200">
              <ParkingSquare className="h-5 w-5" />
            </div>
            <div className="flex-1">
              <div className="text-xs font-semibold tracking-widest text-slate-500">SMART CAR PARKING</div>
              <div className="text-sm font-semibold text-slate-900">LCD Control</div>
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
            <button
              onClick={() => navigate('/doors')}
              className="flex w-full items-center gap-3 rounded-2xl px-3 py-2.5 text-sm text-slate-600 hover:bg-slate-100"
            >
              <DoorOpen className="h-4 w-4" />
              <span>Doors</span>
            </button>
            <div className="flex items-center gap-3 rounded-2xl bg-slate-100 px-3 py-2.5 text-sm ring-1 ring-slate-200">
              <Monitor className="h-4 w-4 text-slate-600" />
              <span className="text-slate-900">LCDs</span>
            </div>
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
                <h1 className="mt-1 text-2xl font-bold text-slate-900">LCD Control</h1>
              </div>
              <div className="flex items-center gap-3">
                <button
                  onClick={fetchLcds}
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
                  <span>Add LCD</span>
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
              <div className="py-12 text-center text-slate-500">Loading LCDs...</div>
            ) : lcds.length === 0 ? (
              <div className="py-12 text-center text-slate-500">No LCDs found.</div>
            ) : (
              <div className="space-y-6">
                {byMc.map(([mcId, list]) => (
                  <div key={mcId}>
                    <div className="mb-3 text-sm font-semibold text-slate-800">Microcontroller #{mcId}</div>
                    <div className="grid grid-cols-1 gap-3 lg:grid-cols-2">
                      {list.map((l) => (
                        <div key={l.id} className="rounded-2xl border border-slate-200 p-4">
                          <div className="flex items-start justify-between gap-3">
                            <div>
                              <div className="text-sm font-semibold text-slate-900">{l.name || `LCD #${l.id}`}</div>
                              <div className="mt-1 text-xs text-slate-500">ID: {l.id}</div>
                            </div>
                            <div className="grid h-10 w-10 place-items-center rounded-2xl bg-indigo-50 text-indigo-700 ring-1 ring-indigo-200">
                              <Monitor className="h-5 w-5" />
                            </div>
                          </div>

                          <div className="mt-3">
                            <label className="text-xs font-semibold text-slate-600">Display text</label>
                            <textarea
                              value={draft[l.id] ?? ''}
                              onChange={(e) => setDraft((p) => ({ ...p, [l.id]: e.target.value }))}
                              rows={3}
                              className="mt-2 w-full rounded-xl border border-slate-200 bg-slate-50 px-3 py-2 text-sm outline-none focus:border-indigo-300 focus:ring-2 focus:ring-indigo-100"
                              placeholder="Enter text to show on LCD"
                              maxLength={255}
                            />
                            <div className="mt-1 text-[11px] text-slate-500">Max 255 chars</div>
                          </div>

                          <div className="mt-3 flex justify-end">
                            <button
                              onClick={() => save(l)}
                              disabled={busyId === l.id}
                              className="rounded-xl bg-indigo-600 px-4 py-2 text-xs font-semibold text-white hover:bg-indigo-700 disabled:opacity-50"
                            >
                              {busyId === l.id ? 'Saving...' : 'Save'}
                            </button>
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
            <h2 className="text-lg font-bold text-slate-900">{isCreateModalOpen ? 'Add New LCD' : 'Edit LCD'}</h2>
            <div className="mt-4 space-y-4">
              <div>
                <label htmlFor="name" className="block text-sm font-medium text-slate-700">Name</label>
                <input
                  type="text"
                  id="name"
                  value={currentLcd?.name || ''}
                  onChange={(e) => setCurrentLcd({ ...currentLcd, name: e.target.value })}
                  className="mt-1 block w-full rounded-md border-slate-300 shadow-sm focus:border-indigo-500 focus:ring-indigo-500 sm:text-sm"
                  required
                />
              </div>

              <div>
                <label htmlFor="microcontrollerId" className="block text-sm font-medium text-slate-700">Microcontroller</label>
                <select
                  id="microcontrollerId"
                  value={currentLcd?.microcontrollerId || ''}
                  onChange={(e) => setCurrentLcd({ ...currentLcd, microcontrollerId: Number(e.target.value) })}
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
                <label htmlFor="displayText" className="block text-sm font-medium text-slate-700">Display Text</label>
                <input
                  type="text"
                  id="displayText"
                  value={currentLcd?.displayText || ''}
                  onChange={(e) => setCurrentLcd({ ...currentLcd, displayText: e.target.value })}
                  className="ml-2 block w-full rounded-md border-slate-300 shadow-sm focus:border-indigo-500 focus:ring-indigo-500 sm:text-sm"
                  placeholder="Optional: Custom display text"
                />
              </div>
            </div>
            <div className="mt-6 flex justify-end gap-3">
              <button
                onClick={() => {
                  setIsCreateModalOpen(false);
                  setIsEditModalOpen(false);
                  setCurrentLcd(null);
                }}
                className="rounded-xl border border-slate-200 bg-white px-4 py-2 text-sm font-semibold text-slate-700 hover:bg-slate-50"
              >
                Cancel
              </button>
              <button
                onClick={handleSave}
                disabled={!currentLcd?.name || !currentLcd?.microcontrollerId}
                className="rounded-xl bg-indigo-600 px-4 py-2 text-sm font-semibold text-white hover:bg-indigo-700 disabled:opacity-50"
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

