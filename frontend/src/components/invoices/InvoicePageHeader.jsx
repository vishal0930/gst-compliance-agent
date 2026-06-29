import React from "react";
import { FileText } from "lucide-react";

const InvoicePageHeader = () => {
  return (
    <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">

      <div className="flex items-center gap-4">

        <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-amber-500/10 border border-amber-500/20">
          <FileText className="h-7 w-7 text-amber-400" />
        </div>

        <div>
          <h1 className="text-3xl font-bold text-white">
            Invoice Management
          </h1>

          <p className="mt-1 text-sm text-slate-400">
            Upload, process and manage GST invoices using AI-powered automation.
          </p>
        </div>

      </div>

      <div className="flex items-center gap-3">

        <div className="rounded-xl border border-slate-800 bg-slate-900 px-4 py-2">
          <p className="text-xs uppercase tracking-wide text-slate-500">
            GST Period
          </p>

          <p className="font-semibold text-white">
            June 2026
          </p>
        </div>

      </div>

    </div>
  );
};

export default InvoicePageHeader;