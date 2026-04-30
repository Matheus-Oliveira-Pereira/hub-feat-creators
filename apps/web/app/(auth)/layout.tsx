export default function AuthLayout({ children }: { children: React.ReactNode }) {
  return (
    <div className="min-h-screen bg-slate-50 flex items-center justify-center p-4">
      <div className="w-full max-w-md bg-white rounded-2xl shadow-sm border border-slate-200 p-8">
        <div className="mb-8 text-center">
          <h1 className="text-2xl font-bold text-slate-900">HUB Feat Creator</h1>
          <p className="text-sm text-slate-500 mt-1">Plataforma de assessorias</p>
        </div>
        {children}
      </div>
    </div>
  );
}
