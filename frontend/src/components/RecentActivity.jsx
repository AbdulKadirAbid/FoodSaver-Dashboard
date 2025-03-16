'use client';
import { Utensils, Shirt, Package, Leaf } from 'lucide-react';

export function RecentActivity() {
  return (
    <div className='space-y-6'>
      {activities.map((activity) => (
        <div className='flex items-center' key={activity.id}>
          <div className='flex h-10 w-10 items-center justify-center rounded-full bg-gradient-to-br from-amber-100 to-amber-200'>
            {activity.type === 'food-donation' && (
              <Utensils className='h-5 w-5 text-amber-600' />
            )}
            {activity.type === 'appliance-donation' && (
              <Package className='h-5 w-5 text-purple-600' />
            )}
            {activity.type === 'food-waste' && (
              <Utensils className='h-5 w-5 text-red-600' />
            )}
            {activity.type === 'clothes-donation' && (
              <Shirt className='h-5 w-5 text-blue-600' />
            )}
            {activity.type === 'food-saved' && (
              <Leaf className='h-5 w-5 text-emerald-600' />
            )}
          </div>
          <div className='ml-4 space-y-1'>
            <p className='text-sm font-medium leading-none'>{activity.name}</p>
            <p className='text-sm text-muted-foreground'>{activity.action}</p>
          </div>
          <div className='ml-auto'>
            <span className='inline-flex items-center rounded-full bg-gray-100 px-2.5 py-0.5 text-xs font-medium text-gray-800'>
              {activity.date}
            </span>
          </div>
        </div>
      ))}
    </div>
  );
}

const activities = [
  {
    id: '1',
    name: 'John Smith',
    action: 'Donated 5kg of food',
    date: 'Just now',
    type: 'food-donation',
  },
  {
    id: '2',
    name: 'Sarah Johnson',
    action: 'Donated 3 appliances',
    date: '2h ago',
    type: 'appliance-donation',
  },
  {
    id: '3',
    name: 'Michael Brown',
    action: 'Recorded 2kg of food waste',
    date: '5h ago',
    type: 'food-waste',
  },
  {
    id: '4',
    name: 'Emily Davis',
    action: 'Donated 12 clothing items',
    date: 'Yesterday',
    type: 'clothes-donation',
  },
  {
    id: '5',
    name: 'David Wilson',
    action: 'Saved 8kg of food from waste',
    date: 'Yesterday',
    type: 'food-saved',
  },
];
