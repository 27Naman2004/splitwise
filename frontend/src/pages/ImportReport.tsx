import React, { useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import api from '../services/api';
import { AlertTriangle, CheckCircle, Settings2, ShieldCheck, ArrowLeft } from 'lucide-react';

interface Issue {
  id: string;
  rowNumber: number;
  anomalyType: string;
  severity: string;
  description: string;
  originalData: string;
  actionTaken: string;
}

interface ImportJobReport {
  id: string;
  status: string;
  fileName: string;
  issues: Issue[];
  issuesCountSummary: Record<string, number>;
}

export const ImportReport: React.FC = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const reportData = location.state?.report as ImportJobReport;

  const [report] = useState<ImportJobReport | null>(reportData || null);
  const [resolutions, setResolutions] = useState<Record<string, { action: string; customData: Record<string, any> }>>({});
  const [submitting, setSubmitting] = useState<boolean>(false);
  const [error, setError] = useState<string>('');

  if (!report) {
    return (
      <div className="container text-center">
        <h2 className="font-bold" style={{ fontSize: '24px' }}>No Import Report Active</h2>
        <button
          onClick={() => navigate('/')}
          className="btn btn-primary mt-4"
          style={{ width: 'auto' }}
        >
          Return to Dashboard
        </button>
      </div>
    );
  }

  const handleActionChange = (issueId: string, action: string, customDataKey?: string, customDataValue?: any) => {
    setResolutions((prev) => {
      const existing = prev[issueId] || { action, customData: {} };
      const updatedCustomData = customDataKey
        ? { ...existing.customData, [customDataKey]: customDataValue }
        : existing.customData;
      return {
        ...prev,
        [issueId]: {
          action,
          customData: updatedCustomData,
        },
      };
    });
  };

  const handleResolveSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setSubmitting(true);
    setError('');

    const payload = {
      resolutions: Object.entries(resolutions).map(([anomalyId, item]) => ({
        anomalyId,
        action: item.action,
        customData: item.customData,
      })),
    };

    try {
      await api.post(`/api/imports/${report.id}/resolve`, payload);
      alert('Resolutions applied successfully! Import committed.');
      navigate('/expenses');
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to submit resolution steps. Verify details.');
    } finally {
      setSubmitting(false);
    }
  };

  const criticalIssues = report.issues.filter((i) => i.severity === 'CRITICAL');
  const warningIssues = report.issues.filter((i) => i.severity === 'WARNING');
  const infoIssues = report.issues.filter((i) => i.severity === 'INFO');

  return (
    <div className="container space-y-6">
      {/* Header Navigation */}
      <div className="flex justify-between items-center">
        <button
          onClick={() => navigate('/')}
          className="btn btn-secondary flex items-center"
          style={{ width: 'auto', padding: '6px 12px', fontSize: '13px' }}
        >
          <ArrowLeft size={14} />
          <span>Dashboard</span>
        </button>
        <div className="text-muted font-mono" style={{ fontSize: '11px' }}>Job ID: {report.id}</div>
      </div>

      {/* Summary Status Panel */}
      <div className={`card flex justify-between items-center ${report.status === 'COMPLETED' ? 'alert-success' : 'alert-warning'}`} style={{ padding: '24px 32px' }}>
        <div>
          <h2 className="font-bold flex items-center" style={{ fontSize: '20px', gap: '8px', color: 'inherit' }}>
            {report.status === 'COMPLETED' ? (
              <>
                <ShieldCheck />
                <span>Import Status: Committed</span>
              </>
            ) : (
              <>
                <AlertTriangle />
                <span>Import Status: Pending Resolution</span>
              </>
            )}
          </h2>
          <p className="text-muted" style={{ fontSize: '13px', marginTop: '4px', color: 'inherit', opacity: 0.8 }}>
            File: <strong style={{ color: 'inherit' }}>{report.fileName}</strong>
          </p>
        </div>
        <div className="flex space-x-2">
          <div style={{ background: 'rgba(0,0,0,0.2)', padding: '6px 12px', borderRadius: '6px', minWidth: '60px' }}>
            <div className="font-bold font-mono text-rose" style={{ fontSize: '18px' }}>{criticalIssues.length}</div>
            <div className="text-muted" style={{ fontSize: '9px', textTransform: 'uppercase', fontWeight: 600 }}>Critical</div>
          </div>
          <div style={{ background: 'rgba(0,0,0,0.2)', padding: '6px 12px', borderRadius: '6px', minWidth: '60px' }}>
            <div className="font-bold font-mono text-rose" style={{ fontSize: '18px', color: 'var(--color-amber)' }}>{warningIssues.length}</div>
            <div className="text-muted" style={{ fontSize: '9px', textTransform: 'uppercase', fontWeight: 600 }}>Warning</div>
          </div>
          <div style={{ background: 'rgba(0,0,0,0.2)', padding: '6px 12px', borderRadius: '6px', minWidth: '60px' }}>
            <div className="font-bold font-mono text-rose" style={{ fontSize: '18px', color: 'var(--color-sky)' }}>{infoIssues.length}</div>
            <div className="text-muted" style={{ fontSize: '9px', textTransform: 'uppercase', fontWeight: 600 }}>Info</div>
          </div>
        </div>
      </div>

      {error && <div className="alert alert-error">{error}</div>}

      {/* Issues Review Table */}
      {report.status === 'PENDING_RESOLUTION' && (
        <form onSubmit={handleResolveSubmit} className="space-y-6">
          <div className="card">
            <h3 className="card-title" style={{ marginBottom: '20px' }}>
              <Settings2 size={18} className="text-emerald" />
              <span>Issues Audit Wizard</span>
            </h3>

            <div className="space-y-4">
              {report.issues.map((issue) => (
                <div
                  key={issue.id}
                  className="flex flex-col md:flex-row justify-between items-center"
                  style={{
                    backgroundColor: 'var(--bg-input)',
                    border: '1px solid var(--border-slate)',
                    borderRadius: '8px',
                    padding: '20px',
                    gap: '20px',
                  }}
                >
                  <div className="space-y-2 flex-1" style={{ textAlign: 'left', width: '100%' }}>
                    <div className="flex items-center space-x-2">
                      <span className={`alert-${issue.severity.toLowerCase()}`} style={{ fontSize: '9px', fontWeight: 700, padding: '2px 6px', borderRadius: '4px', margin: 0 }}>
                        {issue.severity}
                      </span>
                      <span className="text-muted" style={{ fontSize: '11px' }}>Row {issue.rowNumber} • {issue.anomalyType}</span>
                    </div>
                    <div className="font-medium" style={{ fontSize: '14px' }}>{issue.description}</div>
                    <div
                      className="font-mono text-muted"
                      style={{
                        backgroundColor: 'rgba(0,0,0,0.2)',
                        padding: '8px 12px',
                        borderRadius: '4px',
                        fontSize: '11px',
                        overflowX: 'auto',
                        whiteSpace: 'nowrap',
                      }}
                    >
                      Raw Data: {issue.originalData}
                    </div>
                  </div>

                  {/* Resolution Input controls */}
                  <div className="space-y-2" style={{ width: '100%', maxWidth: '240px' }}>
                    <label className="form-label" style={{ fontSize: '11px', textTransform: 'uppercase', display: 'block', textAlign: 'left' }}>Action</label>
                    <select
                      className="input-field"
                      style={{ padding: '8px 12px', fontSize: '12px' }}
                      onChange={(e) => handleActionChange(issue.id, e.target.value)}
                      value={resolutions[issue.id]?.action || 'NONE'}
                    >
                      <option value="NONE">Keep Raw (Review Needed)</option>
                      <option value="EXCLUDE">Exclude Transaction</option>
                      {issue.anomalyType === 'MISSING_FIELD' && issue.description.includes('payer') && (
                        <option value="RESOLVE_PAYER">Assign Payer</option>
                      )}
                      {issue.anomalyType === 'MISSING_FIELD' && issue.description.includes('currency') && (
                        <option value="RESOLVE_CURRENCY">Assign Currency</option>
                      )}
                      {issue.anomalyType === 'SPLIT_PERCENTAGE_IMBALANCE' && (
                        <option value="NORMALIZE_PERCENTAGES">Scale to 100%</option>
                      )}
                      {issue.anomalyType === 'ENTITY_NAME_INCONSISTENCY' && (
                        <option value="MAP_USER">Map to Canonical</option>
                      )}
                    </select>

                    {/* Conditional Fields */}
                    {resolutions[issue.id]?.action === 'RESOLVE_PAYER' && (
                      <input
                        type="text"
                        placeholder="Payer name (e.g. Aisha)"
                        className="input-field"
                        style={{ padding: '8px 12px', fontSize: '12px', marginTop: '6px' }}
                        onChange={(e) => handleActionChange(issue.id, 'RESOLVE_PAYER', 'paidBy', e.target.value)}
                      />
                    )}

                    {resolutions[issue.id]?.action === 'RESOLVE_CURRENCY' && (
                      <select
                        className="input-field"
                        style={{ padding: '8px 12px', fontSize: '12px', marginTop: '6px' }}
                        onChange={(e) => handleActionChange(issue.id, 'RESOLVE_CURRENCY', 'currency', e.target.value)}
                      >
                        <option value="">Select Currency</option>
                        <option value="INR">INR</option>
                        <option value="USD">USD</option>
                      </select>
                    )}

                    {resolutions[issue.id]?.action === 'MAP_USER' && (
                      <input
                        type="text"
                        placeholder="Canonical Name (e.g. Rohan)"
                        className="input-field"
                        style={{ padding: '8px 12px', fontSize: '12px', marginTop: '6px' }}
                        onChange={(e) => handleActionChange(issue.id, 'MAP_USER', 'mappedName', e.target.value)}
                      />
                    )}
                  </div>
                </div>
              ))}
            </div>
          </div>

          <button
            type="submit"
            disabled={submitting}
            className="btn btn-primary"
            style={{ padding: '16px' }}
          >
            {submitting ? 'Applying Resolutions...' : 'Commit Staged Entries'}
          </button>
        </form>
      )}

      {report.status === 'COMPLETED' && (
        <div className="card text-center space-y-4">
          <CheckCircle size={48} className="text-emerald mx-auto" />
          <h3 style={{ fontSize: '20px', fontWeight: 800 }}>Successfully Staged & Saved!</h3>
          <p className="text-muted" style={{ maxWidth: '400px', margin: '0 auto', fontSize: '14px' }}>
            This CSV file contained no critical errors. All expenses were parsed, split calculations were processed, and records have been committed to the ledger.
          </p>
          <button
            onClick={() => navigate('/expenses')}
            className="btn btn-primary"
            style={{ width: 'auto', padding: '10px 24px', marginTop: '16px' }}
          >
            View Expenses
          </button>
        </div>
      )}
    </div>
  );
};
