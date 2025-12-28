type Slot = {
  id: string;
  label: string;
  occupied: boolean;
};

type Props = {
  title?: string;
  slots: Slot[];
};

export default function OccupancyGrid({ title = 'Live Slots', slots }: Props) {
  return (
    <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
      <div className="flex items-center justify-between">
        <h3 className="text-sm font-semibold text-slate-900">{title}</h3>
        <div className="text-xs text-slate-500">Updated just now</div>
      </div>

      <div className="mt-4 grid grid-cols-4 gap-3 sm:grid-cols-6 lg:grid-cols-4">
        {slots.map((s) => (
          <div
            key={s.id}
            className={
              'rounded-xl border p-3 text-center ' +
              (s.occupied ? 'border-rose-200 bg-rose-50' : 'border-emerald-200 bg-emerald-50')
            }
          >
            <div className="text-xs font-semibold text-slate-700">{s.label}</div>
            <div className={'mt-1 text-[11px] ' + (s.occupied ? 'text-rose-700' : 'text-emerald-700')}>
              {s.occupied ? 'Occupied' : 'Available'}
            </div>
          </div>
        ))}
      </div>

      <div className="mt-4 flex flex-wrap items-center gap-3 text-xs text-slate-500">
        <span className="inline-flex items-center gap-2">
          <span className="h-2 w-2 rounded-full bg-emerald-500" /> Available
        </span>
        <span className="inline-flex items-center gap-2">
          <span className="h-2 w-2 rounded-full bg-rose-500" /> Occupied
        </span>
      </div>
    </div>
  );
}

