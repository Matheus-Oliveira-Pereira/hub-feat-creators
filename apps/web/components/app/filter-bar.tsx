'use client';

import * as React from 'react';
import { LayoutGrid, Search, Table2, X } from 'lucide-react';
import { Input } from '@/components/ui/input';
import { Button } from '@/components/ui/button';
import { Tabs, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { cn } from '@/lib/utils';

interface FilterBarProps {
  search: string;
  onSearchChange: (v: string) => void;
  searchPlaceholder?: string;
  view: 'cards' | 'table';
  onViewChange: (view: 'cards' | 'table') => void;
  count?: number;
  countLabel?: string;
  className?: string;
  children?: React.ReactNode;
}

export function FilterBar({
  search,
  onSearchChange,
  searchPlaceholder = 'Buscar…',
  view,
  onViewChange,
  count,
  countLabel,
  className,
  children,
}: FilterBarProps) {
  return (
    <div
      className={cn(
        'flex flex-col gap-3 mb-6 sm:flex-row sm:items-center sm:justify-between',
        className
      )}
    >
      <div className="flex flex-1 items-center gap-2">
        <div className="relative flex-1 max-w-sm">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
          <Input
            value={search}
            onChange={e => onSearchChange(e.target.value)}
            placeholder={searchPlaceholder}
            className="pl-9 pr-9"
          />
          {search && (
            <button
              onClick={() => onSearchChange('')}
              className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground transition-colors"
              aria-label="Limpar busca"
            >
              <X className="h-3.5 w-3.5" />
            </button>
          )}
        </div>
        {children}
        {count !== undefined && (
          <span className="text-sm text-muted-foreground tabular-nums whitespace-nowrap">
            {count} {countLabel ?? 'itens'}
          </span>
        )}
      </div>

      <Tabs value={view} onValueChange={v => onViewChange(v as 'cards' | 'table')}>
        <TabsList>
          <TabsTrigger value="cards" className="gap-1.5">
            <LayoutGrid className="h-3.5 w-3.5" /> Cards
          </TabsTrigger>
          <TabsTrigger value="table" className="gap-1.5">
            <Table2 className="h-3.5 w-3.5" /> Tabela
          </TabsTrigger>
        </TabsList>
      </Tabs>
    </div>
  );
}
