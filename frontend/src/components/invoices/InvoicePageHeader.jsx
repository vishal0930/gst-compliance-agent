
import { FileText } from "lucide-react";
import { Select } from "antd";
import useUiStore from "../../store/uiStore";

const InvoicePageHeader = ({ period }) => {
  const gstPeriod = useUiStore((state) => state.gstPeriod);
  const setGstPeriod = useUiStore((state) => state.setGstPeriod);

  return (
    <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">

      <div className="flex items-center gap-4">

        <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-[var(--accent-soft)] border border-[var(--accent-soft)]">
          <FileText className="h-7 w-7 text-[var(--accent)]" />
        </div>

        <div>
         <h1 className="text-3xl font-bold text-[var(--text-primary)]">
          Invoice Management
         </h1>

         <p className="mt-1 text-sm text-[var(--text-secondary)]">
            Upload, process and manage GST invoices using AI-powered automation.
          </p>
        </div>

      </div>

      <div className="flex items-center gap-3">

       <div className="rounded-xl border border-[var(--border)] bg-[var(--bg-input)] px-4 py-2 shadow-sm">
          <p className="text-xs uppercase tracking-wide text-[var(--text-muted)]">
            GST Period
          </p>

          <div className="flex items-center gap-2 mt-1">
            <Select
              value={gstPeriod.month}
              onChange={(value) => setGstPeriod(value, gstPeriod.year)}
              className="w-28"
              size="small"
            >
              {Array.from({ length: 12 }, (_, i) => (
                <Select.Option key={i + 1} value={i + 1}>
                  {new Date(0, i).toLocaleString('en', { month: 'short' })}
                </Select.Option>
              ))}
            </Select>
            <Select
              value={gstPeriod.year}
              onChange={(value) => setGstPeriod(gstPeriod.month, value)}
              className="w-20"
              size="small"
            >
              {Array.from({ length: 10 }, (_, i) => {
                const year = new Date().getFullYear() - 5 + i;
                return (
                  <Select.Option key={year} value={year}>
                    {year}
                  </Select.Option>
                );
              })}
            </Select>
          </div>
        </div>

      </div>

    </div>
  );
};

export default InvoicePageHeader;