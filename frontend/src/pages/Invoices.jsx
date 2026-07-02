import React, { useMemo, useState } from "react";
import { Tabs, Button } from "antd";
import { 
  FileText, 
  Upload, 
  Clock, 
  CheckCircle2, 
  AlertCircle, 
  Activity, 
  Layers 
} from "lucide-react";

import InvoiceList from "../components/invoices/InvoiceList";
import InvoiceUpload from "../components/invoices/InvoiceUpload";
import InvoicePageHeader from "../components/invoices/InvoicePageHeader";
import InvoiceStats from "../components/invoices/InvoiceStats";
import { useInvoices } from "../hooks/useInvoices";
import useUiStore from "../store/uiStore";

import { formatTaxPeriod } from "../utils/formatters";

const Invoices = () => {

  const [activeTab, setActiveTab] = useState("list");
  const gstPeriod = useUiStore((state) => state.gstPeriod);

const periodLabel = useMemo(
  () => formatTaxPeriod(gstPeriod),
  [gstPeriod]
);
 
  
  

  const {
    invoices = [],
    loading,
    refetch,
  } = useInvoices();
 
  

  // Memoized stats calculation with the missing 'failed' variable implemented
  const stats = useMemo(() => {
    const total = invoices.length;
    
    const pending = invoices.filter(
      (i) => i.parseStatus === "PENDING"
    ).length;

    const processing = invoices.filter(
      (i) => i.parseStatus === "PROCESSING"
    ).length;

    const completed = invoices.filter(
      (i) => i.parseStatus === "DONE" || i.parseStatus === "MATCHED"
    ).length;

    const failed = invoices.filter(
      (i) => i.parseStatus === "FAILED" || i.parseStatus === "ERROR"
    ).length;

    return {
      total,
      pending,
      processing,
      completed,
      failed,
    };
  }, [invoices]);

  // Configuration for Ant Design Tabs
  const items = [
    {
      key: "list",
      label: (
        <div className="flex items-center gap-2">
          <FileText size={16} className="text-blue-400" />
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
          <Upload size={16} className="text-emerald-400" />
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
      {/* Header Section */}
      <div className="flex items-center justify-between bg-card p-6 rounded-2xl border border-[var(--border)] shadow-sm">
        <InvoicePageHeader period={periodLabel} />
        <Button
          type="primary"
          icon={<Upload size={16} />}
          onClick={() => setActiveTab("upload")}
          className="flex items-center gap-2 h-10 px-4 rounded-xl font-semibold"
        >
          Upload Invoice
        </Button>
      </div>

      {/* Stats Overview */}
      <InvoiceStats stats={stats} />

      {/* Tabs Container */}
      <div className="rounded-2xl border border-[var(--border)] bg-card p-6 shadow-sm">
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