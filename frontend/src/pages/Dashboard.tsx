import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../services/api';
import { Upload, AlertCircle, FileText, CheckCircle, ArrowRight } from 'lucide-react';

interface Group {
  id: string;
  name: string;
}

export const Dashboard: React.FC = () => {
  const navigate = useNavigate();
  const [group, setGroup] = useState<Group | null>(null);
  const [file, setFile] = useState<File | null>(null);
  const [uploading, setUploading] = useState<boolean>(false);
  const [error, setError] = useState<string>('');
  const [success, setSuccess] = useState<string>('');

  useEffect(() => {
    fetchOrCreateGroup();
  }, []);

  const fetchOrCreateGroup = async () => {
    try {
      const response = await api.get('/api/groups');
      const groupsList = response.data;
      if (groupsList && groupsList.length > 0) {
        setGroup(groupsList[0]);
      } else {
        const createResponse = await api.post('/api/groups', { name: 'Roommates Group' });
        setGroup(createResponse.data);
      }
    } catch (err: any) {
      setError('Failed to configure active group space.');
    }
  };

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files.length > 0) {
      setFile(e.target.files[0]);
      setError('');
      setSuccess('');
    }
  };

  const handleUploadSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!file || !group) return;

    setUploading(true);
    setError('');
    setSuccess('');

    const formData = new FormData();
    formData.append('file', file);
    formData.append('groupId', group.id);

    try {
      const response = await api.post('/api/imports/upload', formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
      });
      setSuccess('CSV successfully uploaded and analyzed!');
      navigate('/import-report', { state: { report: response.data } });
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to process CSV file. Verify format.');
    } finally {
      setUploading(false);
    }
  };

  return (
    <div className="container space-y-6">
      {/* Welcome & Group Banner */}
      <div className="card flex justify-between items-center" style={{ padding: '24px 32px' }}>
        <div>
          <h1 style={{ fontSize: '24px', fontWeight: 800 }}>Active Workspace</h1>
          <p className="text-muted" style={{ fontSize: '13px', marginTop: '4px' }}>
            reconcile debts, identify duplicates, and audit splits
          </p>
        </div>
        {group && (
          <div className="alert-success" style={{ padding: '8px 16px', borderRadius: '6px', fontSize: '13px', margin: 0, fontWeight: 600 }}>
            Group: {group.name}
          </div>
        )}
      </div>

      {error && (
        <div className="alert alert-error">
          <AlertCircle size={18} />
          <span>{error}</span>
        </div>
      )}

      {success && (
        <div className="alert alert-success">
          <CheckCircle size={18} />
          <span>{success}</span>
        </div>
      )}

      <div className="grid grid-cols-lg-3">
        {/* Upload Column */}
        <div className="card flex flex-col justify-between">
          <div>
            <h3 className="card-title">
              <FileText className="text-emerald" size={20} />
              <span>Import Shared Expenses CSV</span>
            </h3>
            <p className="card-subtitle">
              Upload the export ledger. The auditor engine will parse columns, inspect precision, and flag duplicates or split discrepancies in a review wizard.
            </p>

            <form onSubmit={handleUploadSubmit} className="space-y-4">
              <div className="drop-zone">
                <input
                  type="file"
                  accept=".csv"
                  onChange={handleFileChange}
                />
                <Upload size={32} className="text-muted" />
                <span className="font-semibold" style={{ fontSize: '14px' }}>
                  {file ? file.name : 'Choose CSV file to upload'}
                </span>
                <span className="text-muted" style={{ fontSize: '11px' }}>Only .csv files supported</span>
              </div>

              {file && (
                <button
                  type="submit"
                  disabled={uploading}
                  className="btn btn-primary"
                >
                  {uploading ? 'Processing File...' : 'Begin Stage Audit'}
                </button>
              )}
            </form>
          </div>
        </div>

        {/* Quick Actions Column */}
        <div className="card flex flex-col justify-between">
          <div>
            <h3 className="card-title" style={{ marginBottom: '16px' }}>Quick Links</h3>
            <div className="space-y-4">
              <button
                onClick={() => navigate('/expenses')}
                className="btn btn-secondary flex justify-between items-center"
                style={{ padding: '16px', textAlign: 'left' }}
              >
                <div>
                  <h4 className="font-semibold" style={{ fontSize: '14px' }}>Expenses List</h4>
                  <p className="text-muted" style={{ fontSize: '11px', marginTop: '2px' }}>View and filter historical logs</p>
                </div>
                <ArrowRight size={16} className="text-muted" />
              </button>

              <button
                onClick={() => navigate('/expenses/create')}
                className="btn btn-secondary flex justify-between items-center"
                style={{ padding: '16px', textAlign: 'left' }}
              >
                <div>
                  <h4 className="font-semibold" style={{ fontSize: '14px' }}>Log Expense</h4>
                  <p className="text-muted" style={{ fontSize: '11px', marginTop: '2px' }}>Manually record a shared split</p>
                </div>
                <ArrowRight size={16} className="text-muted" />
              </button>

              <button
                onClick={() => navigate('/balances')}
                className="btn btn-secondary flex justify-between items-center"
                style={{ padding: '16px', textAlign: 'left' }}
              >
                <div>
                  <h4 className="font-semibold" style={{ fontSize: '14px' }}>Balance Summary</h4>
                  <p className="text-muted" style={{ fontSize: '11px', marginTop: '2px' }}>View simplified debt payments</p>
                </div>
                <ArrowRight size={16} className="text-muted" />
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
