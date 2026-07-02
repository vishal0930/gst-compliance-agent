import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Alert, Button, Card, Descriptions, Divider, Spin, Table, Tag, Input, InputNumber, Select, message } from 'antd';
import { ArrowLeft, FileText, Edit, Save, X, Activity } from 'lucide-react';
import { invoiceApi } from '../api/invoices';
import { useQuery } from '@tanstack/react-query';
import { formatCurrency, formatDate, getStatusBadge } from '../utils/formatters';
import useAuthStore from '../store/authStore';

const InvoiceDetails = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const { user } = useAuthStore();

  const [isEditing, setIsEditing] = useState(false);
  const [formValues, setFormValues] = useState(null);

  const { data: invoice, isLoading, error, refetch } = useQuery({
    queryKey: ['invoice', id],
    queryFn: () => invoiceApi.getInvoice(id),
    enabled: !!id,
  });

  // Initialize edit form values on load
  useEffect(() => {
    if (invoice) {
      setFormValues(JSON.parse(JSON.stringify(invoice)));
    }
  }, [invoice]);

  if (isLoading) {
    return (
      <div className="flex flex-col items-center justify-center h-[70vh] gap-4">
        <Spin size="large" />
        <span className="text-[var(--text-muted)] text-sm animate-pulse font-mono">Loading invoice details...</span>
      </div>
    );
  }

  if (error) {
    return (
      <Alert
        message="Error loading invoice"
        description={error.message || 'Failed to load invoice details'}
        type="error" showIcon
      />
    );
  }

  if (!invoice || !formValues) {
    return <Alert message="Invoice not found" type="warning" showIcon />;
  }

  const handleLineItemChange = (index, field, value) => {
    const newItems = [...formValues.lineItems];
    const item = { ...newItems[index] };
    
    // Set value
    item[field] = value;

    // Recalculate Taxable Value if quantity or price changes
    if (field === 'quantity' || field === 'unitPrice') {
      const qty = Number(item.quantity || 0);
      const price = Number(item.unitPrice || 0);
      item.taxableValue = Number((qty * price).toFixed(2));
    }

    // Auto-compute GST Taxes if rate, taxable, quantity, or unitPrice changes
    if (field === 'gstRate' || field === 'taxableValue' || field === 'quantity' || field === 'unitPrice') {
      const taxable = Number(item.taxableValue || 0);
      const rate = Number(item.gstRate || 0);
      
      const userGstin = user?.gstin || '';
      const vendorGstin = formValues.vendorGstin || '';
      
      const userState = userGstin.trim().substring(0, 2);
      const vendorState = vendorGstin.trim().substring(0, 2);
      
      // If GSTIN state codes match, apply CGST+SGST, otherwise apply IGST
      const isIntraState = userState && vendorState && userState === vendorState;
      
      if (isIntraState) {
        item.cgstAmount = Number((taxable * (rate / 100) / 2).toFixed(2));
        item.sgstAmount = Number((taxable * (rate / 100) / 2).toFixed(2));
        item.igstAmount = 0;
      } else {
        item.cgstAmount = 0;
        item.sgstAmount = 0;
        item.igstAmount = Number((taxable * (rate / 100)).toFixed(2));
      }
    }

    newItems[index] = item;

    // Recalculate header totals
    const totalTaxable = newItems.reduce((sum, item) => sum + Number(item.taxableValue || 0), 0);
    const totalCgst = newItems.reduce((sum, item) => sum + Number(item.cgstAmount || 0), 0);
    const totalSgst = newItems.reduce((sum, item) => sum + Number(item.sgstAmount || 0), 0);
    const totalIgst = newItems.reduce((sum, item) => sum + Number(item.igstAmount || 0), 0);
    const totalGst = totalCgst + totalSgst + totalIgst;
    const totalAmount = totalTaxable + totalGst;

    setFormValues({
      ...formValues,
      lineItems: newItems,
      taxableValue: Number(totalTaxable.toFixed(2)),
      totalGst: Number(totalGst.toFixed(2)),
      totalAmount: Number(totalAmount.toFixed(2)),
      cgstAmount: Number(totalCgst.toFixed(2)),
      sgstAmount: Number(totalSgst.toFixed(2)),
      igstAmount: Number(totalIgst.toFixed(2)),
    });
  };

  const handleSave = async () => {
    try {
      await invoiceApi.updateInvoice(id, formValues);
      message.success('Invoice reviewed and updated successfully');
      setIsEditing(false);
      refetch();
    } catch (err) {
      console.error(err);
      message.error(err.response?.data?.message || 'Failed to save invoice edits');
    }
  };

  const handleVendorGstinChange = (newGstin) => {
    const updatedGstin = newGstin || '';
    
    // Recalculate CGST/SGST/IGST for all line items based on new vendor state code
    const newItems = formValues.lineItems.map(item => {
      const updatedItem = { ...item };
      const taxable = Number(updatedItem.taxableValue || 0);
      const rate = Number(updatedItem.gstRate || 0);
      
      const userGstin = user?.gstin || '';
      const userState = userGstin.trim().substring(0, 2);
      const vendorState = updatedGstin.trim().substring(0, 2);
      
      const isIntraState = userState && vendorState && userState === vendorState;
      
      if (isIntraState) {
        updatedItem.cgstAmount = Number((taxable * (rate / 100) / 2).toFixed(2));
        updatedItem.sgstAmount = Number((taxable * (rate / 100) / 2).toFixed(2));
        updatedItem.igstAmount = 0;
      } else {
        updatedItem.cgstAmount = 0;
        updatedItem.sgstAmount = 0;
        updatedItem.igstAmount = Number((taxable * (rate / 100)).toFixed(2));
      }
      return updatedItem;
    });

    // Recalculate header totals
    const totalTaxable = newItems.reduce((sum, item) => sum + Number(item.taxableValue || 0), 0);
    const totalCgst = newItems.reduce((sum, item) => sum + Number(item.cgstAmount || 0), 0);
    const totalSgst = newItems.reduce((sum, item) => sum + Number(item.sgstAmount || 0), 0);
    const totalIgst = newItems.reduce((sum, item) => sum + Number(item.igstAmount || 0), 0);
    const totalGst = totalCgst + totalSgst + totalIgst;
    const totalAmount = totalTaxable + totalGst;

    setFormValues({
      ...formValues,
      vendorGstin: updatedGstin,
      lineItems: newItems,
      taxableValue: Number(totalTaxable.toFixed(2)),
      totalGst: Number(totalGst.toFixed(2)),
      totalAmount: Number(totalAmount.toFixed(2)),
      cgstAmount: Number(totalCgst.toFixed(2)),
      sgstAmount: Number(totalSgst.toFixed(2)),
      igstAmount: Number(totalIgst.toFixed(2)),
    });
  };

  const confidencePercentage = invoice.confidenceScore != null ? Number(invoice.confidenceScore) * 100 : 0;
  const isLowConfidence = confidencePercentage < 80;

  const lineItems = invoice.lineItems || [];
  const statusBadge = getStatusBadge(invoice.parseStatus);

  const readOnlyColumns = [
    { title: 'Description', dataIndex: 'description', key: 'description', ellipsis: true },
    { title: 'HSN Code', dataIndex: 'hsnCode', key: 'hsnCode', width: 110, className: 'font-mono' },
    {
      title: 'HSN Conf.',
      dataIndex: 'hsnConfidence',
      key: 'hsnConfidence',
      width: 110,
      align: 'center',
      render: (v) => v != null ? (
        <Tag color={Number(v) >= 0.8 ? 'green' : 'orange'} className="font-mono">
          {(Number(v) * 100).toFixed(0)}%
        </Tag>
      ) : '-',
    },
    { title: 'Qty', dataIndex: 'quantity', key: 'quantity', width: 80, align: 'right', render: (v) => Number(v || 0).toFixed(0) },
    { title: 'Unit Price', dataIndex: 'unitPrice', key: 'unitPrice', width: 120, align: 'right', render: (v) => formatCurrency(v) },
    { title: 'GST Rate', dataIndex: 'gstRate', key: 'gstRate', width: 90, align: 'center', render: (v) => v != null ? `${v}%` : '-' },
    { title: 'Taxable Value', dataIndex: 'taxableValue', key: 'taxableValue', width: 130, align: 'right', render: (v) => formatCurrency(v) },
    { title: 'CGST', dataIndex: 'cgstAmount', key: 'cgstAmount', width: 110, align: 'right', render: (v) => formatCurrency(v || 0) },
    { title: 'SGST', dataIndex: 'sgstAmount', key: 'sgstAmount', width: 110, align: 'right', render: (v) => formatCurrency(v || 0) },
    { title: 'IGST', dataIndex: 'igstAmount', key: 'igstAmount', width: 110, align: 'right', render: (v) => formatCurrency(v || 0) },
    {
      title: 'Review', dataIndex: 'needsReview', key: 'needsReview', width: 90, align: 'center',
      render: (v) => v ? <Tag color="orange">Review</Tag> : <Tag color="green">OK</Tag>,
    },
  ];

  const editableColumns = [
    {
      title: 'Description',
      dataIndex: 'description',
      key: 'description',
      width: 200,
      render: (text, record, index) => (
        <Input value={record.description} onChange={(e) => handleLineItemChange(index, 'description', e.target.value)} />
      ),
    },
    {
      title: 'HSN Code',
      dataIndex: 'hsnCode',
      key: 'hsnCode',
      width: 110,
      render: (text, record, index) => (
        <Input value={record.hsnCode} onChange={(e) => handleLineItemChange(index, 'hsnCode', e.target.value)} className="font-mono text-center" />
      ),
    },
    {
      title: 'HSN Conf.',
      dataIndex: 'hsnConfidence',
      key: 'hsnConfidence',
      width: 100,
      align: 'center',
      render: (v) => v != null ? `${(Number(v) * 100).toFixed(0)}%` : '-',
    },
    {
      title: 'Qty',
      dataIndex: 'quantity',
      key: 'quantity',
      width: 90,
      align: 'right',
      render: (text, record, index) => (
        <InputNumber value={record.quantity} onChange={(v) => handleLineItemChange(index, 'quantity', v)} className="w-full" />
      ),
    },
    {
      title: 'Unit Price',
      dataIndex: 'unitPrice',
      key: 'unitPrice',
      width: 120,
      align: 'right',
      render: (text, record, index) => (
        <InputNumber value={record.unitPrice} onChange={(v) => handleLineItemChange(index, 'unitPrice', v)} className="w-full text-right" />
      ),
    },
    {
      title: 'GST Rate',
      dataIndex: 'gstRate',
      key: 'gstRate',
      width: 100,
      align: 'center',
      render: (text, record, index) => (
        <InputNumber min={0} max={100} value={record.gstRate} onChange={(v) => handleLineItemChange(index, 'gstRate', v)} className="w-full text-center" />
      ),
    },
    {
      title: 'Taxable Value',
      dataIndex: 'taxableValue',
      key: 'taxableValue',
      width: 130,
      align: 'right',
      render: (text, record, index) => (
        <InputNumber value={record.taxableValue} onChange={(v) => handleLineItemChange(index, 'taxableValue', v)} className="w-full text-right" />
      ),
    },
    {
      title: 'CGST',
      dataIndex: 'cgstAmount',
      key: 'cgstAmount',
      width: 110,
      align: 'right',
      render: (text, record, index) => (
        <InputNumber value={record.cgstAmount} onChange={(v) => handleLineItemChange(index, 'cgstAmount', v)} className="w-full text-right" />
      ),
    },
    {
      title: 'SGST',
      dataIndex: 'sgstAmount',
      key: 'sgstAmount',
      width: 110,
      align: 'right',
      render: (text, record, index) => (
        <InputNumber value={record.sgstAmount} onChange={(v) => handleLineItemChange(index, 'sgstAmount', v)} className="w-full text-right" />
      ),
    },
    {
      title: 'IGST',
      dataIndex: 'igstAmount',
      key: 'igstAmount',
      width: 110,
      align: 'right',
      render: (text, record, index) => (
        <InputNumber value={record.igstAmount} onChange={(v) => handleLineItemChange(index, 'igstAmount', v)} className="w-full text-right" />
      ),
    },
    {
      title: 'Review',
      dataIndex: 'needsReview',
      key: 'needsReview',
      width: 110,
      align: 'center',
      render: (text, record, index) => (
        <Select
          value={record.needsReview}
          onChange={(v) => handleLineItemChange(index, 'needsReview', v)}
          options={[{ label: 'Review', value: true }, { label: 'OK', value: false }]}
          className="w-full text-center"
        />
      ),
    },
  ];

  return (
    <div className="space-y-6">
      {/* Back + Action Header */}
      <div className="flex items-center gap-4 flex-wrap">
        <Button
          icon={<ArrowLeft size={16} />}
          onClick={() => navigate('/invoices')}
          className="border-[var(--border)] text-[var(--text-secondary)] hover:text-[var(--text-primary)] bg-transparent"
        >
          Back
        </Button>
        <div className="flex items-center gap-2">
          <FileText className="text-[var(--accent)]" size={20} />
          <h1 className="text-2xl font-bold text-[var(--text-primary)]">
            {isEditing ? 'Review & Edit Invoice' : 'Invoice Details'}
          </h1>
        </div>
        
        {/* Verification score indicator */}
        <div className="flex items-center gap-2 bg-[var(--bg-input)] border border-[var(--border)] px-3 py-1 rounded-full text-xs">
          <Activity size={12} className={isLowConfidence ? 'text-[var(--yellow)]' : 'text-[var(--success)]'} />
          <span className="text-[var(--text-muted)]">Confidence Score:</span>
          <span className={`font-mono font-bold ${isLowConfidence ? 'text-[var(--yellow)]' : 'text-[var(--success)]'}`}>
            {confidencePercentage.toFixed(1)}%
          </span>
        </div>

        <div className="ml-auto flex items-center gap-2">
          {isEditing ? (
            <>
              <Button
                icon={<X size={16} />}
                onClick={() => {
                  setFormValues(JSON.parse(JSON.stringify(invoice)));
                  setIsEditing(false);
                }}
                className="border-[var(--border)] text-[var(--text-secondary)] hover:text-white bg-transparent"
              >
                Cancel
              </Button>
              <Button
                type="primary"
                icon={<Save size={16} />}
                onClick={handleSave}
              >
                Save Review
              </Button>
            </>
          ) : (
            <Button
              type="primary"
              icon={<Edit size={16} />}
              onClick={() => setIsEditing(true)}
            >
              Review Invoice
            </Button>
          )}
        </div>
      </div>

      {/* Manual Review Alert */}
      {isLowConfidence && !isEditing && (
        <Alert
          message="Manual Review Required"
          description="AI extraction confidence is below 80% or contains flag warnings. Please click 'Review Invoice' to confirm or adjust values."
          type="warning"
          showIcon
          className="rounded-2xl"
        />
      )}

      {/* Header Info Grid */}
      {isEditing ? (
        <Card className="p-4 border border-[var(--border)] bg-card shadow-sm">
          <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
            <div>
              <label className="text-[var(--text-muted)] text-xs block mb-1">Invoice Number</label>
              <Input value={formValues.invoiceNumber} onChange={(e) => setFormValues({ ...formValues, invoiceNumber: e.target.value })} className="font-mono font-semibold" />
            </div>
            <div>
              <label className="text-[var(--text-muted)] text-xs block mb-1">Invoice Date</label>
              <Input type="date" value={formValues.invoiceDate} onChange={(e) => setFormValues({ ...formValues, invoiceDate: e.target.value })} className="w-full font-semibold" />
            </div>
            <div>
              <label className="text-[var(--text-muted)] text-xs block mb-1">Supplier Name</label>
              <Input value={formValues.vendorName} onChange={(e) => setFormValues({ ...formValues, vendorName: e.target.value })} className="font-semibold" />
            </div>
            <div>
              <label className="text-[var(--text-muted)] text-xs block mb-1">Supplier GSTIN</label>
              <Input value={formValues.vendorGstin} onChange={(e) => handleVendorGstinChange(e.target.value)} className="font-mono font-semibold" />
            </div>
            <div>
              <label className="text-[var(--text-muted)] text-xs block mb-1">Taxable Value (₹)</label>
              <InputNumber value={formValues.taxableValue} onChange={(v) => setFormValues({ ...formValues, taxableValue: v })} className="w-full font-semibold" />
            </div>
            <div>
              <label className="text-[var(--text-muted)] text-xs block mb-1">Total GST (₹)</label>
              <InputNumber value={formValues.totalGst} onChange={(v) => setFormValues({ ...formValues, totalGst: v })} className="w-full font-semibold" />
            </div>
            <div>
              <label className="text-[var(--text-muted)] text-xs block mb-1">Total Amount (₹)</label>
              <InputNumber value={formValues.totalAmount} onChange={(v) => setFormValues({ ...formValues, totalAmount: v })} className="w-full font-bold text-[var(--accent)]" />
            </div>
            <div className="flex items-end pb-1">
              <Tag color={getStatusBadge(formValues.parseStatus).color} className="text-sm px-3 py-1 m-0">
                {getStatusBadge(formValues.parseStatus).text}
              </Tag>
            </div>
          </div>
        </Card>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          {/* Card 1: Supplier Details */}
          <div className="p-6 rounded-2xl border border-[var(--border)] bg-[var(--bg-card)] hover:border-[var(--accent)] hover:shadow-lg transition-all duration-300 flex flex-col justify-between">
            <div>
              <div className="flex items-center gap-3 mb-4">
                <div className="p-2.5 rounded-xl bg-[var(--accent-soft)] text-[var(--accent)]">
                  <Activity size={18} />
                </div>
                <div>
                  <h3 className="text-xs uppercase tracking-wider text-[var(--text-muted)] font-bold">Supplier Info</h3>
                  <span className="font-semibold text-[var(--text-primary)] text-lg block mt-0.5">{invoice.vendorName || '-'}</span>
                </div>
              </div>
              <div className="space-y-3 mt-4">
                <div className="flex justify-between items-center text-sm">
                  <span className="text-[var(--text-muted)]">GSTIN</span>
                  <span className="font-mono font-semibold px-2 py-0.5 bg-[var(--accent-soft)] text-[var(--accent)] rounded-lg text-xs border border-[var(--accent-soft)]">
                    {invoice.vendorGstin || '-'}
                  </span>
                </div>
                <div className="flex justify-between items-center text-sm">
                  <span className="text-[var(--text-muted)]">State Code</span>
                  <span className="font-mono font-medium text-[var(--text-primary)]">
                    {invoice.vendorGstin ? invoice.vendorGstin.substring(0, 2) : '-'}
                  </span>
                </div>
              </div>
            </div>
          </div>

          {/* Card 2: Invoice Metadata */}
          <div className="p-6 rounded-2xl border border-[var(--border)] bg-[var(--bg-card)] hover:border-blue-500/40 hover:shadow-lg transition-all duration-300 flex flex-col justify-between">
            <div>
              <div className="flex items-center gap-3 mb-4">
                <div className="p-2.5 rounded-xl bg-blue-500/10 text-blue-400">
                  <FileText size={18} />
                </div>
                <div>
                  <h3 className="text-xs uppercase tracking-wider text-[var(--text-muted)] font-bold">Invoice Details</h3>
                  <span className="font-semibold text-[var(--text-primary)] text-lg block mt-0.5 font-mono">{invoice.invoiceNumber || '-'}</span>
                </div>
              </div>
              <div className="space-y-3 mt-4">
                <div className="flex justify-between items-center text-sm">
                  <span className="text-[var(--text-muted)]">Invoice Date</span>
                  <span className="font-medium text-[var(--text-primary)]">{formatDate(invoice.invoiceDate)}</span>
                </div>
                <div className="flex justify-between items-center text-sm">
                  <span className="text-[var(--text-muted)]">Status</span>
                  <Tag color={statusBadge.color} className="font-medium m-0">{statusBadge.text}</Tag>
                </div>
              </div>
            </div>
          </div>

          {/* Card 3: Financial Summary */}
          <div className="p-6 rounded-2xl border border-[var(--border)] bg-[var(--bg-card)] hover:border-[var(--success)] hover:shadow-lg transition-all duration-300 flex flex-col justify-between">
            <div>
              <div className="flex justify-between items-start mb-2">
                <h3 className="text-xs uppercase tracking-wider text-[var(--text-muted)] font-bold">Financial Summary</h3>
                <span className="text-2xl font-extrabold text-[var(--success)] font-mono">
                  {formatCurrency(invoice.totalAmount)}
                </span>
              </div>
              <div className="h-px bg-[var(--border)] my-3" />
              <div className="space-y-2 text-xs">
                <div className="flex justify-between items-center">
                  <span className="text-[var(--text-secondary)]">Taxable Value</span>
                  <span className="font-semibold text-[var(--text-primary)]">{formatCurrency(invoice.taxableValue)}</span>
                </div>
                {invoice.cgstAmount > 0 && (
                  <div className="flex justify-between items-center text-[var(--text-muted)]">
                    <span>CGST</span>
                    <span>{formatCurrency(invoice.cgstAmount)}</span>
                  </div>
                )}
                {invoice.sgstAmount > 0 && (
                  <div className="flex justify-between items-center text-[var(--text-muted)]">
                    <span>SGST</span>
                    <span>{formatCurrency(invoice.sgstAmount)}</span>
                  </div>
                )}
                {invoice.igstAmount > 0 && (
                  <div className="flex justify-between items-center text-[var(--text-muted)]">
                    <span>IGST</span>
                    <span>{formatCurrency(invoice.igstAmount)}</span>
                  </div>
                )}
                <div className="flex justify-between items-center font-semibold pt-1 border-t border-[var(--border)]/50">
                  <span className="text-[var(--text-secondary)]">Total GST</span>
                  <span className="text-[var(--yellow)]">{formatCurrency(invoice.totalGst)}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Line Items Table */}
      <Card
        title={
          <span className="text-[var(--text-primary)] font-semibold flex items-center gap-2 text-base">
            Line Items
            <Tag color="purple" className="font-mono">{(isEditing ? formValues.lineItems : lineItems).length}</Tag>
          </span>
        }
        className="border border-[var(--border)] bg-card shadow-sm"
      >
        {(isEditing ? formValues.lineItems : lineItems).length === 0 ? (
          <p className="text-[var(--text-muted)] text-sm text-center py-8">
            {invoice.parseStatus === 'DONE'
              ? 'No line items were extracted from this invoice.'
              : 'Line items will appear here once AI processing is complete.'}
          </p>
        ) : (
          <Table
            columns={isEditing ? editableColumns : readOnlyColumns}
            dataSource={isEditing ? formValues.lineItems : lineItems}
            rowKey="id"
            pagination={false}
            size="small"
            scroll={{ x: 1300 }}
            summary={(rows) => {
              const sum = (key) =>
                rows.reduce((acc, r) => acc + Number(r[key] || 0), 0);
              return (
                <Table.Summary>
                  <Table.Summary.Row className="font-bold bg-[var(--bg-input)]">
                    <Table.Summary.Cell index={0} colSpan={isEditing ? 6 : 6}>
                      <strong className="text-[var(--text-primary)]">Total</strong>
                    </Table.Summary.Cell>
                    <Table.Summary.Cell index={isEditing ? 6 : 6} align="right">
                      <strong className="text-[var(--text-primary)]">{formatCurrency(sum('taxableValue'))}</strong>
                    </Table.Summary.Cell>
                    <Table.Summary.Cell index={isEditing ? 7 : 7} align="right">
                      <strong className="text-[var(--text-secondary)]">{formatCurrency(sum('cgstAmount'))}</strong>
                    </Table.Summary.Cell>
                    <Table.Summary.Cell index={isEditing ? 8 : 8} align="right">
                      <strong className="text-[var(--text-secondary)]">{formatCurrency(sum('sgstAmount'))}</strong>
                    </Table.Summary.Cell>
                    <Table.Summary.Cell index={isEditing ? 9 : 9} align="right">
                      <strong className="text-[var(--text-secondary)]">{formatCurrency(sum('igstAmount'))}</strong>
                    </Table.Summary.Cell>
                    <Table.Summary.Cell index={isEditing ? 10 : 10} />
                  </Table.Summary.Row>
                </Table.Summary>
              );
            }}
          />
        )}
      </Card>
    </div>
  );
};

export default InvoiceDetails;
