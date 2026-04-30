'use client';

import * as React from 'react';
import { motion } from 'framer-motion';
import { ArrowUpRight, ArrowDownRight } from 'lucide-react';
import { cn } from '@/lib/utils';
import { Card } from '@/components/ui/card';
import { Skeleton } from '@/components/ui/skeleton';

interface StatCardProps {
  label: string;
  value: number | string | null;
  icon: React.ElementType;
  trend?: { delta: number; period: string };
  hint?: string;
  loading?: boolean;
  accent?: boolean;
}

export function StatCard({
  label,
  value,
  icon: Icon,
  trend,
  hint,
  loading,
  accent,
}: StatCardProps) {
  const trendUp = trend && trend.delta >= 0;
  return (
    <motion.div
      initial={{ opacity: 0, y: 8 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.3, ease: [0.4, 0, 0.2, 1] }}
    >
      <Card
        className={cn(
          'relative overflow-hidden p-5',
          accent && 'bg-foreground text-background border-foreground'
        )}
      >
        <div className="flex items-start justify-between gap-3">
          <div className="space-y-1.5">
            <p
              className={cn(
                'text-xs font-medium uppercase tracking-wider',
                accent ? 'text-background/60' : 'text-muted-foreground'
              )}
            >
              {label}
            </p>
            {loading ? (
              <Skeleton className="h-9 w-24" />
            ) : (
              <p className="font-display text-3xl font-bold tracking-tight tabular-nums">
                {value ?? '—'}
              </p>
            )}
            {hint && !loading && (
              <p
                className={cn(
                  'text-xs',
                  accent ? 'text-background/50' : 'text-muted-foreground'
                )}
              >
                {hint}
              </p>
            )}
          </div>
          <div
            className={cn(
              'flex h-10 w-10 items-center justify-center rounded-lg',
              accent ? 'bg-primary/20 text-primary' : 'bg-primary/10 text-primary'
            )}
          >
            <Icon className="h-5 w-5" />
          </div>
        </div>
        {trend && !loading && (
          <div
            className={cn(
              'mt-3 inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-xs font-medium',
              trendUp
                ? 'bg-success/15 text-success'
                : 'bg-destructive/15 text-destructive'
            )}
          >
            {trendUp ? <ArrowUpRight className="h-3 w-3" /> : <ArrowDownRight className="h-3 w-3" />}
            {Math.abs(trend.delta)}% {trend.period}
          </div>
        )}
      </Card>
    </motion.div>
  );
}
