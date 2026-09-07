import React, { useState, useEffect, useMemo } from "react";
import { 
  RepairCase, 
  TechUser, 
  NotificationDoc, 
  SystemLog 
} from "./types";
import { 
  subscribeToCases 
} from "./services/caseService";
import { 
  subscribeToTechnicians 
} from "./services/techService";
import { 
  subscribeToNotifications 
} from "./services/notificationService";
import { 
  subscribeToSystemLogs 
} from "./services/logService";
import { 
  subscribeToAdminSession,
  clearAdminSession 
} from "./services/sessionService";
import { playNotificationSound } from "./utils/audio";

import { auth } from "./firebase";
import { onAuthStateChanged } from "firebase/auth";
import { Navbar, NavTab } from "./components/Navbar";
import { LoginScreen } from "./components/LoginScreen";
import { OverviewDashboard } from "./components/OverviewDashboard";
import { CaseMonitoring } from "./components/CaseMonitoring";
import { TechManagement } from "./components/TechManagement";
import { TechPerformanceAnalytics } from "./components/TechPerformanceAnalytics";
import { SystemLogsView } from "./components/SystemLogsView";
import { CreateCaseModal } from "./components/CreateCaseModal";
import { CaseDetailModal } from "./components/CaseDetailModal";
import { PrintWorkOrderModal } from "./components/PrintWorkOrderModal";
import { ToastContainer, ToastMessage } from "./components/ToastContainer";
import { MobileCheckInView } from "./components/MobileCheckInView";
import { Loader2, ShieldCheck } from "lucide-react";

export default function App() {
  const [mobileCheckInCaseId, setMobileCheckInCaseId] = useState<string | null>(() => {
    if (typeof window !== "undefined") {
      const params = new URLSearchParams(window.location.search);
      const view = params.get("view");
      const action = params.get("action");
      const caseId = params.get("caseId") || params.get("id");
      if ((view === "checkin" || action === "checkin") && caseId) {
        return caseId;
      }
    }
    return null;
  });

  const [isLoggedIn, setIsLoggedIn] = useState<boolean>(() => {
    const saved = sessionStorage.getItem("worklog_admin_logged_in");
    return saved === "true";
  });
  const [isAuthLoading, setIsAuthLoading] = useState<boolean>(true);

  const [activeTab, setActiveTab] = useState<NavTab>("OVERVIEW");
  const [theme, setTheme] = useState<"dark" | "light" >(() => {
    return (localStorage.getItem("theme") as "dark" | "light") || "dark";
  });

  const handleToggleTheme = () => {
    const nextTheme = theme === "dark" ? "light" : "dark";
    setTheme(nextTheme);
    localStorage.setItem("theme", nextTheme);
  };

  // Sync theme with document and body for proper root-level styling
  useEffect(() => {
    if (theme === "light") {
      document.documentElement.classList.add("light-mode");
      document.body.classList.add("light-mode");
      document.body.style.backgroundColor = "#f8fafc";
      document.body.style.color = "#0f172a";
    } else {
      document.documentElement.classList.remove("light-mode");
      document.body.classList.remove("light-mode");
      document.body.style.backgroundColor = "#090D16";
      document.body.style.color = "#F1F5F9";
    }
  }, [theme]);

  // Sync with Firebase Auth state
  useEffect(() => {
    const unsubscribe = onAuthStateChanged(auth, (user) => {
      setIsAuthLoading(false);
      if (user) {
        setIsLoggedIn(true);
        sessionStorage.setItem("worklog_admin_logged_in", "true");
      } else {
        setIsLoggedIn(false);
        sessionStorage.removeItem("worklog_admin_logged_in");
      }
    });
    return () => unsubscribe();
  }, []);
  const [cases, setCases] = useState<RepairCase[]>([]);
  const [technicians, setTechnicians] = useState<TechUser[]>([]);
  const [notifications, setNotifications] = useState<NotificationDoc[]>([]);
  const [systemLogs, setSystemLogs] = useState<SystemLog[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(true);

  // Modals
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
  const [selectedCase, setSelectedCase] = useState<RepairCase | null>(null);
  const [printCase, setPrintCase] = useState<RepairCase | null>(null);

  // Live Sound & Toast Notifications state
  const [toasts, setToasts] = useState<ToastMessage[]>([]);
  const lastNotifIdRef = React.useRef<string | null>(null);
  const isInitialLoadRef = React.useRef<boolean>(true);

  // Handle new incoming notifications for sound & toast
  useEffect(() => {
    if (notifications.length === 0) return;
    const latest = notifications[0];
    if (isInitialLoadRef.current) {
      lastNotifIdRef.current = latest.id || null;
      isInitialLoadRef.current = false;
      return;
    }

    if (latest.id && latest.id !== lastNotifIdRef.current) {
      lastNotifIdRef.current = latest.id;
      // Play live notification sound
      playNotificationSound();
      // Add Toast
      const newToast: ToastMessage = {
        id: latest.id || Date.now().toString(),
        title: `การแจ้งเตือน: ${latest.customerName || "งานซ่อม"}`,
        message: latest.message,
        type: "info",
        timestamp: Date.now()
      };
      setToasts(prev => [newToast, ...prev.slice(0, 4)]);
    }
  }, [notifications]);

  const handleDismissToast = (id: string) => {
    setToasts(prev => prev.filter(t => t.id !== id));
  };

  // Handle Login success
  const handleLoginSuccess = (adminEmail: string) => {
    setIsLoggedIn(true);
    sessionStorage.setItem("worklog_admin_logged_in", "true");
    sessionStorage.setItem("worklog_admin_email", adminEmail);
    sessionStorage.setItem("worklog_session_start", Date.now().toString());
  };

  // Handle Logout
  const handleLogout = async () => {
    setIsLoggedIn(false);
    sessionStorage.removeItem("worklog_admin_logged_in");
    sessionStorage.removeItem("worklog_admin_email");
    sessionStorage.removeItem("worklog_session_start");
    clearAdminSession();
    try {
      await auth.signOut();
    } catch (e) {
      console.warn("Logout error:", e);
    }
  };

  // Subscribe to Firestore Realtime listeners matching exact collection rules
  useEffect(() => {
    if (!isLoggedIn || isAuthLoading) return;

    let unsubscribeTechs: (() => void) | undefined;
    let unsubscribeCases: (() => void) | undefined;
    let unsubscribeNotifs: (() => void) | undefined;
    let unsubscribeLogs: (() => void) | undefined;

    // 1. Techs Listener (/tech_users & technicians)
    unsubscribeTechs = subscribeToTechnicians((loadedTechs) => {
      setTechnicians(loadedTechs);
    }, (err) => {
      console.warn("Tech subscription:", err);
      setIsLoading(false);
    });

    // 2. Cases Listener (/repair_cases)
    unsubscribeCases = subscribeToCases((loadedCases) => {
      setCases(loadedCases);
      setIsLoading(false);

      // Keep selectedCase in sync if open in modal
      setSelectedCase(prev => {
        if (!prev) return null;
        const updated = loadedCases.find(c => c.caseId === prev.caseId);
        return updated || null;
      });
    }, (err) => {
      console.warn("Cases subscription:", err);
      setIsLoading(false);
    });

    // 3. Notifications Listener (/notifications)
    unsubscribeNotifs = subscribeToNotifications((loadedNotifs) => {
      setNotifications(loadedNotifs);
    }, 20, (err) => {
      console.warn("Notifications subscription:", err);
    });

    // 4. System Logs Listener (/system_logs & activity_logs)
    unsubscribeLogs = subscribeToSystemLogs((loadedLogs) => {
      setSystemLogs(loadedLogs);
    }, 50, (err) => {
      console.warn("Logs subscription:", err);
    });

    // 5. Single-Session Conflict Listener (Enforce 1 Admin per 1 Device)
    const currentSessionId = localStorage.getItem("worklog_admin_session_id") || "";
    const unsubscribeSession = subscribeToAdminSession(currentSessionId, () => {
      handleLogout();
      alert("แจ้งเตือนความปลอดภัย: มีการเข้าสู่ระบบจากอุปกรณ์หรือหน้าต่างอื่น คุณถูกออกจากระบบอัตโนมัติ");
    });

    return () => {
      if (unsubscribeTechs) unsubscribeTechs();
      if (unsubscribeCases) unsubscribeCases();
      if (unsubscribeNotifs) unsubscribeNotifs();
      if (unsubscribeLogs) unsubscribeLogs();
      if (unsubscribeSession) unsubscribeSession();
    };
  }, [isLoggedIn]);

  // Compute active OPEN cases count map per technician
  const activeCasesCountMap = useMemo(() => {
    const map: Record<string, number> = {};
    cases.forEach(c => {
      if (c.status === "OPEN") {
        map[c.technicianId] = (map[c.technicianId] || 0) + 1;
      }
    });
    return map;
  }, [cases]);

  const openCasesCount = useMemo(() => {
    return cases.filter(c => c.status === "OPEN").length;
  }, [cases]);

  const handleSelectCaseId = (caseId: string) => {
    const found = cases.find(c => c.caseId === caseId);
    if (found) {
      setSelectedCase(found);
    }
  };

  // If URL points to mobile check-in view for a technician
  if (mobileCheckInCaseId) {
    return (
      <MobileCheckInView 
        caseId={mobileCheckInCaseId} 
        onBackToAdmin={() => {
          if (typeof window !== "undefined") {
            const url = new URL(window.location.href);
            url.searchParams.delete("view");
            url.searchParams.delete("caseId");
            url.searchParams.delete("action");
            window.history.replaceState({}, document.title, url.pathname);
          }
          setMobileCheckInCaseId(null);
        }}
      />
    );
  }

  // If still loading auth state and they supposedly are logged in, show loader
  const savedLoggedIn = sessionStorage.getItem("worklog_admin_logged_in") === "true";
  if (isAuthLoading && savedLoggedIn) {
    return (
      <div className="min-h-screen bg-[#090D16] flex flex-col items-center justify-center space-y-3">
        <Loader2 className="w-7 h-7 text-orange-500 animate-spin" />
        <p className="text-xs text-slate-400 font-['Prompt']">กำลังตรวจสอบสิทธิ์...</p>
      </div>
    );
  }

  // If not logged in, show Login Screen
  if (!isLoggedIn) {
    return <LoginScreen onLoginSuccess={handleLoginSuccess} />;
  }

  return (
    <div id="worklog-operations-app" className={`min-h-screen ${theme === "light" ? "light-mode bg-slate-50 text-slate-900" : "bg-[#090D16] text-slate-100"} flex flex-col font-['Prompt'] selection:bg-orange-500 selection:text-white`}>
      {/* Top Navigation */}
      <Navbar
        activeTab={activeTab}
        onTabChange={setActiveTab}
        openCasesCount={openCasesCount}
        notifications={notifications}
        onOpenCreateModal={() => setIsCreateModalOpen(true)}
        onSelectCaseId={handleSelectCaseId}
        onLogout={handleLogout}
        theme={theme}
        onToggleTheme={handleToggleTheme}
      />

      {/* Main View Container */}
      <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-6">
        {isLoading ? (
          <div className="py-32 flex flex-col items-center justify-center space-y-3">
            <Loader2 className="w-7 h-7 text-orange-500 animate-spin" />
            <p className="text-xs text-slate-400">กำลังโหลดข้อมูลจาก Firestore...</p>
          </div>
        ) : (
          <>
            {activeTab === "OVERVIEW" && (
              <OverviewDashboard
                cases={cases}
                technicians={technicians}
                onSelectCase={(c) => setSelectedCase(c)}
                onSwitchToMonitoring={() => setActiveTab("CASES")}
              />
            )}

            {activeTab === "CASES" && (
              <CaseMonitoring
                cases={cases}
                technicians={technicians}
                onSelectCase={(c) => setSelectedCase(c)}
                onOpenCreateModal={() => setIsCreateModalOpen(true)}
                onPrintRequest={(c) => setPrintCase(c)}
              />
            )}

            {activeTab === "TECHS" && (
              <TechManagement
                technicians={technicians}
                activeCasesCountMap={activeCasesCountMap}
              />
            )}

            {activeTab === "ANALYTICS" && (
              <TechPerformanceAnalytics
                cases={cases}
                technicians={technicians}
              />
            )}

            {activeTab === "LOGS" && (
              <SystemLogsView
                logs={systemLogs}
                cases={cases}
                onSelectCaseId={handleSelectCaseId}
              />
            )}
          </>
        )}
      </main>

      {/* Live Toast Notifications Container */}
      <ToastContainer
        toasts={toasts}
        onDismiss={handleDismissToast}
        onSelectCase={handleSelectCaseId}
      />

      {/* Footer */}
      <footer className="border-t border-slate-900 bg-[#090D16] py-3.5 text-center text-xs text-slate-500">
        <div className="max-w-7xl mx-auto px-4 flex flex-col sm:flex-row items-center justify-between gap-2">
          <div className="flex items-center space-x-2 text-[11px]">
            <span className="w-1.5 h-1.5 rounded-full bg-emerald-400"></span>
            <span>Firestore Real-time Active • WORKLOG · OPERATIONS</span>
          </div>
          <div className="text-[11px] text-slate-500 flex items-center space-x-1.5">
            <ShieldCheck className="w-3.5 h-3.5 text-emerald-400" />
            <span>Admin Single-Device Session Active (1 อุปกรณ์ต่อ 1 บัญชี)</span>
          </div>
        </div>
      </footer>

      {/* Create / Dispatch Case Modal */}
      <CreateCaseModal
        isOpen={isCreateModalOpen}
        onClose={() => setIsCreateModalOpen(false)}
        technicians={technicians}
        onCaseCreated={(caseId) => {
          const found = cases.find(c => c.caseId === caseId);
          if (found) setSelectedCase(found);
        }}
      />

      {/* Case Details & Action Modal */}
      {selectedCase && (
        <CaseDetailModal
          isOpen={!!selectedCase}
          repairCase={selectedCase}
          technicians={technicians}
          onClose={() => setSelectedCase(null)}
          onPrintRequest={(c) => setPrintCase(c)}
        />
      )}

      {/* Printable Work Order Modal */}
      {printCase && (
        <PrintWorkOrderModal
          isOpen={!!printCase}
          repairCase={printCase}
          onClose={() => setPrintCase(null)}
        />
      )}
    </div>
  );
}
