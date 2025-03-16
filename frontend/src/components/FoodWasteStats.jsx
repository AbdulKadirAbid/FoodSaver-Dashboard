'use client';

import { useState, useEffect } from 'react';
import {
  fetchFoodSummary,
  fetchLastWeekWaste,
  fetchLastMonthWaste,
} from '@/Api/api';

export function FoodWasteStats() {
  const [stats, setStats] = useState({
    totalWasted: 0,
    wastedThisWeek: 0,
    wastedLastWeek: 0,
    wastedThisMonth: 0,
    wastedLastMonth: 0,
  });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const userId = 1;

  useEffect(() => {
    const loadWasteStats = async () => {
      try {
        setLoading(true);

        // Fetch data from API endpoints
        const [foodSummary, lastWeek, lastMonth] = await Promise.all([
          fetchFoodSummary(userId),
          fetchLastWeekWaste(userId),
          fetchLastMonthWaste(userId),
        ]);

        console.log('Food Summary:', foodSummary);
        console.log('Last Week Waste:', lastWeek);
        console.log('Last Month Waste:', lastMonth);

        // For this week's data, we might not have a direct API
        // Using current week as most recent data point (could be estimated)
        const thisWeek = foodSummary.wastedLastWeek;

        // For current month, we'll use the total wasted - last month's waste
        const thisMonth = foodSummary.totalWasted;

        setStats({
          totalWasted: foodSummary.totalWasted,
          wastedThisWeek: thisWeek,
          wastedLastWeek: lastWeek,
          wastedThisMonth: thisMonth,
          wastedLastMonth: lastMonth,
        });

        setLoading(false);
      } catch (error) {
        console.error('Error loading waste stats:', error);
        setError('Failed to load waste statistics');
        setLoading(false);
      }
    };

    loadWasteStats();
  }, [userId]);

  const calculateImprovement = (current, previous) => {
    if (!previous || previous === 0) return 0;
    const change = ((previous - current) / previous) * 100;
    return change.toFixed(1);
  };

  if (loading) {
    return <div className='p-4 text-red-600'>Loading waste statistics...</div>;
  }

  if (error) {
    return <div className='p-4 text-red-600'>{error}</div>;
  }

  // Calculate improvements
  const weeklyImprovement = calculateImprovement(
    stats.wastedThisWeek,
    stats.wastedLastWeek
  );
  const monthlyImprovement = calculateImprovement(
    stats.wastedThisMonth,
    stats.wastedLastMonth
  );

  return (
    <div className='space-y-6'>
      <div className='grid grid-cols-2 gap-4'>
        <div className='rounded-lg bg-red-50 p-4'>
          <span className='text-sm font-medium text-red-600'>This Week</span>
          <span className='mt-1 block text-2xl font-bold text-red-700'>
            {stats.wastedThisWeek} kg
          </span>
        </div>
        <div className='rounded-lg bg-emerald-50 p-4'>
          <span className='text-sm font-medium text-emerald-600'>
            Last Week
          </span>
          <span className='mt-1 block text-2xl font-bold text-emerald-700'>
            {stats.wastedLastWeek} kg
          </span>
          {weeklyImprovement > 0 && (
            <span className='mt-1 text-xs font-medium text-emerald-600'>
              -{weeklyImprovement}% improvement
            </span>
          )}
          {weeklyImprovement < 0 && (
            <span className='mt-1 text-xs font-medium text-red-600'>
              +{Math.abs(weeklyImprovement)}% increase
            </span>
          )}
        </div>
      </div>

      <div className='grid grid-cols-2 gap-4'>
        <div className='rounded-lg bg-red-50 p-4'>
          <span className='text-sm font-medium text-red-600'>This Month</span>
          <span className='mt-1 block text-2xl font-bold text-red-700'>
            {stats.wastedThisMonth} kg
          </span>
        </div>
        <div className='rounded-lg bg-emerald-50 p-4'>
          <span className='text-sm font-medium text-emerald-600'>
            Last Month
          </span>
          <span className='mt-1 block text-2xl font-bold text-emerald-700'>
            {stats.wastedLastMonth} kg
          </span>
          {monthlyImprovement > 0 && (
            <span className='mt-1 text-xs font-medium text-emerald-600'>
              -{monthlyImprovement}% improvement
            </span>
          )}
          {monthlyImprovement < 0 && (
            <span className='mt-1 text-xs font-medium text-red-600'>
              +{Math.abs(monthlyImprovement)}% increase
            </span>
          )}
        </div>
      </div>

      <div className='rounded-lg bg-amber-50 p-4'>
        <h4 className='text-sm font-medium text-amber-800'>
          Waste Reduction Tips
        </h4>
        <ul className='mt-2 space-y-2 text-sm text-amber-700'>
          <li className='flex items-center gap-2'>
            <div className='flex h-6 w-6 items-center justify-center rounded-full bg-amber-100'>
              <span className='text-xs font-bold text-amber-700'>1</span>
            </div>
            <span>Plan meals and create shopping lists</span>
          </li>
          <li className='flex items-center gap-2'>
            <div className='flex h-6 w-6 items-center justify-center rounded-full bg-amber-100'>
              <span className='text-xs font-bold text-amber-700'>2</span>
            </div>
            <span>Store food properly to extend shelf life</span>
          </li>
          <li className='flex items-center gap-2'>
            <div className='flex h-6 w-6 items-center justify-center rounded-full bg-amber-100'>
              <span className='text-xs font-bold text-amber-700'>3</span>
            </div>
            <span>Use leftovers creatively in new recipes</span>
          </li>
          <li className='flex items-center gap-2'>
            <div className='flex h-6 w-6 items-center justify-center rounded-full bg-amber-100'>
              <span className='text-xs font-bold text-amber-700'>4</span>
            </div>
            <span>Freeze excess food before it spoils</span>
          </li>
        </ul>
      </div>
    </div>
  );
}
