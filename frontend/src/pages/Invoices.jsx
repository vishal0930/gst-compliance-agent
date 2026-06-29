import React, { useMemo, useState } from "react";
import { Tabs } from "antd";
import { FileText, Upload } from "lucide-react";

import InvoiceList from "../components/invoices/InvoiceList";
import InvoiceUpload from "../components/invoices/InvoiceUpload";
import InvoicePageHeader from "../components/invoices/InvoicePageHeader";
import InvoiceStats from "../components/invoices/InvoiceStats";

import { useInvoices } from "../hooks/useInvoices";

const Invoices = () => {
  const [activeTab, setActiveTab] = useState("list");

  const {
    invoices = [],
    loading,
    refetch,
  } = useInvoices();

  const stats = useMemo(() => {
    const total = invoices.length;

    const pending = invoices.filter(
      (i) => i.parseStatus === "PENDING" || i.parseStatus === "PROCESSING"
    ).length;

    const processing = invoices.filter(
      (i) => i.parseStatus === "PROCESSING"
    ).length;

    const completed = invoices.filter(
      (i) => i.parseStatus === "DONE" || i.parseStatus === "MATCHED"
    ).length;

    const failed = invoices.filter(
      (i) => i.parseStatus === "FAILED"
    ).length;

    return {
      total,
      pending,
      processing,
      completed,
      failed,
    };
  }, [invoices]);

  const items = [
    {
      key: "list",
      label: (
        <div className="flex items-center gap-2">
          <FileText size={16} />
          <span>All Invoices</span>
        </div>
      ),
      children: (
        <InvoiceList
          invoices={invoices}
          loading={loading}
          refetch={refetch}
        />
      ),
    },
    {
      key: "upload",
      label: (
        <div className="flex items-center gap-2">
          <Upload size={16} />
          <span>Upload Invoice</span>
        </div>
      ),
      children: (
        <InvoiceUpload
          onUploadSuccess={() => {
            refetch();
            setActiveTab("list");
          }}
        />
      ),
    },
  ];

  return (
    <div className="space-y-6">

      <InvoicePageHeader />

      <InvoiceStats stats={stats} />

      <div className="rounded-2xl border border-slate-800 bg-slate-900">

        <Tabs
          activeKey={activeTab}
          onChange={setActiveTab}
          items={items}
          className="enterprise-tabs"
        />

      </div>

    </div>
  );
};

export default Invoices;