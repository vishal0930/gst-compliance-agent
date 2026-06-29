import React, { useEffect, useRef, useState } from "react";
import { Alert, Card, Result, Spin, Tag } from "antd";
import { BrainCircuit, Loader2 } from "lucide-react";
import { useNavigate, useParams } from "react-router-dom";
import { invoiceApi } from "../api/invoices";

const STATUS_COLOR = {
  PENDING: "default",
  PROCESSING: "processing",
  COMPLETED: "success",
  FAILED: "error",
};

const Processing = () => {
  const { jobId } = useParams();
  const navigate = useNavigate();

  const intervalRef = useRef(null);

  const [loading, setLoading] = useState(true);
  const [job, setJob] = useState(null);
  const [error, setError] = useState(null);

  const loadJob = async () => {
    try {
      const data = await invoiceApi.getJobStatus(jobId);

      setJob(data);
      setLoading(false);
      setError(null);

      if (data.completed || data.status === 'COMPLETED') {
        clearInterval(intervalRef.current);

        setTimeout(() => {
          navigate("/invoices", {
            state: { refresh: true },
          });
        }, 1500);
      }
    } catch (err) {
      console.error(err);
      setLoading(false);
      setError("Unable to fetch processing status.");
      clearInterval(intervalRef.current);
    }
  };

  useEffect(() => {
    loadJob();

    intervalRef.current = setInterval(loadJob, 2000);

    return () => {
      clearInterval(intervalRef.current);
    };
  }, [jobId]);

  return (
    <div className="max-w-4xl mx-auto p-4 space-y-6">
      <Card className="bg-slate-900 border-slate-800">
        <div className="flex items-center gap-4">
          <div className="h-14 w-14 rounded-xl bg-amber-500/10 flex items-center justify-center">
            <BrainCircuit size={30} className="text-amber-400" />
          </div>
          <div>
            <h1 className="text-2xl font-bold text-white">AI Invoice Processing</h1>
            <p className="text-slate-400">Your invoice is being processed.</p>
          </div>
        </div>
      </Card>

      {loading && (
        <Card className="bg-slate-900 border-slate-800">
          <div className="flex items-center gap-4 py-6">
            <Spin />
            <span className="text-slate-300">Loading processing status...</span>
          </div>
        </Card>
      )}

      {error && (
        <Alert type="error" showIcon message={error} />
      )}

      {!loading && !error && job && (
        <Card className="bg-slate-900 border-slate-800">
          <div className="space-y-5">
            <div className="flex items-center justify-between">
              <span className="text-slate-400">Job ID</span>
              <span className="text-white font-mono">{job.jobId || jobId}</span>
            </div>

            <div className="flex items-center justify-between">
              <span className="text-slate-400">Status</span>
              <Tag color={STATUS_COLOR[job.status]}>{job.status}</Tag>
            </div>

            <div className="flex items-center justify-between">
              <span className="text-slate-400">Completed</span>
              <span className="text-white">
                {job.completed || job.status === 'COMPLETED' ? "Yes" : "No"}
              </span>
            </div>

            {!job.completed && job.status !== 'COMPLETED' && (
              <div className="flex items-center gap-3 pt-4">
                <Loader2 className="animate-spin text-amber-400" size={22} />
                <span className="text-slate-300">AI agents are processing the invoice...</span>
              </div>
            )}

            {(job.completed || job.status === 'COMPLETED') && (
              <Result
                status="success"
                title="Invoice Processing Completed"
                subTitle="Redirecting to Invoice Management..."
              />
            )}
          </div>
        </Card>
      )}
    </div>
  );
};

export default Processing;