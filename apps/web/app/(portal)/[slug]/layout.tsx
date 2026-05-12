'use client';

import * as React from 'react';
import { useParams } from 'next/navigation';
import { portalAuth, type AssessoriaBranding } from '@/lib/api';
import { BrandingContext } from './branding-context';

export default function PortalLayout({ children }: { children: React.ReactNode }) {
  const { slug } = useParams<{ slug: string }>();
  const [branding, setBranding] = React.useState<AssessoriaBranding | null>(null);

  React.useEffect(() => {
    portalAuth.branding(slug).then(setBranding).catch(() => {});
  }, [slug]);

  const primary = branding?.corPrimaria ?? '#C2E000';

  return (
    <div
      className="min-h-screen bg-white"
      style={{ '--portal-primary': primary } as React.CSSProperties}
    >
      <header className="border-b px-6 py-3 flex items-center gap-3">
        {branding?.logoUrl ? (
          // eslint-disable-next-line @next/next/no-img-element
          <img src={branding.logoUrl} alt="Logo" className="h-8 object-contain" />
        ) : (
          <span className="text-sm font-semibold text-[#141414]">feat. creators</span>
        )}
      </header>
      <BrandingContext.Provider value={branding}>
        <main className="max-w-2xl mx-auto px-4 py-8">{children}</main>
      </BrandingContext.Provider>
    </div>
  );
}
