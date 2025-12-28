import type { ReactNode } from 'react';

type Props = {
  title: string;
  value: string;
  delta?: string;
  icon?: ReactNode;
  tone?: 'indigo' | 'emerald' | 'amber' | 'rose' | 'slate';
};

const toneMap: Record<NonNullable<Props['tone']>, { ring: string; bg: string; text: string }> = {
  indigo: {
    ring: 'ring-indigo-500/10',
    bg: 'bg-indigo-500/10',
    text: 'text-indigo-700',
  },
  emerald: {
    ring: 'ring-emerald-500/10',
    bg: 'bg-emerald-500/10',
    text: 'text-emerald-700',
  },
  amber: {
    ring: 'ring-amber-500/10',
    bg: 'bg-amber-500/10',
    text: 'text-amber-700',
  },
  rose: {
    ring: 'ring-rose-500/10',
    bg: 'bg-rose-500/10',
    text: 'text-rose-700',
  },
  slate: {
    ring: 'ring-slate-200',
    bg: 'bg-slate-100',
    text: 'text-slate-700',
  },
};

export default function StatCard({ title, value, delta, icon, tone = 'slate' }: Props) {
  const t = toneMap[tone];

  return (
    <div className={`rounded-2xl border border-slate-200 bg-white p-4 shadow-sm ring-1 ${t.ring}`}>
      <div className="flex items-start justify-between gap-3">
        <div>
          <div className="text-xs font-semibold tracking-wide text-slate-500">{title}</div>
          <div className="mt-2 text-2xl font-bold text-slate-900">{value}</div>
          {delta ? <div className="mt-2 text-xs text-slate-500">{delta}</div> : null}
        </div>
        {icon ? (
          <div className={`grid h-10 w-10 place-items-center rounded-xl ${t.bg} ${t.text}`}>
            {icon}
          </div>
        ) : null}
      </div>
    </div>
  );
}

