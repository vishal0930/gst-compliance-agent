import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import { UploadCloud, FileText } from "lucide-react";
import { message, Progress } from "antd";
import { invoiceApi } from "../../api/invoices";

const InvoiceUpload = ({ onUploadSuccess }) => {
  const navigate = useNavigate();

  const [file, setFile] = useState(null);
  const [uploading, setUploading] = useState(false);
  const [progress, setProgress] = useState(0);

  const handleFileChange = (e) => {
    if (e.target.files.length === 0) return;

    setFile(e.target.files[0]);
  };

  const handleUpload = async () => {
    if (!file) {
      message.warning("Please select an invoice.");
      return;
    }

    try {
      setUploading(true);
      setProgress(15);

      const response = await invoiceApi.uploadInvoices(file, (percent) => {
        setProgress(percent);
      });

      setProgress(100);

      message.success("Invoice uploaded successfully.");

      onUploadSuccess?.();

      /*
        Expected backend response:

        {
          jobId: "...",
          invoiceId: "..."
        }
      */

      if (response?.jobId) {
        navigate(`/processing/${response.jobId}`);
      } else {
        navigate("/invoices");
      }
    } catch (err) {
      console.error(err);
      message.error("Upload failed.");
    } finally {
      setUploading(false);
      setTimeout(() => setProgress(0), 1200);
    }
  };

  return (
    <div className="p-4 md:p-8">

      <div className="max-w-3xl mx-auto">

        <div
          className="
            border-2
            border-dashed
            border-[var(--border)]
            rounded-2xl
            p-12
            bg-[var(--bg-input)]
            text-center
            shadow-inner
          "
        >
          <UploadCloud
            size={56}
            className="mx-auto text-[var(--accent)]"
          />

          <h2 className="mt-6 text-2xl font-bold text-[var(--text-primary)]">
            Upload GST Invoice
          </h2>

          <p className="mt-2 text-[var(--text-secondary)]">
            Upload PDF, PNG or JPG invoices for AI processing.
          </p>

          <label
            htmlFor="invoice-file"
            className="
              mt-8
              inline-flex
              cursor-pointer
              rounded-xl
              bg-[var(--accent)]
              px-6
              py-3
              font-semibold
              text-white
              hover:bg-[var(--accent-hover)]
              transition
              shadow-md
              shadow-[rgba(139,92,246,0.15)]
            "
          >
            Choose File
          </label>

          <input
            id="invoice-file"
            type="file"
            accept=".pdf,.png,.jpg,.jpeg"
            hidden
            onChange={handleFileChange}
          />

          {file && (
            <div className="mt-8 flex items-center justify-center gap-3 rounded-xl border border-[var(--border)] bg-card p-4">

              <FileText
                size={22}
                className="text-[var(--accent)]"
              />

              <span className="text-[var(--text-primary)]">
                {file.name}
              </span>

            </div>
          )}

          {uploading && (
            <div className="mt-8">

              <Progress
                percent={progress}
                strokeColor="var(--accent)"
              />

              <p className="mt-3 text-sm text-[var(--text-secondary)]">
                Uploading invoice...
              </p>

            </div>
          )}

          <button
            onClick={handleUpload}
            disabled={!file || uploading}
            className="
              mt-8
              w-full
              rounded-xl
              bg-[var(--accent)]
              py-3
              font-semibold
              text-white
              transition
              hover:bg-[var(--accent-hover)]
              disabled:cursor-not-allowed
              disabled:opacity-50
              shadow-md
              shadow-[rgba(139,92,246,0.15)]
            "
          >
            {uploading
              ? "Uploading..."
              : "Start AI Processing"}
          </button>

        </div>

      </div>

    </div>
  );
};

export default InvoiceUpload;