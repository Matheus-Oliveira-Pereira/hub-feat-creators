'use client';

import * as React from 'react';
import { toast } from 'sonner';
import { useEmailLayout, useSaveEmailLayout } from '@/lib/queries';
import { Button } from '@/components/ui/button';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { Skeleton } from '@/components/ui/skeleton';
import { Can } from '@/components/auth/can';

export function EmailLayoutEditor() {
  const { data: layout, isLoading } = useEmailLayout();
  const saveLayout = useSaveEmailLayout();

  const [headerHtml, setHeaderHtml] = React.useState('');
  const [footerHtml, setFooterHtml] = React.useState('');

  React.useEffect(() => {
    if (layout) {
      setHeaderHtml(layout.headerHtml);
      setFooterHtml(layout.footerHtml);
    }
  }, [layout]);

  if (isLoading) return <Skeleton className="h-64 w-full" />;

  return (
    <div className="grid gap-6">
      <div className="grid gap-1.5">
        <Label htmlFor="headerHtml">HTML do cabeçalho</Label>
        <Textarea
          id="headerHtml"
          rows={6}
          className="font-mono text-xs"
          value={headerHtml}
          onChange={(e) => setHeaderHtml(e.target.value)}
          placeholder="<div style='...'>Logotipo da assessoria</div>"
        />
      </div>
      <div className="grid gap-1.5">
        <Label htmlFor="footerHtml">HTML do rodapé</Label>
        <Textarea
          id="footerHtml"
          rows={6}
          className="font-mono text-xs"
          value={footerHtml}
          onChange={(e) => setFooterHtml(e.target.value)}
          placeholder="<p>Use {{unsubscribe_url}} para o link de descadastro.</p>"
        />
        <p className="text-xs text-muted-foreground">Use {'{{unsubscribe_url}}'} para o link de descadastro obrigatório (LGPD).</p>
      </div>
      <Can role="EEML">
        <Button
          onClick={() =>
            saveLayout.mutate({ headerHtml, footerHtml }, {
              onSuccess: () => toast.success('Layout salvo'),
              onError: () => toast.error('Erro ao salvar layout'),
            })
          }
          disabled={saveLayout.isPending}
        >
          {saveLayout.isPending ? 'Salvando…' : 'Salvar layout'}
        </Button>
      </Can>
    </div>
  );
}
