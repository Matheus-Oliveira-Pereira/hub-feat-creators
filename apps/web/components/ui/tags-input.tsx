'use client';

import * as React from 'react';
import { X } from 'lucide-react';
import { cn } from '@/lib/utils';
import { Badge } from '@/components/ui/badge';

interface TagsInputProps {
  value: string[];
  onChange: (next: string[]) => void;
  placeholder?: string;
  maxTags?: number;
  maxLength?: number;
  id?: string;
  'aria-invalid'?: boolean;
  disabled?: boolean;
}

export function TagsInput({
  value,
  onChange,
  placeholder = 'Adicionar tag…',
  maxTags = 20,
  maxLength = 40,
  id,
  disabled,
  ...rest
}: TagsInputProps) {
  const [draft, setDraft] = React.useState('');
  const inputRef = React.useRef<HTMLInputElement>(null);

  function commit() {
    const tag = draft.trim().toLowerCase();
    if (!tag || tag.length > maxLength) {
      setDraft('');
      return;
    }
    if (value.includes(tag)) {
      setDraft('');
      return;
    }
    if (value.length >= maxTags) {
      setDraft('');
      return;
    }
    onChange([...value, tag]);
    setDraft('');
  }

  function remove(tag: string) {
    onChange(value.filter(t => t !== tag));
  }

  function onKeyDown(e: React.KeyboardEvent<HTMLInputElement>) {
    if (e.key === 'Enter' || e.key === ',') {
      e.preventDefault();
      commit();
    } else if (e.key === 'Backspace' && draft === '' && value.length) {
      onChange(value.slice(0, -1));
    }
  }

  return (
    <div
      role="group"
      aria-invalid={rest['aria-invalid']}
      onClick={() => inputRef.current?.focus()}
      className={cn(
        'flex flex-wrap items-center gap-1.5 min-h-10 w-full rounded-md border border-input bg-background px-2 py-1.5 text-sm shadow-xs transition-colors',
        'focus-within:outline-none focus-within:ring-2 focus-within:ring-ring focus-within:ring-offset-2 focus-within:ring-offset-background focus-within:border-ring',
        disabled && 'opacity-50 cursor-not-allowed',
        rest['aria-invalid'] && 'border-destructive focus-within:ring-destructive'
      )}
    >
      {value.map(tag => (
        <Badge
          key={tag}
          variant="secondary"
          className="gap-1 pr-1 cursor-default font-normal"
        >
          {tag}
          {!disabled && (
            <button
              type="button"
              onClick={e => {
                e.stopPropagation();
                remove(tag);
              }}
              className="rounded-sm hover:bg-foreground/10 transition-colors"
              aria-label={`Remover tag ${tag}`}
            >
              <X className="h-3 w-3" />
            </button>
          )}
        </Badge>
      ))}
      <input
        ref={inputRef}
        id={id}
        type="text"
        value={draft}
        onChange={e => setDraft(e.target.value.slice(0, maxLength))}
        onKeyDown={onKeyDown}
        onBlur={commit}
        disabled={disabled}
        placeholder={value.length === 0 ? placeholder : ''}
        className="flex-1 min-w-[120px] bg-transparent outline-none placeholder:text-muted-foreground disabled:cursor-not-allowed"
      />
    </div>
  );
}
