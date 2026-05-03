'use client';

import * as React from 'react';
import {
  DndContext,
  DragOverlay,
  PointerSensor,
  KeyboardSensor,
  useSensor,
  useSensors,
  closestCorners,
  type DragStartEvent,
  type DragEndEvent,
} from '@dnd-kit/core';
import { useDroppable, useDraggable } from '@dnd-kit/core';
import { Calendar, Building2, AtSign } from 'lucide-react';
import { motion } from 'framer-motion';
import { type Prospeccao, type ProspeccaoStatus } from '@/lib/api';
import { Card } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { cn } from '@/lib/utils';
import {
  STATUS_LABEL,
  STATUS_TONE,
  STATUS_ORDER,
  formatBRL,
  isValidTransition,
} from '@/lib/prospeccao';

interface KanbanProps {
  items: Prospeccao[];
  onCardClick?: (p: Prospeccao) => void;
  onMove?: (p: Prospeccao, novoStatus: ProspeccaoStatus) => Promise<void> | void;
  marcaNomeById?: Map<string, string>;
}

export function ProspeccaoKanban({ items, onCardClick, onMove, marcaNomeById }: KanbanProps) {
  const [activeId, setActiveId] = React.useState<string | null>(null);

  const sensors = useSensors(
    useSensor(PointerSensor, { activationConstraint: { distance: 6 } }),
    useSensor(KeyboardSensor)
  );

  const byStatus = React.useMemo(() => {
    const m = new Map<ProspeccaoStatus, Prospeccao[]>();
    for (const s of STATUS_ORDER) m.set(s, []);
    for (const p of items) m.get(p.status)?.push(p);
    return m;
  }, [items]);

  const active = items.find(i => i.id === activeId) ?? null;

  function onDragStart(e: DragStartEvent) {
    setActiveId(String(e.active.id));
  }

  async function onDragEnd(e: DragEndEvent) {
    setActiveId(null);
    const id = String(e.active.id);
    const overId = e.over?.id ? String(e.over.id) : null;
    if (!overId) return;

    const item = items.find(i => i.id === id);
    if (!item) return;
    const novo = overId as ProspeccaoStatus;
    if (item.status === novo) return;
    if (!isValidTransition(item.status, novo)) return; // dropAreas filtram, mas defesa
    await onMove?.(item, novo);
  }

  return (
    <DndContext
      sensors={sensors}
      collisionDetection={closestCorners}
      onDragStart={onDragStart}
      onDragEnd={onDragEnd}
    >
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-5 gap-3">
        {STATUS_ORDER.map(status => (
          <KanbanColumn
            key={status}
            status={status}
            items={byStatus.get(status) ?? []}
            activeFromStatus={active?.status}
            onCardClick={onCardClick}
            marcaNomeById={marcaNomeById}
          />
        ))}
      </div>
      <DragOverlay dropAnimation={null}>
        {active && (
          <KanbanCard p={active} dragging marcaNomeById={marcaNomeById} />
        )}
      </DragOverlay>
    </DndContext>
  );
}

interface KanbanColumnProps {
  status: ProspeccaoStatus;
  items: Prospeccao[];
  activeFromStatus?: ProspeccaoStatus;
  onCardClick?: (p: Prospeccao) => void;
  marcaNomeById?: Map<string, string>;
}

function KanbanColumn({
  status,
  items,
  activeFromStatus,
  onCardClick,
  marcaNomeById,
}: KanbanColumnProps) {
  const tone = STATUS_TONE[status];
  // só aceita drop se transição é válida ou mesmo status (no-op)
  const acceptsDrop =
    !activeFromStatus ||
    activeFromStatus === status ||
    isValidTransition(activeFromStatus, status);

  const { setNodeRef, isOver } = useDroppable({ id: status, disabled: !acceptsDrop });

  return (
    <div
      ref={setNodeRef}
      className={cn(
        'flex flex-col rounded-xl border border-border bg-muted/30 transition-colors',
        isOver && acceptsDrop && 'ring-2 ring-primary/40 bg-primary/5',
        !acceptsDrop && activeFromStatus && 'opacity-50'
      )}
    >
      <div className="flex items-center justify-between gap-2 px-3 py-2.5 border-b border-border">
        <div className="flex items-center gap-2 min-w-0">
          <span className={cn('h-2 w-2 rounded-full shrink-0', tone.dot)} aria-hidden="true" />
          <span className="font-display text-sm font-semibold truncate">
            {STATUS_LABEL[status]}
          </span>
        </div>
        <Badge variant="muted" className="tabular-nums shrink-0">
          {items.length}
        </Badge>
      </div>
      <div className="flex-1 p-2 space-y-2 min-h-[140px] max-h-[calc(100vh-280px)] overflow-y-auto scrollbar-thin">
        {items.length === 0 ? (
          <p className="text-center text-xs text-muted-foreground py-6">vazio</p>
        ) : (
          items.map(p => (
            <DraggableCard
              key={p.id}
              p={p}
              onClick={() => onCardClick?.(p)}
              marcaNomeById={marcaNomeById}
            />
          ))
        )}
      </div>
    </div>
  );
}

function DraggableCard({
  p,
  onClick,
  marcaNomeById,
}: {
  p: Prospeccao;
  onClick: () => void;
  marcaNomeById?: Map<string, string>;
}) {
  const { setNodeRef, listeners, attributes, isDragging, transform } = useDraggable({ id: p.id });

  return (
    <div
      ref={setNodeRef}
      style={
        transform
          ? { transform: `translate3d(${transform.x}px, ${transform.y}px, 0)` }
          : undefined
      }
      className={cn(isDragging && 'opacity-30')}
      {...attributes}
      {...listeners}
      onClick={e => {
        // só aceita click se não for drag (sem transform)
        if (!isDragging) onClick();
      }}
    >
      <KanbanCard p={p} marcaNomeById={marcaNomeById} />
    </div>
  );
}

function KanbanCard({
  p,
  dragging,
  marcaNomeById,
}: {
  p: Prospeccao;
  dragging?: boolean;
  marcaNomeById?: Map<string, string>;
}) {
  const marcaNome = marcaNomeById?.get(p.marcaId) ?? '';
  return (
    <motion.div
      initial={{ opacity: 0, y: 4 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.15 }}
    >
      <Card
        className={cn(
          'p-3 cursor-grab active:cursor-grabbing space-y-2 hover:border-border-strong',
          dragging && 'shadow-lg rotate-1'
        )}
      >
        <p className="font-medium text-sm leading-tight line-clamp-2">{p.titulo}</p>
        {marcaNome && (
          <div className="flex items-center gap-1 text-xs text-muted-foreground">
            <Building2 className="h-3 w-3" />
            <span className="truncate">{marcaNome}</span>
          </div>
        )}
        <div className="flex items-center justify-between text-xs">
          <span className="font-display font-semibold tabular-nums">
            {formatBRL(p.valorEstimadoCentavos)}
          </span>
          {p.proximaAcaoEm && (
            <span className="inline-flex items-center gap-1 text-muted-foreground">
              <Calendar className="h-3 w-3" />
              {new Date(p.proximaAcaoEm).toLocaleDateString('pt-BR', {
                day: '2-digit',
                month: '2-digit',
              })}
            </span>
          )}
        </div>
      </Card>
    </motion.div>
  );
}
