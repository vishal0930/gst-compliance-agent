import React from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Card, Button, Spin, Descriptions, Alert } from 'antd';
import { ArrowLeft, FileText } from 'lucide-react';
import { invoiceApi } from '../api/invoices';
import { useQuery } from '@tanstack/react-query';

const InvoiceDetails = () => {
  const { id } = useParams();
  const navigate = useNavigate();

  const { data: invoice, isLoading, error } = useQuery({
    queryKey: ['invoice', id],
    queryFn: () => invoiceApi.getInvoice(id),
    enabled: !!id,
  });

  if (isLoading) {
    return (
      <div className="flex flex-col items-center justify-center h-[70vh] gap-4">
        <Spin size="large" />
        <span className="text-slate-400 text-sm">Loading invoice details...</span>
      </div>
    );
  }

  if (error) {
    return (
      <div className="p-4">
        <Alert
          message="Error loading invoice"
          description={error.message || 'Failed to load invoice details'}
          type="error"
          showIcon
        />
      </div>
    );
  }

  if (!invoice) {
    return (
      <div className="p-4">
        <Alert
          message="Invoice not found"
          description="The requested invoice could not be found"
          type="warning"
          showIcon
        />
      </div>
    );
  }

  return (
    <div className="p-4 space-y-6">
      <div className="flex items-center gap-4 mb-4">
        <Button
          icon={<ArrowLeft size={16} />}
          onClick={() => navigate('/invoices')}
          className="border-slate-700 text-slate-300 hover:text-white"
        >
          Back to Invoices
        </Button>
        <div className="flex items-center gap-2">
          <FileText className="text-amber-400" size={20} />
          <h1 className="text-2xl font-bold text-white">Invoice Details</h1>
        </div>
      </div>

      <Card className="bg-slate-900 border-slate-800">
        <Descriptions
          title="Invoice Information"
          bordered
          column={{ xs: 1, sm: 1, md: 2 }}
          className="text-slate-300"
        >
          <Descriptions.Item label="Invoice Number">{invoice.invoiceNumber || 'N/A'}</Descriptions.Item>
          <Descriptions.Item label="Invoice Date">{invoice.invoiceDate || 'N/A'}</Descriptions.Item>
          <Descriptions.Item label="Supplier GSTIN">{invoice.supplierGstin || 'N/A'}</Descriptions.Item>
          <Descriptions.Item label="Invoice Value">{invoice.invoiceValue || 'N/A'}</Descriptions.Item>
          <Descriptions.Item label="Taxable Value">{invoice.taxableValue || 'N/A'}</Descriptions.Item>
          <Descriptions.Item label="IGST">{invoice.igst || 'N/A'}</Descriptions.Item>
          <Descriptions.Item label="CGST">{invoice.cgst || 'N/A'}</Descriptions.Item>
          <Descriptions.Item label="SGST">{invoice.sgst || 'N/A'}</Descriptions.Item>
          <Descriptions.Item label="Status">
            <span className={`px-2 py-1 rounded text-xs font-medium ${
              invoice.status === 'MATCHED' ? 'bg-green-500/20 text-green-400' :
              invoice.status === 'MISMATCH' ? 'bg-red-500/20 text-red-400' :
              'bg-slate-500/20 text-slate-400'
            }`}>
              {invoice.status || 'N/A'}
            </span>
          </Descriptions.Item>
          <Descriptions.Item label="Parse Status">{invoice.parseStatus || 'N/A'}</Descriptions.Item>
        </Descriptions>
      </Card>
    </div>
  );
};

export default InvoiceDetails;