import { useState, useEffect } from 'react';
import { ShoppingBag, Utensils, Shirt, Package } from 'lucide-react';
import { fetchDonationSummary, fetchLastMonthDonations } from '@/Api/api';

export function DonationStats() {
  const [donationStats, setDonationStats] = useState({
    totalSaved: 0,
    totalWasted: 0,
    wastedLastWeek: 0,
    wastedLastMonth: 0,
    lastMonthDonations: 0,
  });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const userId = 1;

  useEffect(() => {
    const loadDonationStats = async () => {
      try {
        setLoading(true);

        // Fetch donation data from API
        const [summary, lastMonth] = await Promise.all([
          fetchDonationSummary(userId),
          fetchLastMonthDonations(userId),
        ]);

        console.log('Donation Summary:', summary);
        console.log('Last Month Donations:', lastMonth);

        setDonationStats({
          ...summary,
          lastMonthDonations: lastMonth,
        });

        setLoading(false);
      } catch (error) {
        console.error('Error loading donation stats:', error);
        setError('Failed to load donation statistics');
        setLoading(false);
      }
    };

    loadDonationStats();
  }, [userId]);

  if (loading) {
    return (
      <div className='p-4 text-amber-600'>Loading donation statistics...</div>
    );
  }

  if (error) {
    return <div className='p-4 text-red-600'>{error}</div>;
  }

  return (
    <div className='space-y-6'>
      <div className='rounded-lg bg-amber-50 p-4'>
        <div className='flex items-center gap-3'>
          <ShoppingBag className='h-6 w-6 text-amber-600' />
          <div>
            <span className='text-sm font-medium text-amber-600'>
              Total Donations
            </span>
            <span className='mt-1 block text-2xl font-bold text-amber-700'>
              {donationStats.totalSaved} items
            </span>
          </div>
        </div>
      </div>

      <div className='grid grid-cols-2 gap-4'>
        <div className='rounded-lg bg-emerald-50 p-4'>
          <div className='flex items-center gap-2'>
            <Utensils className='h-5 w-5 text-emerald-600' />
            <span className='text-sm font-medium text-emerald-600'>
              Food Saved
            </span>
          </div>
          <span className='mt-1 block text-2xl font-bold text-emerald-700'>
            {donationStats.totalSaved}
          </span>
        </div>
        <div className='rounded-lg bg-blue-50 p-4'>
          <div className='flex items-center gap-2'>
            <Shirt className='h-5 w-5 text-blue-600' />
            <span className='text-sm font-medium text-blue-600'>
              Last Month
            </span>
          </div>
          <span className='mt-1 block text-2xl font-bold text-blue-700'>
            {donationStats.lastMonthDonations}
          </span>
        </div>
      </div>

      <div className='grid grid-cols-2 gap-4'>
        <div className='rounded-lg bg-purple-50 p-4'>
          <div className='flex items-center gap-2'>
            <Package className='h-5 w-5 text-purple-600' />
            <span className='text-sm font-medium text-purple-600'>
              Last Week
            </span>
          </div>
          <span className='mt-1 block text-2xl font-bold text-purple-700'>
            {donationStats.wastedLastWeek}
          </span>
        </div>
        <div className='rounded-lg bg-gray-50 p-4'>
          <div className='flex items-center gap-2'>
            <div className='flex h-5 w-5 items-center justify-center rounded-full bg-gray-200'>
              <span className='text-xs font-bold text-gray-600'>+</span>
            </div>
            <span className='text-sm font-medium text-gray-600'>
              Food Wasted
            </span>
          </div>
          <span className='mt-1 block text-2xl font-bold text-gray-700'>
            {donationStats.totalWasted}
          </span>
        </div>
      </div>

      <div className='rounded-lg bg-amber-50 p-4'>
        <h4 className='text-sm font-medium text-amber-800'>
          This Month vs Last Month
        </h4>
        <div className='mt-3 grid grid-cols-2 gap-3'>
          <div className='rounded-md bg-white p-2 shadow-sm'>
            <div className='flex items-center justify-between'>
              <span className='text-sm text-amber-600'>Food Saved</span>
              <span className='text-sm font-medium text-emerald-600'>
                {donationStats.totalSaved > donationStats.lastMonthDonations
                  ? '+'
                  : ''}
                {Math.round(
                  ((donationStats.totalSaved -
                    donationStats.lastMonthDonations) /
                    donationStats.lastMonthDonations) *
                    100
                ) || 0}
                %
              </span>
            </div>
            <div className='mt-1 h-2 w-full overflow-hidden rounded-full bg-amber-100'>
              <div
                className='h-full rounded-full bg-emerald-500'
                style={{
                  width: `${Math.min(
                    70,
                    Math.max(
                      30,
                      (donationStats.totalSaved /
                        (donationStats.lastMonthDonations || 1)) *
                        50
                    )
                  )}%`,
                }}
              ></div>
            </div>
          </div>
          <div className='rounded-md bg-white p-2 shadow-sm'>
            <div className='flex items-center justify-between'>
              <span className='text-sm text-blue-600'>Waste Reduced</span>
              <span className='text-sm font-medium text-emerald-600'>
                {donationStats.totalWasted < donationStats.wastedLastMonth
                  ? '+'
                  : ''}
                {Math.round(
                  (Math.abs(
                    donationStats.totalWasted - donationStats.wastedLastMonth
                  ) /
                    (donationStats.wastedLastMonth || 1)) *
                    100
                ) || 0}
                %
              </span>
            </div>
            <div className='mt-1 h-2 w-full overflow-hidden rounded-full bg-blue-100'>
              <div
                className='h-full rounded-full bg-emerald-500'
                style={{
                  width: `${Math.min(
                    70,
                    Math.max(
                      30,
                      (1 -
                        donationStats.totalWasted /
                          (donationStats.wastedLastMonth || 1)) *
                        60
                    )
                  )}%`,
                }}
              ></div>
            </div>
          </div>
          <div className='rounded-md bg-white p-2 shadow-sm'>
            <div className='flex items-center justify-between'>
              <span className='text-sm text-purple-600'>Weekly Progress</span>
              <span className='text-sm font-medium text-emerald-600'>
                {donationStats.wastedLastWeek <
                donationStats.wastedLastMonth / 4
                  ? '+'
                  : ''}
                {Math.round(
                  (Math.abs(
                    donationStats.wastedLastWeek -
                      donationStats.wastedLastMonth / 4
                  ) /
                    (donationStats.wastedLastMonth / 4 || 1)) *
                    100
                ) || 0}
                %
              </span>
            </div>
            <div className='mt-1 h-2 w-full overflow-hidden rounded-full bg-purple-100'>
              <div
                className='h-full rounded-full bg-emerald-500'
                style={{
                  width: `${Math.min(
                    70,
                    Math.max(
                      30,
                      (1 -
                        donationStats.wastedLastWeek /
                          (donationStats.wastedLastMonth / 4 || 1)) *
                        60
                    )
                  )}%`,
                }}
              ></div>
            </div>
          </div>
          <div className='rounded-md bg-white p-2 shadow-sm'>
            <div className='flex items-center justify-between'>
              <span className='text-sm text-gray-600'>Overall Savings</span>
              <span className='text-sm font-medium text-amber-600'>
                {donationStats.totalSaved > donationStats.totalWasted
                  ? '+'
                  : ''}
                {Math.round(
                  ((donationStats.totalSaved - donationStats.totalWasted) /
                    (donationStats.totalWasted || 1)) *
                    100
                )}
                %
              </span>
            </div>
            <div className='mt-1 h-2 w-full overflow-hidden rounded-full bg-gray-100'>
              <div
                className='h-full rounded-full bg-amber-500'
                style={{
                  width: `${Math.min(
                    70,
                    Math.max(
                      30,
                      (donationStats.totalSaved /
                        (donationStats.totalWasted || 1)) *
                        40
                    )
                  )}%`,
                }}
              ></div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
