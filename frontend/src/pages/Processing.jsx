import React, { useEffect, useRef, useState } from 'react';
import { Alert, Card, Progress, Result, Steps, Tag } from 'antd';
import {
  BrainCircuit, FileSearch, Layers, RefreshCw,
  CalendarClock, ClipboardList, CheckCircle2, XCircle, Loader2,
} from 'lucide-react';
import { useNavigate, useParams } from 'react-router-dom';
import { invoiceApi } from '../api/invoices';

const PIPELINE_STEPS = [
  { key: 'PARSING', label: 'Invoice Parsing', icon: <FileSearch size={16} />, desc: 'Extracting text via OCR + Tika' },
  { key: 'CLASSIFYING', label: 'HSN Classification', icon: <Layers size={16} />, desc: 'Matching line items to HSN master via RAG' },
  { key: 'RECONCILING', label: 'GSTR-2B Reconciliation', icon: <RefreshCw size={16} />, desc: 'Comparing books with portal data' },
  { key: 'DEADLINE_TRACKING', label: 'Deadline Tracking', icon: <CalendarClock size={16} />, desc: 'Calculating filing deadlines and penalties' },
  { key: 'DRAFTING', label: 'Return Drafting', icon: <ClipboardList size={16} />, desc: 'Generating AI compliance brief' },
  { key: 'COMPLETED', label: 'Complete', icon: <CheckCircle2 size={16} />, desc: 'All agents finished' },
];

const STATUS_ORDER = ['PENDING', 'PARSING', 'CLASSIFYING', 'RECONCILING', 'DEADLINE_TRACKING', 'DRAFTING', 'COMPLETED'];

function getCurrentStepIndex(status) {
  const idx = STATUS_ORDER.indexOf(status);
  return idx < 0 ? 0 : Math.max(0, idx - 1); // Steps component is 0-based after PENDING
}

function stepStatus(stepKey, currentStatus, isFailed) {
  const current = STATUS_ORDER.indexOf(currentStatus);
  const step = STATUS_ORDER.indexOf(stepKey);
  if (isFailed && current === step) return 'error';
  if (step < current) return 'finish';
  if (step === current) return 'process';
  return 'wait';
}

const Processing = () => {
  const { jobId } = useParams();
  const navigate = useNavigate();
  const intervalRef = useRef(null);

  const [job, setJob] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const loadJob = async () => {
    try {
      const data = await invoiceApi.getJobStatus(jobId);
      setJob(data);
      setLoading(false);
      setError(null);

      const done = data.completed || data.status === 'COMPLETED' || data.status === 'FAILED';
      if (done) {
        clearInterval(intervalRef.current);
        if (data.status !== 'FAILED') {
          setTimeout(() => navigate('/invoices', { state: { refresh: true } }), 2000);
        }
      }
    } catch (err) {
      setLoading(false);
      setError('Unable to fetch processing status.');
      clearInterval(intervalRef.current);
    }
  };

  useEffect(() => {
    loadJob();
    intervalRef.current = setInterval(loadJob, 2500);
    return () => clearInterval(intervalRef.current);
  }, [jobId]);

  const isFailed = job?.status === 'FAILED';
  const isComplete = job?.status === 'COMPLETED' || job?.completed;
  const currentIdx = job ? getCurrentStepIndex(job.status) : 0;

  const pipelineProgress = isComplete ? 100 :
    isFailed ? (currentIdx / PIPELINE_STEPS.length) * 100 :
      Math.round(((currentIdx + 0.5) / PIPELINE_STEPS.length) * 100);

  const steps = PIPELINE_STEPS.map((s) => ({
    title: s.label,
    description: s.desc,
    icon: s.icon,
    status: job ? stepStatus(s.key, job.status, isFailed) : 'wait',
  }));

  return (
    <div className="max-w-3xl mx-auto space-y-6">
      {/* Header */}
      <Card className="bg-slate-900 border-slate-800">
        <div className="flex items-center gap-4">
          <div className="h-14 w-14 rounded-xl bg-amber-500/10 flex items-center justify-center">
            <BrainCircuit size={30} className="text-amber-400" />
          </div>
          <div>
            <h1 className="text-2xl font-bold text-white">AI Invoice Pipeline</h1>
            <p className="text-slate-400 text-sm">
              Job <span className="font-mono text-amber-400">{jobId}</span>
            </p>
          </div>
          {job && (
            <div className="ml-auto">
              <Tag color={isComplete ? 'green' : isFailed ? 'red' : 'processing'}>
                {job.status}
              </Tag>
            </div>
          )}
        </div>
      </Card>

      {error && <Alert type="error" showIcon message={error} />}

      {/* Progress bar */}
      {!error && (
        <Card className="bg-slate-900 border-slate-800">
          <p className="text-slate-400 text-xs mb-3 uppercase tracking-widest">
            Overall Pipeline Progress
          </p>
          <Progress
            percent={pipelineProgress}
            strokeColor={isFailed ? '#ef4444' : { '0%': '#f59e0b', '100%': '#22c55e' }}
            trailColor="#1e293b"
            status={isFailed ? 'exception' : isComplete ? 'success' : 'active'}
          />
        </Card>
      )}

      {/* Step-by-step pipeline */}
      {!loading && !error && job && (
        <Card className="bg-slate-900 border-slate-800">
          <Steps
            direction="vertical"
            current={isComplete ? PIPELINE_STEPS.length : currentIdx}
            status={isFailed ? 'error' : 'process'}
            items={steps}
            className="px-2 py-2"
          />
        </Card>
      )}

      {/* Loading skeleton */}
      {loading && (
        <Card className="bg-slate-900 border-slate-800">
          <div className="flex items-center gap-4 py-6">
            <Loader2 className="animate-spin text-amber-400" size={24} />
            <span className="text-slate-300">Connecting to pipeline...</span>
          </div>
        </Card>
      )}

      {/* Error detail */}
      {isFailed && job?.errors?.length > 0 && (
        <Card className="bg-slate-900 border-slate-800" title={
          <span className="flex items-center gap-2 text-red-400">
            <XCircle size={16} /> Pipeline Errors
          </span>
        }>
          <ul className="list-disc pl-5 space-y-1">
            {job.errors.map((e, i) => (
              <li key={i} className="text-red-300 text-sm font-mono">{e}</li>
            ))}
          </ul>
        </Card>
      )}

      {/* Completion */}
      {isComplete && (
        <Result
          status="success"
          title="Invoice Processed Successfully"
          subTitle="Redirecting to your invoice list..."
        />
      )}
    </div>
  );
};

export default Processing;
