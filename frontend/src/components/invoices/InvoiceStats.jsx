import React from "react";
import {
  FileText,
  Clock3,
  Loader2,
  CircleCheckBig,
  CircleX,
} from "lucide-react";

const cards = [
  {
    key: "total",
    title: "Total Invoices",
    icon: FileText,
    color: "text-blue-400",
  },
  {
    key: "pending",
    title: "Pending",
    icon: Clock3,
    color: "text-yellow-400",
  },
  {
    key: "processing",
    title: "Processing",
    icon: Loader2,
    color: "text-cyan-400",
  },
  {
    key: "completed",
    title: "Completed",
    icon: CircleCheckBig,
    color: "text-green-400",
  },
  {
    key: "failed",
    title: "Failed",
    icon: CircleX,
    color: "text-red-400",
  },
];

const InvoiceStats = ({ stats }) => {
  return (
    <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-5">

      {cards.map((card) => {
        const Icon = card.icon;

        return (
          <div
            key={card.key}
            className="rounded-2xl border border-slate-800 bg-slate-900 p-5 transition hover:border-amber-500/40"
          >
            <div className="flex items-center justify-between">

              <div>
                <p className="text-sm text-slate-400">
                  {card.title}
                </p>

                <h2 className="mt-3 text-3xl font-bold text-white">
                  {stats?.[card.key] ?? 0}
                </h2>
              </div>

              <div className="rounded-xl bg-slate-950 p-3">
                <Icon className={`h-6 w-6 ${card.color}`} />
              </div>

            </div>
          </div>
        );
      })}

    </div>
  );
};

export default InvoiceStats;