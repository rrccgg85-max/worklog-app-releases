import React, { useEffect } from "react";
import { Bell, X, CheckCircle2, Wrench, AlertCircle } from "lucide-react";

export interface ToastMessage {
  id: string;
  title: string;
  message: string;
  type?: "info" | "success" | "warning";
  timestamp: number;
}

interface ToastContainerProps {
  toasts: ToastMessage[];
  onDismiss: (id: string) => void;
  onSelectCase?: (caseId: string) => void;
}

export const ToastContainer: React.FC<ToastContainerProps> = ({
  toasts,
  onDismiss,
  onSelectCase
}) => {
  return (
    <div className="fixed bottom-5 right-5 z-50 flex flex-col space-y-3 max-w-sm w-full px-4 pointer-events-none font-['Prompt']">
      {toasts.map((toast) => (
        <div
          key={toast.id}
          className="pointer-events-auto bg-slate-900/95 backdrop-blur-md border border-slate-700/80 rounded-2xl p-4 shadow-2xl shadow-black/60 text-slate-100 flex items-start space-x-3 animate-slideUp relative overflow-hidden group"
        >
          <div className="absolute top-0 left-0 bottom-0 w-1 bg-orange-500"></div>
          
          <div className="w-8 h-8 rounded-xl bg-orange-500/10 text-orange-400 flex items-center justify-center shrink-0 mt-0.5 border border-orange-500/20">
            {toast.type === "success" ? (
              <CheckCircle2 className="w-4 h-4 text-emerald-400" />
            ) : toast.type === "warning" ? (
              <AlertCircle className="w-4 h-4 text-amber-400" />
            ) : (
              <Bell className="w-4 h-4 text-orange-400" />
            )}
          </div>

          <div className="flex-1 min-w-0 pr-2">
            <h5 className="text-xs font-bold text-white tracking-tight">{toast.title}</h5>
            <p className="text-[11px] text-slate-300 mt-0.5 line-clamp-2 leading-relaxed">{toast.message}</p>
          </div>

          <button
            onClick={() => onDismiss(toast.id)}
            className="text-slate-500 hover:text-slate-300 p-1 rounded-lg transition-colors shrink-0"
          >
            <X className="w-3.5 h-3.5" />
          </button>
        </div>
      ))}
    </div>
  );
};
