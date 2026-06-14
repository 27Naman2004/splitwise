import React, { useState, useEffect } from 'react';
import api from '../services/api';
import { AlertCircle, PlusCircle, CreditCard, RefreshCw } from 'lucide-react';

interface Settlement {
  id: string;
  fromUser: {
    id: string;
    name: string;
  };
  toUser: {
    id: string;
    name: string;
  };
  amount: number;
  currency: string;
  settlementDate: string;
  notes: string;
}

interface GroupMember {
  id: string;
  name: string;
}

export const Settlements: React.FC = () => {
  const [settlements, setSettlements] = useState<Settlement[]>([]);
  const [members, setMembers] = useState<GroupMember[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [groupId, setGroupId] = useState<string>('');
  const [error, setError] = useState<string>('');
  const [submitting, setSubmitting] = useState<boolean>(false);

  const [fromUserId, setFromUserId] = useState<string>('');
  const [toUserId, setToUserId] = useState<string>('');
  const [amount, setAmount] = useState<string>('');
  const [currency, setCurrency] = useState<string>('INR');
  const [settlementDate, setSettlementDate] = useState<string>(new Date().toISOString().substring(0, 16));
  const [notes, setNotes] = useState<string>('');

  useEffect(() => {
    fetchActiveGroupAndData();
  }, []);

  const fetchActiveGroupAndData = async () => {
    try {
      setLoading(true);
      const groupResponse = await api.get('/api/groups');
      if (groupResponse.data && groupResponse.data.length > 0) {
        const activeGroup = groupResponse.data[0];
        setGroupId(activeGroup.id);
        
        await Promise.all([
          fetchSettlements(activeGroup.id),
          fetchMembers(activeGroup.id),
        ]);
      }
    } catch (err: any) {
      setError('Failed to load active group space data.');
    } finally {
      setLoading(false);
    }
  };

  const fetchSettlements = async (gId: string) => {
    const response = await api.get(`/api/groups/${gId}/settlements`);
    setSettlements(response.data || []);
  };

  const fetchMembers = async (gId: string) => {
    const response = await api.get(`/api/groups/${gId}/members`);
    setMembers(response.data || []);
    if (response.data.length > 1) {
      setFromUserId(response.data[0].id);
      setToUserId(response.data[1].id);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!groupId) return;

    setError('');
    setSubmitting(true);

    if (fromUserId === toUserId) {
      setError('Debtor and Creditor cannot represent the same user.');
      setSubmitting(false);
      return;
    }

    try {
      const payload = {
        groupId,
        fromUserId,
        toUserId,
        amount: parseFloat(amount),
        currency,
        settlementDate: new Date(settlementDate).toISOString(),
        notes,
      };

      await api.post(`/api/groups/${groupId}/settlements`, payload);
      setAmount('');
      setNotes('');
      await fetchSettlements(groupId);
      alert('Settlement successfully recorded!');
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to record settlement.');
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return (
      <div className="container text-center" style={{ padding: '80px 24px' }}>
        <h2 className="flex justify-center items-center space-x-2" style={{ fontSize: '20px' }}>
          <RefreshCw className="animate-spin text-emerald" />
          <span>Fetching Settlement Records...</span>
        </h2>
      </div>
    );
  }

  return (
    <div className="container space-y-6">
      {/* Title */}
      <div>
        <h2 className="font-bold" style={{ fontSize: '28px' }}>Settlements Ledger</h2>
        <p className="text-muted" style={{ fontSize: '14px', marginTop: '4px' }}>Record and inspect direct payments between group members</p>
      </div>

      {error && (
        <div className="alert alert-error">
          <AlertCircle size={18} />
          <span>{error}</span>
        </div>
      )}

      <div className="grid grid-cols-lg-3">
        {/* Left Columns: Historical Payments List */}
        <div className="card space-y-4">
          <h3 className="card-title" style={{ borderBottom: '1px solid var(--border-slate)', paddingBottom: '12px' }}>
            <CreditCard size={18} className="text-emerald" />
            <span>Payments History</span>
          </h3>

          <div className="table-container">
            {settlements.length === 0 ? (
              <div className="text-center text-muted" style={{ padding: '32px', fontSize: '14px' }}>
                No settlements have been recorded in this group.
              </div>
            ) : (
              <table className="table">
                <thead>
                  <tr>
                    <th>Date</th>
                    <th>Payer (From)</th>
                    <th>Recipient (To)</th>
                    <th className="text-right">Amount</th>
                    <th>Notes</th>
                  </tr>
                </thead>
                <tbody>
                  {settlements.map((s) => (
                    <tr key={s.id}>
                      <td className="font-mono text-muted" style={{ fontSize: '12px' }}>
                        {new Date(s.settlementDate).toLocaleDateString('en-GB')}
                      </td>
                      <td className="font-semibold">{s.fromUser.name}</td>
                      <td className="font-semibold">{s.toUser.name}</td>
                      <td className="text-right font-mono font-bold text-emerald">
                        {s.currency === 'INR' ? '₹' : '$'} {s.amount.toFixed(2)}
                      </td>
                      <td className="text-muted" style={{ fontSize: '12px' }}>{s.notes}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        </div>

        {/* Right Column: Record Form */}
        <div className="card space-y-4" style={{ backgroundColor: 'var(--bg-input)' }}>
          <h3 className="card-title" style={{ borderBottom: '1px solid var(--border-slate)', paddingBottom: '12px' }}>
            <PlusCircle size={18} className="text-emerald" />
            <span>Record Payment</span>
          </h3>

          <form onSubmit={handleSubmit} className="space-y-4">
            <div className="form-group">
              <label className="form-label">Payer (Debtor)</label>
              <select
                value={fromUserId}
                onChange={(e) => setFromUserId(e.target.value)}
                className="input-field"
              >
                {members.map((m) => (
                  <option key={m.id} value={m.id}>{m.name}</option>
                ))}
              </select>
            </div>

            <div className="form-group">
              <label className="form-label">Recipient (Creditor)</label>
              <select
                value={toUserId}
                onChange={(e) => setToUserId(e.target.value)}
                className="input-field"
              >
                {members.map((m) => (
                  <option key={m.id} value={m.id}>{m.name}</option>
                ))}
              </select>
            </div>

            <div className="grid grid-cols-2" style={{ gap: '16px' }}>
              <div className="form-group">
                <label className="form-label">Amount</label>
                <input
                  type="number"
                  step="0.01"
                  required
                  value={amount}
                  onChange={(e) => setAmount(e.target.value)}
                  placeholder="1000.00"
                  className="input-field"
                />
              </div>

              <div className="form-group">
                <label className="form-label">Currency</label>
                <select
                  value={currency}
                  onChange={(e) => setCurrency(e.target.value)}
                  className="input-field"
                >
                  <option value="INR">INR (₹)</option>
                  <option value="USD">USD ($)</option>
                </select>
              </div>
            </div>

            <div className="form-group">
              <label className="form-label">Date</label>
              <input
                type="datetime-local"
                required
                value={settlementDate}
                onChange={(e) => setSettlementDate(e.target.value)}
                className="input-field"
              />
            </div>

            <div className="form-group">
              <label className="form-label">Notes</label>
              <textarea
                value={notes}
                onChange={(e) => setNotes(e.target.value)}
                placeholder="Details (e.g. Paid cash back for dinner)"
                rows={2}
                className="input-field"
                style={{ resize: 'none' }}
              />
            </div>

            <button
              type="submit"
              disabled={submitting}
              className="btn btn-primary"
              style={{ marginTop: '8px' }}
            >
              {submitting ? 'Recording...' : 'Log Settlement'}
            </button>
          </form>
        </div>
      </div>
    </div>
  );
};
