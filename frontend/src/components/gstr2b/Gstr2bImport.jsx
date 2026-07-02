import React, { useState } from 'react';
import { Alert, Button, Divider, Select, Steps, Tag, Upload } from 'antd';
import { FileJson, FileSpreadsheet, Upload as UploadIcon, CheckCircle2, AlertTriangle } from 'lucide-react';
import { message } from 'antd';
import { getMonthOptions, getYearOptions, formatCurrency } from '../../utils/formatters';

const { Dragger } = Upload;

/**
 * Bulk GSTR-2B import — JSON or Excel (one click = entire month's statement).
 * Replaces the old "add invoice one-by-one" form.
 */
const Gstr2bImport = ({ period, onSuccess, uploadGstr2b, uploading }) => {
    const [step, setStep] = useState(0);
    const [month, setMonth] = useState(period?.month || new Date().getMonth() + 1);
    const [year, setYear] = useState(period?.year || new Date().getFullYear());
    const [fileType, setFileType] = useState('json');
    const [parsed, setParsed] = useState(null);
    const [result, setResult] = useState(null);
    const [error, setError] = useState(null);
    const [conflict, setConflict] = useState(false);

    // ── Parse uploaded file client-side ───────────────────────────────────
    const handleFile = async (file) => {
        setError(null);
        setParsed(null);

        try {
            if (fileType === 'json') {
                const text = await file.text();
                const json = JSON.parse(text);

                // Support both { invoices: [] } and plain []
                const invoices = Array.isArray(json) ? json : json.invoices || json.data || [];

                if (!Array.isArray(invoices) || invoices.length === 0) {
                    throw new Error('No invoices found. Expected { "invoices": [...] } or a top-level array.');
                }

                setParsed({ invoices, fileName: file.name });
                setStep(1);
            } else {
                // Excel — we just hold the raw file and let the backend parse it.
                // For now show a placeholder count.
                setParsed({ rawFile: file, fileName: file.name, invoices: [] });
                setStep(1);
            }
        } catch (e) {
            setError(e.message || 'Failed to parse file');
        }

        // prevent antd auto-upload
        return false;
    };

    // ── Submit to backend — sends month+year+replace+invoices ─────────────
    const handleImport = async (replace = false) => {
        if (!parsed) return;
        setError(null);

        try {
            const payload = {
                month,
                year,
                replace,
                invoices: parsed.invoices,
            };
            const res = await uploadGstr2b(payload);
            setResult(res);
            setStep(2);
        } catch (e) {
            // 409 = period already exists — ask user whether to replace
            if (e?.status === 409) {
                setError(
                    `GSTR-2B for ${periodLabel} already exists. Click "Replace" to overwrite, ` +
                    `or cancel and choose a different period.`
                );
                setConflict(true);
            } else {
                setError(e?.message || 'Import failed');
            }
        }
    };

    const periodLabel = `${String(month).padStart(2, '0')}-${year}`;

    // ── Step 0: period + file type ─────────────────────────────────────────
    const StepPeriod = () => (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
            <p style={{ margin: 0, color: 'var(--text-secondary)', fontSize: 13 }}>
                Select the GST period for this GSTR-2B statement. One import covers the
                entire month.
            </p>

            <div style={{ display: 'flex', gap: 10 }}>
                <div style={{ flex: 1 }}>
                    <label style={{
                        display: 'block', fontSize: 11, fontWeight: 600,
                        textTransform: 'uppercase', letterSpacing: '0.06em',
                        color: 'var(--text-muted)', marginBottom: 6
                    }}>
                        Month
                    </label>
                    <Select
                        value={month} onChange={setMonth}
                        options={getMonthOptions()} style={{ width: '100%' }}
                    />
                </div>
                <div style={{ flex: 1 }}>
                    <label style={{
                        display: 'block', fontSize: 11, fontWeight: 600,
                        textTransform: 'uppercase', letterSpacing: '0.06em',
                        color: 'var(--text-muted)', marginBottom: 6
                    }}>
                        Year
                    </label>
                    <Select
                        value={year} onChange={setYear}
                        options={getYearOptions()} style={{ width: '100%' }}
                    />
                </div>
            </div>

            <div>
                <label style={{
                    display: 'block', fontSize: 11, fontWeight: 600,
                    textTransform: 'uppercase', letterSpacing: '0.06em',
                    color: 'var(--text-muted)', marginBottom: 8
                }}>
                    File Format
                </label>
                <div style={{ display: 'flex', gap: 10 }}>
                    {[
                        { key: 'json', icon: FileJson, label: 'JSON', desc: 'From GSTN portal / ERP export' },
                        { key: 'excel', icon: FileSpreadsheet, label: 'Excel', desc: '.xlsx from ClearTax / Tally' },
                    ].map(({ key, icon: Icon, label, desc }) => (
                        <div key={key}
                            onClick={() => setFileType(key)}
                            style={{
                                flex: 1, padding: 14, borderRadius: 10, cursor: 'pointer',
                                border: `2px solid ${fileType === key ? 'var(--accent)' : 'var(--border)'}`,
                                background: fileType === key ? 'var(--accent-soft)' : 'var(--bg-card)',
                                transition: 'all 0.15s',
                            }}>
                            <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 4 }}>
                                <Icon size={16} style={{ color: fileType === key ? 'var(--accent)' : 'var(--text-muted)' }} />
                                <span style={{
                                    fontWeight: 700, fontSize: 13,
                                    color: fileType === key ? 'var(--accent)' : 'var(--text-primary)'
                                }}>
                                    {label}
                                </span>
                            </div>
                            <p style={{ margin: 0, fontSize: 11, color: 'var(--text-muted)' }}>{desc}</p>
                        </div>
                    ))}
                </div>
            </div>

            <Divider style={{ margin: '4px 0', borderColor: 'var(--border)' }} />

            <Dragger
                accept={fileType === 'json' ? '.json' : '.xlsx,.xls,.csv'}
                beforeUpload={handleFile}
                showUploadList={false}
                style={{ background: 'var(--bg-input)', borderColor: 'var(--border)', borderRadius: 10 }}
            >
                <div style={{ padding: 24, textAlign: 'center' }}>
                    <UploadIcon size={32} style={{ color: 'var(--accent)', margin: '0 auto 12px' }} />
                    <p style={{ margin: 0, fontWeight: 600, fontSize: 14, color: 'var(--text-primary)' }}>
                        Drop your {fileType === 'json' ? 'JSON' : 'Excel'} file here
                    </p>
                    <p style={{ margin: '4px 0 0', fontSize: 12, color: 'var(--text-muted)' }}>
                        One file = entire {periodLabel} statement
                    </p>
                </div>
            </Dragger>

            {error && (
                <Alert type="error" showIcon message={error} style={{ fontSize: 12 }} />
            )}
        </div>
    );

    // ── Step 1: preview + confirm ──────────────────────────────────────────
    const StepPreview = () => (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
            <Alert
                type="success" showIcon
                message={
                    <span>
                        <strong>{parsed?.fileName}</strong> parsed successfully
                    </span>
                }
                description={
                    fileType === 'json'
                        ? `${parsed?.invoices?.length} invoices ready to import for ${periodLabel}`
                        : `Excel file ready to import for ${periodLabel}`
                }
            />

            {fileType === 'json' && parsed?.invoices?.length > 0 && (
                <div style={{
                    background: 'var(--bg-input)', borderRadius: 8, padding: 14,
                    border: '1px solid var(--border)'
                }}>
                    <p style={{
                        margin: '0 0 8px', fontSize: 11, fontWeight: 600,
                        textTransform: 'uppercase', letterSpacing: '0.06em', color: 'var(--text-muted)'
                    }}>
                        Preview (first 3)
                    </p>
                    {parsed.invoices.slice(0, 3).map((inv, i) => (
                        <div key={i} style={{
                            display: 'flex', justifyContent: 'space-between',
                            padding: '6px 0', borderBottom: '1px solid var(--border)',
                            fontSize: 12
                        }}>
                            <span style={{ fontFamily: 'monospace', color: 'var(--accent)' }}>
                                {inv.invoiceNumber}
                            </span>
                            <span style={{ color: 'var(--text-secondary)' }}>
                                {inv.supplierName}
                            </span>
                            <span style={{ fontWeight: 600, color: 'var(--text-primary)' }}>
                                {formatCurrency(inv.grandTotal || inv.taxableValue)}
                            </span>
                        </div>
                    ))}
                    {parsed.invoices.length > 3 && (
                        <p style={{ margin: '6px 0 0', fontSize: 11, color: 'var(--text-muted)' }}>
                            + {parsed.invoices.length - 3} more invoices…
                        </p>
                    )}
                </div>
            )}

            {error && <Alert type={conflict ? 'warning' : 'error'} showIcon message={error} style={{ fontSize: 12 }} />}

            <div style={{ display: 'flex', gap: 8, justifyContent: 'flex-end', marginTop: 8 }}>
                <Button onClick={() => { setParsed(null); setStep(0); setError(null); setConflict(false); }}>
                    Back
                </Button>
                {conflict ? (
                    <>
                        <Button danger onClick={() => handleImport(true)} loading={uploading}>
                            Replace Existing
                        </Button>
                        <Button onClick={() => { setConflict(false); setError(null); setStep(0); setParsed(null); }}>
                            Cancel
                        </Button>
                    </>
                ) : (
                    <Button type="primary" loading={uploading} icon={<UploadIcon size={14} />}
                        onClick={() => handleImport(false)}>
                        Import {parsed?.invoices?.length > 0 ? `${parsed.invoices.length} Invoices` : 'File'}
                    </Button>
                )}
            </div>
        </div>
    );

    // ── Step 2: result ─────────────────────────────────────────────────────
    const StepResult = () => (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 16, padding: '8px 0' }}>
            <div style={{ textAlign: 'center' }}>
                <CheckCircle2 size={48} style={{ color: '#22c55e', marginBottom: 12 }} />
                <h3 style={{ margin: '0 0 4px', color: 'var(--text-primary)' }}>Import Complete</h3>
                <p style={{ margin: 0, color: 'var(--text-muted)', fontSize: 13 }}>
                    {periodLabel} GSTR-2B data is ready
                </p>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: 10 }}>
                {[
                    { label: 'Total', value: result?.totalProcessed || 0, color: 'var(--text-primary)' },
                    { label: 'Imported', value: result?.successfulCount || 0, color: '#22c55e' },
                    { label: 'Skipped', value: result?.failedCount || 0, color: result?.failedCount > 0 ? '#ef4444' : 'var(--text-muted)' },
                ].map(({ label, value, color }) => (
                    <div key={label} style={{
                        textAlign: 'center', background: 'var(--bg-input)',
                        borderRadius: 8, padding: '12px 8px',
                        border: '1px solid var(--border)'
                    }}>
                        <p style={{ margin: 0, fontSize: 22, fontWeight: 800, color }}>{value}</p>
                        <p style={{ margin: '2px 0 0', fontSize: 11, color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.06em' }}>{label}</p>
                    </div>
                ))}
            </div>

            <div style={{ display: 'flex', gap: 8, justifyContent: 'flex-end' }}>
                <Button onClick={() => { setStep(0); setParsed(null); setResult(null); }}>
                    Import Another
                </Button>
                <Button type="primary" onClick={onSuccess}>
                    Done
                </Button>
            </div>
        </div>
    );

    return (
        <div style={{ padding: '4px 0' }}>
            <Steps
                current={step}
                size="small"
                style={{ marginBottom: 24 }}
                items={[
                    { title: 'Period & File' },
                    { title: 'Preview' },
                    { title: 'Done' },
                ]}
            />
            {step === 0 && <StepPeriod />}
            {step === 1 && <StepPreview />}
            {step === 2 && <StepResult />}
        </div>
    );
};

export default Gstr2bImport;
