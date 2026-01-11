import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  KeyRound,
  Plus,
  Trash2,
  Search,
  X,
  DoorOpen,
  Monitor,
  LayoutDashboard,
  Radio,
  Car,
  ShieldCheck,
  ParkingSquare,
  LogOut,
  ChevronDown
} from 'lucide-react';
import toast from 'react-hot-toast';

import { useAuth } from '../context/AuthContext';
import rfidService, { type RfidDto, type CreateRfidRequest } from '../services/rfidService';
import parkingSpaceService, { type ParkingSpaceDto } from '../services/parkingSpaceService';
import { useWebSocket } from '../services/websocket';

export default function RfidPage() {
  const { user, logout } = useAuth();
  const { subscribe } = useWebSocket();
  const navigate = useNavigate();
  const [allRfids, setAllRfids] = useState<RfidDto[]>([]);
  const [parkingSpaces, setParkingSpaces] = useState<ParkingSpaceDto[]>([]);
  const [selectedParkingSpaceId, setSelectedParkingSpaceId] = useState<number | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Form state
  const [formData, setFormData] = useState<CreateRfidRequest>({
    rfidCode: '',
    parkingSpaceId: 0,
  });

  // Fetch RFIDs and Parking Spaces
  useEffect(() => {
    fetchData();
  }, []);

  const fetchData = async () => {
    try {
      setIsLoading(true);
      const [rfidsData, parkingSpacesData] = await Promise.all([
        rfidService.getAllRfids(),
        parkingSpaceService.getAllParkingSpaces(),
      ]);
      setAllRfids(rfidsData);
      setParkingSpaces(parkingSpacesData);

      // Auto-select first parking space if available
      if (parkingSpacesData.length > 0) {
        setSelectedParkingSpaceId(parkingSpacesData[0].id);
      }
    } catch (error: any) {
      console.error('Error fetching data:', error);
      toast.error('Failed to load RFID cards');
    } finally {
      setIsLoading(false);
    }
  };

  // Subscribe to real-time RFID updates
  useEffect(() => {
    if (!selectedParkingSpaceId) return;

    const unsubscribe = subscribe('/topic/rfid_updates', (event: any) => {
      console.log('[WebSocket] RFID update received:', event);
      // Only process events for the currently selected parking space
      if (event?.type === 'rfid_changed' && event.parkingSpaceId === selectedParkingSpaceId) {
        // Update the RFID in the list
        setAllRfids((prevRfids) =>
          prevRfids.map((rfid) =>
            rfid.id === event.rfidId
              ? { ...rfid, currentlyUsed: event.currentlyUsed }
              : rfid
          )
        );

        // Show toast notification
        const status = event.currentlyUsed ? 'now in use' : 'now available';
        toast.success(`RFID "${event.rfidCode}" is ${status}`);
      }
    });

    return () => {
      unsubscribe?.();
    };
  }, [subscribe, selectedParkingSpaceId]);

  const handleCreateRfid = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!formData.rfidCode.trim()) {
      toast.error('RFID code is required');
      return;
    }

    if (!formData.parkingSpaceId) {
      toast.error('Please select a parking space');
      return;
    }

    try {
      setIsSubmitting(true);
      await rfidService.createRfid(formData);
      toast.success('RFID card created successfully');
      setShowCreateModal(false);
      setFormData({ rfidCode: '', parkingSpaceId: 0 });
      fetchData();
    } catch (error: any) {
      console.error('Error creating RFID:', error);
      toast.error(error.response?.data?.message || 'Failed to create RFID card');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleDeleteRfid = async (id: number, rfidCode: string) => {
    if (!confirm(`Are you sure you want to delete RFID card "${rfidCode}"?`)) {
      return;
    }

    try {
      await rfidService.deleteRfid(id);
      toast.success('RFID card deleted successfully');
      fetchData();
    } catch (error: any) {
      console.error('Error deleting RFID:', error);
      toast.error('Failed to delete RFID card');
    }
  };

  const getParkingSpaceName = (parkingSpaceId: number) => {
    const space = parkingSpaces.find(ps => ps.id === parkingSpaceId);
    return space ? space.name : 'Unknown';
  };

  // Filter RFIDs by selected parking space
  const rfids = selectedParkingSpaceId
    ? allRfids.filter(rfid => rfid.parkingSpaceId === selectedParkingSpaceId)
    : allRfids;

  // Then filter by search term
  const filteredRfids = rfids.filter(rfid =>
    rfid.rfidCode.toLowerCase().includes(searchTerm.toLowerCase()) ||
    getParkingSpaceName(rfid.parkingSpaceId).toLowerCase().includes(searchTerm.toLowerCase())
  );

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
              <div className="text-sm font-semibold text-slate-900">RFID Management</div>
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
            <div className="flex items-center gap-3 rounded-2xl bg-slate-100 px-3 py-2.5 text-sm ring-1 ring-slate-200">
              <KeyRound className="h-4 w-4 text-slate-600" />
              <span className="text-slate-900">RFID</span>
            </div>
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
                <h1 className="mt-1 text-2xl font-bold text-slate-900">RFID Management</h1>
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
          <div className="mt-6 grid grid-cols-1 gap-4 sm:grid-cols-3">
            <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
              <div className="flex items-start justify-between">
                <div>
                  <div className="text-xs font-semibold text-slate-500">Total RFID Cards</div>
                  <div className="mt-2 text-3xl font-bold text-slate-900">{rfids.length}</div>
                  <div className="mt-1 text-xs text-slate-500">
                    {selectedParkingSpace ? selectedParkingSpace.name : 'All spaces'}
                  </div>
                </div>
                <div className="grid h-12 w-12 place-items-center rounded-2xl bg-indigo-50 text-indigo-700 ring-1 ring-indigo-200">
                  <KeyRound className="h-6 w-6" />
                </div>
              </div>
            </div>

            <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
              <div className="flex items-start justify-between">
                <div>
                  <div className="text-xs font-semibold text-slate-500">Active Cards</div>
                  <div className="mt-2 text-3xl font-bold text-emerald-600">
                    {rfids.filter(r => r.currentlyUsed).length}
                  </div>
                  <div className="mt-1 text-xs text-slate-500">Currently in use</div>
                </div>
                <div className="grid h-12 w-12 place-items-center rounded-2xl bg-emerald-50 text-emerald-700 ring-1 ring-emerald-200">
                  <KeyRound className="h-6 w-6" />
                </div>
              </div>
            </div>

            <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
              <div className="flex items-start justify-between">
                <div>
                  <div className="text-xs font-semibold text-slate-500">Inactive Cards</div>
                  <div className="mt-2 text-3xl font-bold text-slate-600">
                    {rfids.filter(r => !r.currentlyUsed).length}
                  </div>
                  <div className="mt-1 text-xs text-slate-500">Available for use</div>
                </div>
                <div className="grid h-12 w-12 place-items-center rounded-2xl bg-slate-50 text-slate-700 ring-1 ring-slate-200">
                  <KeyRound className="h-6 w-6" />
                </div>
              </div>
            </div>
          </div>

          {/* Table Section */}
          <div className="mt-6 rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
            <div className="flex flex-wrap items-center justify-between gap-4 mb-5">
              <h2 className="text-lg font-semibold text-slate-900">RFID Cards</h2>

              <div className="flex items-center gap-3">
                {/* Search */}
                <div className="relative">
                  <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
                  <input
                    type="text"
                    placeholder="Search RFID..."
                    value={searchTerm}
                    onChange={(e) => setSearchTerm(e.target.value)}
                    className="rounded-xl border border-slate-200 bg-slate-50 py-2 pl-9 pr-3 text-sm outline-none focus:border-indigo-300 focus:ring-2 focus:ring-indigo-100"
                  />
                </div>

                {/* Add Button */}
                <button
                  onClick={() => {
                    setFormData({
                      rfidCode: '',
                      parkingSpaceId: selectedParkingSpaceId || 0,
                    });
                    setShowCreateModal(true);
                  }}
                  className="inline-flex items-center gap-2 rounded-xl bg-indigo-600 px-4 py-2 text-sm font-semibold text-white hover:bg-indigo-700"
                >
                  <Plus className="h-4 w-4" />
                  Add RFID
                </button>
              </div>
            </div>

            {/* Table */}
            {isLoading ? (
              <div className="flex items-center justify-center py-12">
                <div className="text-slate-500">Loading RFID cards...</div>
              </div>
            ) : parkingSpaces.length === 0 ? (
              <div className="flex flex-col items-center justify-center py-12">
                <ParkingSquare className="h-12 w-12 text-slate-300" />
                <div className="mt-3 text-slate-900 font-semibold">No Parking Spaces</div>
                <div className="mt-1 text-sm text-slate-500">Create a parking space to manage RFID cards</div>
              </div>
            ) : filteredRfids.length === 0 ? (
              <div className="flex flex-col items-center justify-center py-12">
                <KeyRound className="h-12 w-12 text-slate-300" />
                <div className="mt-3 text-slate-900 font-semibold">
                  {searchTerm ? 'No RFID cards found' : 'No RFID cards yet'}
                </div>
                <div className="mt-1 text-sm text-slate-500">
                  {searchTerm
                    ? 'Try adjusting your search terms'
                    : selectedParkingSpace
                      ? `${selectedParkingSpace.name} has no RFID cards yet`
                      : 'Add RFID cards to get started'}
                </div>
                {!searchTerm && (
                  <button
                    onClick={() => {
                      setFormData({
                        rfidCode: '',
                        parkingSpaceId: selectedParkingSpaceId || 0,
                      });
                      setShowCreateModal(true);
                    }}
                    className="mt-4 text-sm text-indigo-600 hover:text-indigo-700"
                  >
                    Add RFID card
                  </button>
                )}
              </div>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full">
                  <thead>
                    <tr className="border-b border-slate-200">
                      <th className="pb-3 text-left text-xs font-semibold text-slate-500">ID</th>
                      <th className="pb-3 text-left text-xs font-semibold text-slate-500">RFID Code</th>
                      <th className="pb-3 text-left text-xs font-semibold text-slate-500">Parking Space</th>
                      <th className="pb-3 text-left text-xs font-semibold text-slate-500">Status</th>
                      <th className="pb-3 text-right text-xs font-semibold text-slate-500">Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {filteredRfids.map((rfid) => (
                      <tr key={rfid.id} className="border-b border-slate-100 hover:bg-slate-50">
                        <td className="py-4 text-sm text-slate-600">#{rfid.id}</td>
                        <td className="py-4">
                          <div className="flex items-center gap-2">
                            <KeyRound className="h-4 w-4 text-indigo-600" />
                            <span className="font-mono text-sm font-semibold text-slate-900">
                              {rfid.rfidCode}
                            </span>
                          </div>
                        </td>
                        <td className="py-4 text-sm text-slate-600">
                          {getParkingSpaceName(rfid.parkingSpaceId)}
                        </td>
                        <td className="py-4">
                          {rfid.currentlyUsed ? (
                            <span className="inline-flex items-center gap-1.5 rounded-full bg-emerald-100 px-2.5 py-1 text-xs font-semibold text-emerald-700">
                              <span className="h-1.5 w-1.5 rounded-full bg-emerald-600"></span>
                              Active
                            </span>
                          ) : (
                            <span className="inline-flex items-center gap-1.5 rounded-full bg-slate-100 px-2.5 py-1 text-xs font-semibold text-slate-600">
                              <span className="h-1.5 w-1.5 rounded-full bg-slate-400"></span>
                              Inactive
                            </span>
                          )}
                        </td>
                        <td className="py-4 text-right">
                          <button
                            onClick={() => handleDeleteRfid(rfid.id, rfid.rfidCode)}
                            className="inline-flex items-center gap-1.5 rounded-lg bg-rose-50 px-3 py-1.5 text-xs font-semibold text-rose-700 hover:bg-rose-100"
                          >
                            <Trash2 className="h-3.5 w-3.5" />
                            Delete
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </main>
      </div>

      {/* Create Modal */}
      {showCreateModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
          <div className="w-full max-w-md rounded-2xl border border-slate-200 bg-white p-6 shadow-xl">
            <div className="flex items-center justify-between mb-5">
              <h3 className="text-lg font-semibold text-slate-900">Add New RFID Card</h3>
              <button
                onClick={() => setShowCreateModal(false)}
                className="rounded-lg p-1 hover:bg-slate-100"
              >
                <X className="h-5 w-5 text-slate-500" />
              </button>
            </div>

            <form onSubmit={handleCreateRfid} className="space-y-4">
              <div>
                <label className="block text-sm font-semibold text-slate-700 mb-2">
                  RFID Code *
                </label>
                <input
                  type="text"
                  value={formData.rfidCode}
                  onChange={(e) => setFormData({ ...formData, rfidCode: e.target.value })}
                  placeholder="Enter RFID code"
                  className="w-full rounded-xl border border-slate-200 bg-slate-50 px-4 py-2.5 text-sm outline-none focus:border-indigo-300 focus:ring-2 focus:ring-indigo-100"
                  required
                  minLength={3}
                  maxLength={100}
                />
                <p className="mt-1 text-xs text-slate-500">3-100 characters</p>
              </div>

              <div>
                <label className="block text-sm font-semibold text-slate-700 mb-2">
                  Parking Space *
                </label>
                <select
                  value={formData.parkingSpaceId}
                  onChange={(e) => setFormData({ ...formData, parkingSpaceId: Number(e.target.value) })}
                  className="w-full rounded-xl border border-slate-200 bg-slate-50 px-4 py-2.5 text-sm outline-none focus:border-indigo-300 focus:ring-2 focus:ring-indigo-100"
                  required
                >
                  <option value={0}>Select parking space</option>
                  {parkingSpaces.map((space) => (
                    <option key={space.id} value={space.id}>
                      {space.name} - {space.location}
                    </option>
                  ))}
                </select>
              </div>

              <div className="flex gap-3 pt-2">
                <button
                  type="button"
                  onClick={() => setShowCreateModal(false)}
                  className="flex-1 rounded-xl border border-slate-200 bg-white px-4 py-2.5 text-sm font-semibold text-slate-700 hover:bg-slate-50"
                  disabled={isSubmitting}
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="flex-1 rounded-xl bg-indigo-600 px-4 py-2.5 text-sm font-semibold text-white hover:bg-indigo-700 disabled:opacity-50"
                  disabled={isSubmitting}
                >
                  {isSubmitting ? 'Creating...' : 'Create RFID'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}

