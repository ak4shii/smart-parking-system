import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Building2,
  Plus,
  Trash2,
  Edit,
  X,
  LayoutDashboard,
  Radio,
  Car,
  KeyRound,
  DoorOpen,
  Monitor,
  ShieldCheck,
  ParkingSquare,
  LogOut,
  ChevronRight,
  Check,
} from 'lucide-react';
import toast from 'react-hot-toast';

import { useAuth } from '../context/AuthContext';
import parkingSpaceService, {
  type ParkingSpaceDto,
  type UpdateParkingSpaceRequest,
} from '../services/parkingSpaceService';
import microcontrollerService, { type CreateMicrocontrollerRequest } from '../services/microcontrollerService';

type WizardStep = 1 | 2 | 3;

interface ParkingSpaceFormData {
  name: string;
  location: string;
}

interface MicrocontrollerFormData {
  mcCode: string;
  name: string;
}

export default function AdminPage() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [parkingSpaces, setParkingSpaces] = useState<ParkingSpaceDto[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [showCreateWizard, setShowCreateWizard] = useState(false);
  const [showEditModal, setShowEditModal] = useState(false);
  const [editingSpace, setEditingSpace] = useState<ParkingSpaceDto | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Wizard state
  const [wizardStep, setWizardStep] = useState<WizardStep>(1);
  const [parkingSpaceData, setParkingSpaceData] = useState<ParkingSpaceFormData>({
    name: '',
    location: '',
  });
  const [microcontrollerData, setMicrocontrollerData] = useState<MicrocontrollerFormData>({
    mcCode: '',
    name: '',
  });
  
  

  // Validation errors state
  const [validationErrors, setValidationErrors] = useState<{
    name?: string;
    location?: string;
    mcCode?: string;
    mcName?: string;
  }>({});

  // Edit form state
  const [editFormData, setEditFormData] = useState<UpdateParkingSpaceRequest>({
    name: '',
    location: '',
  });

  useEffect(() => {
    fetchParkingSpaces();
  }, []);

  const fetchParkingSpaces = async () => {
    try {
      setIsLoading(true);
      const data = await parkingSpaceService.getAllParkingSpaces();
      setParkingSpaces(data);
    } catch (error: any) {
      console.error('Error fetching parking spaces:', error);
      toast.error('Failed to load parking spaces');
    } finally {
      setIsLoading(false);
    }
  };

  const resetWizard = () => {
    setWizardStep(1);
    setParkingSpaceData({ name: '', location: '' });
    setMicrocontrollerData({ mcCode: '', name: '' });
    setValidationErrors({});
  };

  // Validation functions for each step
  const validateStep1 = (): boolean => {
    const errors: { name?: string; location?: string } = {};

    // Validate name
    if (!parkingSpaceData.name.trim()) {
      errors.name = 'Name is required';
    } else if (parkingSpaceData.name.trim().length < 3) {
      errors.name = 'Name must be at least 3 characters';
    } else if (parkingSpaceData.name.trim().length > 100) {
      errors.name = 'Name must not exceed 100 characters';
    }

    // Validate location
    if (!parkingSpaceData.location.trim()) {
      errors.location = 'Location is required';
    } else if (parkingSpaceData.location.trim().length < 3) {
      errors.location = 'Location must be at least 3 characters';
    } else if (parkingSpaceData.location.trim().length > 200) {
      errors.location = 'Location must not exceed 200 characters';
    }

    setValidationErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const validateStep2 = (): boolean => {
    const errors: { mcCode?: string; mcName?: string } = {};

    // Validate mcCode
    if (!microcontrollerData.mcCode.trim()) {
      errors.mcCode = 'Microcontroller code is required';
    } else if (microcontrollerData.mcCode.trim().length < 3) {
      errors.mcCode = 'Code must be at least 3 characters';
    } else if (microcontrollerData.mcCode.trim().length > 100) {
      errors.mcCode = 'Code must not exceed 100 characters';
    }

    // Validate mcName
    if (!microcontrollerData.name.trim()) {
      errors.mcName = 'Controller name is required';
    } else if (microcontrollerData.name.trim().length < 1) {
      errors.mcName = 'Name must be at least 1 character';
    } else if (microcontrollerData.name.trim().length > 100) {
      errors.mcName = 'Name must not exceed 100 characters';
    }

    setValidationErrors(errors);
    return Object.keys(errors).length === 0;
  };

  

  const handleNextStep = () => {
    let isValid = false;

    switch (wizardStep) {
      case 1:
        isValid = validateStep1();
        break;
      case 2:
        isValid = validateStep2();
        break;
      
      default:
        isValid = true;
    }

    if (isValid) {
      setWizardStep((prev) => (prev + 1) as WizardStep);
    } else {
      toast.error('Please fix the validation errors before proceeding');
    }
  };

  const handleCreateComplete = async () => {
    // Final validation before submission
    if (!parkingSpaceData.name.trim() || !parkingSpaceData.location.trim()) {
      toast.error('Please fill in all parking space details');
      return;
    }

    if (!microcontrollerData.mcCode.trim() || !microcontrollerData.name.trim()) {
      toast.error('Please fill in all microcontroller details');
      return;
    }

    try {
      setIsSubmitting(true);

      // Step 1: Create Parking Space
      const parkingSpace = await parkingSpaceService.createParkingSpace(parkingSpaceData);
      toast.success(`Parking space "${parkingSpace.name}" created!`);

      // Step 2: Create Microcontroller
      const mcRequest: CreateMicrocontrollerRequest = {
        ...microcontrollerData,
        parkingSpaceId: parkingSpace.id,
      };
      await microcontrollerService.createMicrocontroller(mcRequest);
      toast.success('Parking space created successfully! (Provision will create slots/sensors/RFID later)');
      setShowCreateWizard(false);
      resetWizard();
      fetchParkingSpaces();
    } catch (error: any) {
      console.error('Error creating parking space:', error);
      
      // Provide specific error messages
      if (error.response?.data?.error) {
        toast.error(error.response.data.error);
      } else if (error.response?.data?.message) {
        toast.error(error.response.data.message);
      } else if (error.response?.status === 400) {
        toast.error('Invalid data provided. Please check all fields and try again.');
      } else if (error.response?.status === 403) {
        toast.error('You do not have permission to perform this action');
      } else if (error.response?.status === 401) {
        toast.error('Your session has expired. Please log in again');
        logout();
      } else {
        toast.error(error.message || 'Failed to create parking space. Please try again.');
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleEditSpace = (space: ParkingSpaceDto) => {
    setEditingSpace(space);
    setEditFormData({
      name: space.name,
      location: space.location,
    });
    setShowEditModal(true);
  };

  const handleUpdateSpace = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!editingSpace) return;

    try {
      setIsSubmitting(true);
      await parkingSpaceService.updateParkingSpace(editingSpace.id, editFormData);
      toast.success('Parking space updated successfully');
      setShowEditModal(false);
      setEditingSpace(null);
      fetchParkingSpaces();
    } catch (error: any) {
      console.error('Error updating parking space:', error);
      toast.error('Failed to update parking space');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleDeleteSpace = async (id: number, name: string) => {
    if (!confirm(`Are you sure you want to delete "${name}"?\n\nThis action cannot be undone. Related data (slots/sensors/RFID/logs) may also be removed depending on provisioning and database cascade rules.`)) {
      return;
    }

    try {
      setIsSubmitting(true);
      await parkingSpaceService.deleteParkingSpace(id);
      toast.success(`Parking space "${name}" deleted successfully`);
      await fetchParkingSpaces();
    } catch (error: any) {
      console.error('Error deleting parking space:', error);
      
      // Provide more specific error messages
      if (error.response?.status === 403) {
        toast.error('You do not have permission to delete this parking space');
      } else if (error.response?.status === 404) {
        toast.error('Parking space not found');
      } else if (error.response?.status === 401) {
        toast.error('Your session has expired. Please log in again');
        logout();
      } else if (error.response?.data?.message) {
        toast.error(error.response.data.message);
      } else {
        toast.error(error.message || 'Failed to delete parking space. Please try again.');
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  const renderWizardStep = () => {
    switch (wizardStep) {
      case 1:
        return (
          <div className="space-y-4">
            <h3 className="text-lg font-semibold text-slate-900">Step 1: Parking Space Details</h3>
            <div>
              <label className="block text-sm font-semibold text-slate-700 mb-2">Name *</label>
              <input
                type="text"
                value={parkingSpaceData.name}
                onChange={(e) => {
                  setParkingSpaceData({ ...parkingSpaceData, name: e.target.value });
                  // Clear error when user types
                  if (validationErrors.name) {
                    setValidationErrors({ ...validationErrors, name: undefined });
                  }
                }}
                placeholder="e.g., Downtown Parking"
                className={`w-full rounded-xl border px-4 py-2.5 text-sm outline-none focus:ring-2 ${
                  validationErrors.name
                    ? 'border-rose-300 bg-rose-50 focus:border-rose-400 focus:ring-rose-100'
                    : 'border-slate-200 bg-slate-50 focus:border-indigo-300 focus:ring-indigo-100'
                }`}
              />
              {validationErrors.name && (
                <p className="mt-1 text-xs text-rose-600">{validationErrors.name}</p>
              )}
            </div>
            <div>
              <label className="block text-sm font-semibold text-slate-700 mb-2">Location *</label>
              <input
                type="text"
                value={parkingSpaceData.location}
                onChange={(e) => {
                  setParkingSpaceData({ ...parkingSpaceData, location: e.target.value });
                  // Clear error when user types
                  if (validationErrors.location) {
                    setValidationErrors({ ...validationErrors, location: undefined });
                  }
                }}
                placeholder="e.g., 123 Main Street, Hanoi"
                className={`w-full rounded-xl border px-4 py-2.5 text-sm outline-none focus:ring-2 ${
                  validationErrors.location
                    ? 'border-rose-300 bg-rose-50 focus:border-rose-400 focus:ring-rose-100'
                    : 'border-slate-200 bg-slate-50 focus:border-indigo-300 focus:ring-indigo-100'
                }`}
              />
              {validationErrors.location && (
                <p className="mt-1 text-xs text-rose-600">{validationErrors.location}</p>
              )}
            </div>
          </div>
        );

      case 2:
        return (
          <div className="space-y-4">
            <h3 className="text-lg font-semibold text-slate-900">Step 2: Microcontroller Setup</h3>
            <div>
              <label className="block text-sm font-semibold text-slate-700 mb-2">Microcontroller Code *</label>
              <input
                type="text"
                value={microcontrollerData.mcCode}
                onChange={(e) => {
                  setMicrocontrollerData({ ...microcontrollerData, mcCode: e.target.value });
                  // Clear error when user types
                  if (validationErrors.mcCode) {
                    setValidationErrors({ ...validationErrors, mcCode: undefined });
                  }
                }}
                placeholder="e.g., MC-001"
                className={`w-full rounded-xl border px-4 py-2.5 text-sm outline-none focus:ring-2 ${
                  validationErrors.mcCode
                    ? 'border-rose-300 bg-rose-50 focus:border-rose-400 focus:ring-rose-100'
                    : 'border-slate-200 bg-slate-50 focus:border-indigo-300 focus:ring-indigo-100'
                }`}
              />
              {validationErrors.mcCode && (
                <p className="mt-1 text-xs text-rose-600">{validationErrors.mcCode}</p>
              )}
            </div>
            <div>
              <label className="block text-sm font-semibold text-slate-700 mb-2">Controller Name *</label>
              <input
                type="text"
                value={microcontrollerData.name}
                onChange={(e) => {
                  setMicrocontrollerData({ ...microcontrollerData, name: e.target.value });
                  // Clear error when user types
                  if (validationErrors.mcName) {
                    setValidationErrors({ ...validationErrors, mcName: undefined });
                  }
                }}
                placeholder="e.g., Main Controller"
                className={`w-full rounded-xl border px-4 py-2.5 text-sm outline-none focus:ring-2 ${
                  validationErrors.mcName
                    ? 'border-rose-300 bg-rose-50 focus:border-rose-400 focus:ring-rose-100'
                    : 'border-slate-200 bg-slate-50 focus:border-indigo-300 focus:ring-indigo-100'
                }`}
              />
              {validationErrors.mcName && (
                <p className="mt-1 text-xs text-rose-600">{validationErrors.mcName}</p>
              )}
            </div>
          </div>
        );

      

      case 3:
        return (
          <div className="space-y-4">
            <h3 className="text-lg font-semibold text-slate-900">Step 3: Review</h3>

            <div className="rounded-xl border border-slate-200 bg-slate-50 p-4 space-y-3">
              <div>
                <div className="text-xs font-semibold text-slate-500">Parking Space</div>
                <div className="text-sm font-semibold text-slate-900">{parkingSpaceData.name}</div>
                <div className="text-xs text-slate-600">{parkingSpaceData.location}</div>
              </div>

              <div className="border-t border-slate-200 pt-3">
                <div className="text-xs font-semibold text-slate-500">Microcontroller</div>
                <div className="text-sm text-slate-900">
                  {microcontrollerData.mcCode} - {microcontrollerData.name}
                </div>
              </div>

              <div className="border-t border-slate-200 pt-3">
                <div className="text-xs font-semibold text-slate-500">Provisioning</div>
                <div className="text-sm text-slate-600">
                  Slots / Sensors / RFID will be created by the provision process after this parking space is created.
                </div>
              </div>
            </div>
          </div>
        );

      default:
        return null;
    }
  };

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
              <div className="text-sm font-semibold text-slate-900">Administration</div>
            </div>
          </div>

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
            <button
              onClick={() => navigate('/lcds')}
              className="flex w-full items-center gap-3 rounded-2xl px-3 py-2.5 text-sm text-slate-600 hover:bg-slate-100"
            >
              <Monitor className="h-4 w-4" />
              <span>LCDs</span>
            </button>
            <div className="flex items-center gap-3 rounded-2xl bg-slate-100 px-3 py-2.5 text-sm ring-1 ring-slate-200">
              <ShieldCheck className="h-4 w-4 text-slate-600" />
              <span className="text-slate-900">Admin</span>
            </div>
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
                <h1 className="mt-1 text-2xl font-bold text-slate-900">Admin Panel</h1>
              </div>

              <button
                onClick={logout}
                className="inline-flex items-center gap-2 rounded-2xl bg-slate-900 px-3 py-2 text-sm font-semibold text-white hover:bg-slate-800 lg:hidden"
              >
                <LogOut className="h-4 w-4" /> Logout
              </button>
            </div>
          </div>

          {/* Stats */}
          <div className="mt-6 grid grid-cols-1 gap-4 sm:grid-cols-3">
            <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
              <div className="flex items-start justify-between">
                <div>
                  <div className="text-xs font-semibold text-slate-500">Total Parking Spaces</div>
                  <div className="mt-2 text-3xl font-bold text-slate-900">{parkingSpaces.length}</div>
                  <div className="mt-1 text-xs text-slate-500">Active locations</div>
                </div>
                <div className="grid h-12 w-12 place-items-center rounded-2xl bg-indigo-50 text-indigo-700 ring-1 ring-indigo-200">
                  <Building2 className="h-6 w-6" />
                </div>
              </div>
            </div>

            <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
              <div className="flex items-start justify-between">
                <div>
                  <div className="text-xs font-semibold text-slate-500">System Status</div>
                  <div className="mt-2 text-2xl font-bold text-emerald-600">Online</div>
                  <div className="mt-1 text-xs text-slate-500">All systems operational</div>
                </div>
                <div className="grid h-12 w-12 place-items-center rounded-2xl bg-emerald-50 text-emerald-700 ring-1 ring-emerald-200">
                  <ShieldCheck className="h-6 w-6" />
                </div>
              </div>
            </div>

            <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
              <div className="flex items-start justify-between">
                <div>
                  <div className="text-xs font-semibold text-slate-500">Your Role</div>
                  <div className="mt-2 text-2xl font-bold text-slate-900">{user?.role?.replace('ROLE_', '') || 'User'}</div>
                  <div className="mt-1 text-xs text-slate-500">Access level</div>
                </div>
                <div className="grid h-12 w-12 place-items-center rounded-2xl bg-slate-50 text-slate-700 ring-1 ring-slate-200">
                  <ShieldCheck className="h-6 w-6" />
                </div>
              </div>
            </div>
          </div>

          {/* Parking Spaces Table */}
          <div className="mt-6 rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
            <div className="flex flex-wrap items-center justify-between gap-4 mb-5">
              <h2 className="text-lg font-semibold text-slate-900">Parking Spaces Management</h2>

              <button
                onClick={() => setShowCreateWizard(true)}
                className="inline-flex items-center gap-2 rounded-xl bg-indigo-600 px-4 py-2 text-sm font-semibold text-white hover:bg-indigo-700"
              >
                <Plus className="h-4 w-4" />
                Create New Parking Space
              </button>
            </div>

            {isLoading ? (
              <div className="flex items-center justify-center py-12">
                <div className="text-slate-500">Loading parking spaces...</div>
              </div>
            ) : parkingSpaces.length === 0 ? (
              <div className="flex flex-col items-center justify-center py-12">
                <Building2 className="h-12 w-12 text-slate-300" />
                <div className="mt-3 text-slate-900 font-semibold">No Parking Spaces</div>
                <div className="mt-1 text-sm text-slate-500">Create your first parking space to get started</div>
                <button
                  onClick={() => setShowCreateWizard(true)}
                  className="mt-4 text-sm text-indigo-600 hover:text-indigo-700"
                >
                  Create parking space
                </button>
              </div>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full">
                  <thead>
                    <tr className="border-b border-slate-200">
                      <th className="pb-3 text-left text-xs font-semibold text-slate-500">ID</th>
                      <th className="pb-3 text-left text-xs font-semibold text-slate-500">Name</th>
                      <th className="pb-3 text-left text-xs font-semibold text-slate-500">Location</th>
                      <th className="pb-3 text-left text-xs font-semibold text-slate-500">Owner</th>
                      <th className="pb-3 text-right text-xs font-semibold text-slate-500">Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {parkingSpaces.map((space) => (
                      <tr key={space.id} className="border-b border-slate-100 hover:bg-slate-50">
                        <td className="py-4 text-sm text-slate-600">#{space.id}</td>
                        <td className="py-4">
                          <div className="flex items-center gap-2">
                            <Building2 className="h-4 w-4 text-indigo-600" />
                            <span className="text-sm font-semibold text-slate-900">{space.name}</span>
                          </div>
                        </td>
                        <td className="py-4 text-sm text-slate-600">{space.location}</td>
                        <td className="py-4 text-sm text-slate-600">{space.owner}</td>
                        <td className="py-4 text-right">
                          <div className="flex items-center justify-end gap-2">
                            <button
                              onClick={() => handleEditSpace(space)}
                              className="inline-flex items-center gap-1.5 rounded-lg bg-indigo-50 px-3 py-1.5 text-xs font-semibold text-indigo-700 hover:bg-indigo-100"
                            >
                              <Edit className="h-3.5 w-3.5" />
                              Edit
                            </button>
                            <button
                              onClick={() => handleDeleteSpace(space.id, space.name)}
                              disabled={isSubmitting}
                              className="inline-flex items-center gap-1.5 rounded-lg bg-rose-50 px-3 py-1.5 text-xs font-semibold text-rose-700 hover:bg-rose-100 disabled:opacity-50 disabled:cursor-not-allowed"
                            >
                              <Trash2 className="h-3.5 w-3.5" />
                              {isSubmitting ? 'Deleting...' : 'Delete'}
                            </button>
                          </div>
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

      {/* Create Wizard Modal */}
      {showCreateWizard && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4 overflow-y-auto">
          <div className="w-full max-w-2xl rounded-2xl border border-slate-200 bg-white shadow-xl my-8 flex flex-col max-h-[90vh]">
            {/* Header - Fixed */}
            <div className="flex items-center justify-between p-6 pb-4 border-b border-slate-200">
              <h3 className="text-lg font-semibold text-slate-900">Create Parking Space Setup</h3>
              <button
                onClick={() => {
                  setShowCreateWizard(false);
                  resetWizard();
                }}
                className="rounded-lg p-1 hover:bg-slate-100"
                disabled={isSubmitting}
              >
                <X className="h-5 w-5 text-slate-500" />
              </button>
            </div>

            {/* Progress Steps - Fixed */}
            <div className="px-6 pt-4 pb-6 border-b border-slate-200">
              <div className="flex items-center justify-between">
                {[1, 2, 3].map((step) => (
                  <div key={step} className="flex items-center">
                    <div
                      className={`flex h-8 w-8 items-center justify-center rounded-full text-xs font-semibold ${
                        wizardStep >= step
                          ? 'bg-indigo-600 text-white'
                          : 'bg-slate-200 text-slate-600'
                      }`}
                    >
                      {wizardStep > step ? <Check className="h-4 w-4" /> : step}
                    </div>
                    {step < 4 && (
                      <div
                        className={`h-1 w-16 ${
                          wizardStep > step ? 'bg-indigo-600' : 'bg-slate-200'
                        }`}
                      />
                    )}
                  </div>
                ))}
              </div>
            </div>

            {/* Step Content - Scrollable */}
            <div className="flex-1 overflow-y-auto px-6 py-6">
              {renderWizardStep()}
            </div>

            {/* Navigation Buttons - Fixed at Bottom */}
            <div className="border-t border-slate-200 px-6 py-4">
              <div className="flex gap-3">
                {wizardStep > 1 && (
                  <button
                    type="button"
                    onClick={() => setWizardStep((prev) => (prev - 1) as WizardStep)}
                    className="flex-1 rounded-xl border border-slate-200 bg-white px-4 py-2.5 text-sm font-semibold text-slate-700 hover:bg-slate-50"
                    disabled={isSubmitting}
                  >
                    Back
                  </button>
                )}
                {wizardStep < 3 ? (
                  <button
                    type="button"
                    onClick={handleNextStep}
                    className="flex-1 rounded-xl bg-indigo-600 px-4 py-2.5 text-sm font-semibold text-white hover:bg-indigo-700 inline-flex items-center justify-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed"
                    disabled={isSubmitting}
                  >
                    Next <ChevronRight className="h-4 w-4" />
                  </button>
                ) : (
                  <button
                    type="button"
                    onClick={handleCreateComplete}
                    className="flex-1 rounded-xl bg-emerald-600 px-4 py-2.5 text-sm font-semibold text-white hover:bg-emerald-700 disabled:opacity-50"
                    disabled={isSubmitting}
                  >
                    {isSubmitting ? 'Creating...' : '✓ Complete Setup'}
                  </button>
                )}
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Edit Modal */}
      {showEditModal && editingSpace && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
          <div className="w-full max-w-md rounded-2xl border border-slate-200 bg-white p-6 shadow-xl">
            <div className="flex items-center justify-between mb-5">
              <h3 className="text-lg font-semibold text-slate-900">Edit Parking Space</h3>
              <button
                onClick={() => {
                  setShowEditModal(false);
                  setEditingSpace(null);
                }}
                className="rounded-lg p-1 hover:bg-slate-100"
              >
                <X className="h-5 w-5 text-slate-500" />
              </button>
            </div>

            <form onSubmit={handleUpdateSpace} className="space-y-4">
              <div>
                <label className="block text-sm font-semibold text-slate-700 mb-2">Name *</label>
                <input
                  type="text"
                  value={editFormData.name}
                  onChange={(e) => setEditFormData({ ...editFormData, name: e.target.value })}
                  placeholder="Enter name"
                  className="w-full rounded-xl border border-slate-200 bg-slate-50 px-4 py-2.5 text-sm outline-none focus:border-indigo-300 focus:ring-2 focus:ring-indigo-100"
                  required
                  minLength={3}
                  maxLength={100}
                />
              </div>

              <div>
                <label className="block text-sm font-semibold text-slate-700 mb-2">Location *</label>
                <input
                  type="text"
                  value={editFormData.location}
                  onChange={(e) => setEditFormData({ ...editFormData, location: e.target.value })}
                  placeholder="Enter location"
                  className="w-full rounded-xl border border-slate-200 bg-slate-50 px-4 py-2.5 text-sm outline-none focus:border-indigo-300 focus:ring-2 focus:ring-indigo-100"
                  required
                  minLength={3}
                  maxLength={200}
                />
              </div>

              <div className="flex gap-3 pt-2">
                <button
                  type="button"
                  onClick={() => {
                    setShowEditModal(false);
                    setEditingSpace(null);
                  }}
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
                  {isSubmitting ? 'Updating...' : 'Update'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}

