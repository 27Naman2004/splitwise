import React, { useState, useEffect } from 'react';
import api from '../services/api';
import { Users, RefreshCw, Landmark, ArrowRight, Settings, CheckCircle } from 'lucide-react';

interface MemberBalance {
  userId: string;
  name: string;
  netBalance: number;
}

interface SimplifiedDebt {
  fromUserId: string;
  fromUserName: string;
  toUserId: string;
  toUserName: string;
  amount: number;
  currency: string;
}

interface ExchangeRate {
  id: string;
  fromCurrency: string;
  toCurrency: string;
  rate: number;
}

export const BalanceSummary: React.FC = () => {
  const [balances, setBalances] = useState<MemberBalance[]>([]);
  const [debts, setDebts] = useState<SimplifiedDebt[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [groupId, setGroupId] = useState<string>('');
  const [error, setError] = useState<string>('');
  const [rateInput, setRateInput] = useState<string>('83.00');

  useEffect(() => {
    fetchActiveGroupDetails();
  }, []);

  const fetchActiveGroupDetails = async () => {
    try {
      setLoading(true);
      const groupResponse = await api.get('/api/groups');
      if (groupResponse.data && groupResponse.data.length > 0) {
        const activeGroup = groupResponse.data[0];
        setGroupId(activeGroup.id);
        
        await Promise.all([
          fetchBalances(activeGroup.id),
          fetchSimplifiedDebts(activeGroup.id),
          fetchRates(),
        ]);
      }
    } catch (err: any) {
      setError('Failed to load active group data.');
    } finally {
      setLoading(false);
    }
  };

  const fetchBalances = async (gId: string) => {
    const response = await api.get(`/api/groups/${gId}/balances`);
    setBalances(response.data.balances || []);
  };

  const fetchSimplifiedDebts = async (gId: string) => {
    const response = await api.get(`/api/groups/${gId}/simplified-debts`);
    setDebts(response.data || []);
  };

  const fetchRates = async () => {
    try {
      const response = await api.get('/api/currencies/rates');
      const usdInrRate = response.data.find(
        (r: ExchangeRate) => r.fromCurrency === 'USD' && r.toCurrency === 'INR'
      );
      if (usdInrRate) {
        setRateInput(usdInrRate.rate.toString());
      }
    } catch (e) {
      // Endpoint may be stubbed out
    }
  };

  const handleSettleDebt = async (debt: SimplifiedDebt) => {
    if (!window.confirm(`Are you sure you want to log a settlement of ₹${debt.amount.toFixed(2)} from ${debt.fromUserName} to ${debt.toUserName}?`)) {
      return;
    }

    try {
      const payload = {
        groupId,
        fromUserId: debt.fromUserId,
        toUserId: debt.toUserId,
        amount: debt.amount,
        currency: debt.currency,
        settlementDate: new Date().toISOString(),
        notes: `Settled simplified debt: ${debt.fromUserName} paid ${debt.toUserName}`,
      };

      await api.post(`/api/groups/${groupId}/settlements`, payload);
      await Promise.all([fetchBalances(groupId), fetchSimplifiedDebts(groupId)]);
      alert('Settlement successfully recorded!');
    } catch (err: any) {
      setError('Failed to record settlement.');
    }
  };

  const handleUpdateRate = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      const parsedRate = parseFloat(rateInput);
      if (isNaN(parsedRate) || parsedRate <= 0) {
        alert('Exchange rate must be a valid positive number.');
        return;
      }
      await api.post('/api/currencies/rates', {
        fromCurrency: 'USD',
        toCurrency: 'INR',
        rate: parsedRate,
      });
      alert('Exchange rate updated! Recalculating balances.');
      await Promise.all([fetchBalances(groupId), fetchSimplifiedDebts(groupId), fetchRates()]);
    } catch (err: any) {
      alert('Failed to update exchange rate.');
    }
  };

  if (loading) {
    return (
      <div className="container text-center" style={{ padding: '80px 24px' }}>
        <h2 className="flex justify-center items-center space-x-2" style={{ fontSize: '20px' }}>
          <RefreshCw className="animate-spin text-emerald" />
          <span>Analyzing Group Balances...</span>
        </h2>
      </div>
    );
  }

  return (
    <div className="container space-y-6">
      {/* Title */}
      <div>
        <h2 className="font-bold" style={{ fontSize: '28px' }}>Balances & Settlements</h2>
        <p className="text-muted" style={{ fontSize: '14px', marginTop: '4px' }}>Audit net outlays, resolve debts, and manage conversion settings</p>
      </div>

      {error && <div className="alert alert-error">{error}</div>}

      <div className="grid grid-cols-3">
        {/* Left Column: Net Balances */}
        <div className="card space-y-4">
          <h3 className="card-title" style={{ borderBottom: '1px solid var(--border-slate)', paddingBottom: '12px' }}>
            <Users size={18} className="text-emerald" />
            <span>Net Balances</span>
          </h3>

          <div className="space-y-3">
            {balances.length === 0 ? (
              <div className="text-muted text-sm" style={{ padding: '16px 0' }}>No balances logged in this group.</div>
            ) : (
              balances.map((mb) => (
                <div
                  key={mb.userId}
                  className="flex justify-between items-center"
                  style={{
                    backgroundColor: 'var(--bg-input)',
                    padding: '12px 16px',
                    borderRadius: '8px',
                    border: '1px solid var(--border-slate)',
                  }}
                >
                  <span className="font-semibold" style={{ fontSize: '14px' }}>{mb.name}</span>
                  <span className={`font-mono font-bold`} style={{ fontSize: '14px', color: mb.netBalance > 0 ? 'var(--color-emerald)' : mb.netBalance < 0 ? 'var(--color-rose)' : 'var(--text-muted)' }}>
                    {mb.netBalance > 0 ? '+' : ''}₹{mb.netBalance.toFixed(2)}
                  </span>
                </div>
              ))
            )}
          </div>
        </div>

        {/* Middle Column: Simplified Debts Checklist */}
        <div className="card space-y-4">
          <h3 className="card-title" style={{ borderBottom: '1px solid var(--border-slate)', paddingBottom: '12px' }}>
            <Landmark size={18} className="text-sky" />
            <span>Simplified Payments</span>
          </h3>

          <div className="space-y-3">
            {debts.length === 0 ? (
              <div className="text-center text-muted" style={{ padding: '24px 0' }}>
                <CheckCircle size={32} className="text-emerald mx-auto" style={{ marginBottom: '8px' }} />
                <span style={{ fontSize: '13px' }}>All roommates are fully settled!</span>
              </div>
            ) : (
              debts.map((debt, index) => (
                <div
                  key={index}
                  className="flex flex-col"
                  style={{
                    backgroundColor: 'var(--bg-input)',
                    border: '1px solid var(--border-slate)',
                    padding: '12px 16px',
                    borderRadius: '8px',
                    gap: '12px',
                  }}
                >
                  <div className="flex justify-between items-center" style={{ fontSize: '13px' }}>
                    <span className="font-semibold">{debt.fromUserName}</span>
                    <ArrowRight size={12} className="text-muted" />
                    <span className="font-semibold">{debt.toUserName}</span>
                  </div>
                  <div className="flex justify-between items-center">
                    <span className="font-mono text-emerald font-bold" style={{ fontSize: '14px' }}>₹{debt.amount.toFixed(2)}</span>
                    <button
                      onClick={() => handleSettleDebt(debt)}
                      className="btn btn-primary"
                      style={{ padding: '6px 12px', fontSize: '11px', width: 'auto' }}
                    >
                      Settle Up
                    </button>
                  </div>
                </div>
              ))
            )}
          </div>
        </div>

        {/* Right Column: Currency Config */}
        <div className="card space-y-4" style={{ backgroundColor: 'var(--bg-input)' }}>
          <h3 className="card-title" style={{ borderBottom: '1px solid var(--border-slate)', paddingBottom: '12px' }}>
            <Settings size={18} className="text-amber-500" style={{ color: 'var(--color-amber)' }} />
            <span>Currency Config</span>
          </h3>

          <form onSubmit={handleUpdateRate} className="space-y-4">
            <div className="form-group">
              <label className="form-label" style={{ fontSize: '11px', textTransform: 'uppercase' }}>Exchange Rate (USD to INR)</label>
              <div className="flex space-x-2">
                <div style={{ position: 'relative', flex: 1 }}>
                  <span className="text-muted font-mono" style={{ position: 'absolute', left: '10px', top: '12px', fontSize: '12px' }}>1 USD =</span>
                  <input
                    type="number"
                    step="0.0001"
                    required
                    value={rateInput}
                    onChange={(e) => setRateInput(e.target.value)}
                    className="input-field"
                    style={{ paddingLeft: '56px' }}
                  />
                </div>
                <button
                  type="submit"
                  className="btn btn-primary"
                  style={{ width: 'auto', padding: '10px 16px' }}
                >
                  Save
                </button>
              </div>
            </div>
            <p className="text-muted" style={{ fontSize: '11px', lineHeight: '1.4' }}>
              Updating the rate will dynamically convert historical USD transactions to INR in calculations without altering original records.
            </p>
          </form>
        </div>
      </div>
    </div>
  );
};
