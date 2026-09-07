import React, { useState, useEffect } from "react";
import { 
  Wrench, 
  Layers, 
  Users, 
  FileCode2, 
  Bell, 
  PlusCircle, 
  Menu, 
  X,
  LogOut,
  Shield,
  BarChart2,
  Sun,
  Moon
} from "lucide-react";
import { NotificationDoc, RepairCase } from "../types";
import { markNotificationAsRead } from "../services/notificationService";
import { Logo } from "./Logo";

export type NavTab = "OVERVIEW" | "CASES" | "TECHS" | "ANALYTICS" | "LOGS";

interface NavbarProps {
  activeTab: NavTab;
  onTabChange: (tab: NavTab) => void;
  openCasesCount: number;
  notifications: NotificationDoc[];
  onOpenCreateModal: () => void;
  onSelectCaseId?: (caseId: string) => void;
  onLogout: () => void;
  theme: "dark" | "light";
  onToggleTheme: () => void;
}

export const Navbar: React.FC<NavbarProps> = ({
  activeTab,
  onTabChange,
  openCasesCount,
  notifications,
  onOpenCreateModal,
  onSelectCaseId,
  onLogout,
  theme,
  onToggleTheme
}) => {
  const [showNotifDropdown, setShowNotifDropdown] = useState(false);
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  // Close mobile menu on Escape key or when viewport expands to desktop
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === "Escape") {
        setMobileMenuOpen(false);
        setShowNotifDropdown(false);
      }
    };
    const handleResize = () => {
      if (window.innerWidth >= 768) {
        setMobileMenuOpen(false);
      }
    };
    window.addEventListener("keydown", handleKeyDown);
    window.addEventListener("resize", handleResize);
    return () => {
      window.removeEventListener("keydown", handleKeyDown);
      window.removeEventListener("resize", handleResize);
    };
  }, []);

  const unreadNotifs = notifications.filter(n => !n.isRead);

  const handleNotificationClick = async (notif: NotificationDoc) => {
    if (notif.id) {
      await markNotificationAsRead(notif.id);
    }
    if (notif.caseId && onSelectCaseId) {
      onSelectCaseId(notif.caseId);
      setShowNotifDropdown(false);
    }
  };

  const navItems: { id: NavTab; label: string; icon: React.ReactNode; badge?: number }[] = [
    { id: "OVERVIEW", label: "ภาพรวม & แดชบอร์ด", icon: <Layers className="w-4 h-4" /> },
    { id: "CASES", label: "เคสงานซ่อม", icon: <Wrench className="w-4 h-4" />, badge: openCasesCount },
    { id: "TECHS", label: "ช่างเทคนิค", icon: <Users className="w-4 h-4" /> },
    { id: "ANALYTICS", label: "รายงานวิเคราะห์", icon: <BarChart2 className="w-4 h-4" /> },
    { id: "LOGS", label: "บันทึกระบบ", icon: <FileCode2 className="w-4 h-4" /> }
  ];

  return (
    <header className={`sticky top-0 z-40 backdrop-blur-md border-b transition-colors w-full overflow-x-clip ${
      theme === "light" 
        ? "bg-white/95 border-slate-300 shadow-sm" 
        : "bg-[#090D16]/95 border-slate-800/80"
    }`}>
      <div className="max-w-7xl mx-auto px-3 sm:px-6 lg:px-8 w-full">
        <div className="flex items-center justify-between h-16 gap-2 w-full">
          
          {/* Official Brand Logo & Title */}
          <div 
            className="flex items-center cursor-pointer select-none min-w-0 shrink"
            onClick={() => onTabChange("OVERVIEW")}
          >
            <Logo size={32} showText={true} textSize="text-xs sm:text-base" subtextSize="text-[9px]" />
          </div>

          {/* Desktop Navigation Tabs - Minimalist Pill Bar */}
          <nav className={`hidden md:flex items-center space-x-1 p-1 rounded-xl border ${
            theme === "light"
              ? "bg-slate-100 border-slate-300"
              : "bg-slate-900/60 border-slate-800/80"
          }`}>
            {navItems.map((item) => {
              const isActive = activeTab === item.id;
              return (
                <button
                  key={item.id}
                  onClick={() => onTabChange(item.id)}
                  className={`px-3.5 py-1.5 rounded-lg text-xs font-medium flex items-center space-x-2 transition-all ${
                    isActive
                      ? theme === "light"
                        ? "bg-white text-slate-900 font-bold shadow-sm border border-slate-300"
                        : "bg-slate-800 text-white font-semibold shadow-sm border border-slate-700/60"
                      : theme === "light"
                        ? "text-slate-600 hover:text-slate-900 hover:bg-slate-200/70"
                        : "text-slate-400 hover:text-slate-200 hover:bg-slate-800/40"
                  }`}
                >
                  <span className={isActive ? (theme === "light" ? "text-orange-600" : "text-orange-400") : (theme === "light" ? "text-slate-500" : "text-slate-400")}>{item.icon}</span>
                  <span>{item.label}</span>
                  {item.badge !== undefined && item.badge > 0 && (
                    <span className={`px-1.5 py-0.2 rounded-full text-[10px] font-bold font-mono ${
                      theme === "light"
                        ? "bg-orange-100 text-orange-700 border border-orange-300"
                        : "bg-orange-500/20 text-orange-400 border border-orange-500/30"
                    }`}>
                      {item.badge}
                    </span>
                  )}
                </button>
              );
            })}
          </nav>

          {/* Right Actions: Status Badge, Notifications, Create Action, Theme, Logout, Mobile Menu */}
          <div className="flex items-center gap-1.5 sm:gap-2 shrink-0 ml-auto">
            {/* Single Device Status Badge (Desktop only) */}
            <div className={`hidden lg:flex items-center space-x-1.5 px-2.5 py-1 rounded-xl text-[11px] font-semibold border shrink-0 ${
              theme === "light"
                ? "bg-emerald-50 border-emerald-300 text-emerald-800"
                : "bg-slate-900 border-slate-800 text-emerald-400"
            }`} title="ระบบล็อกอินจำกัด 1 อุปกรณ์ต่อ 1 บัญชีผู้ใช้">
              <Shield className={`w-3.5 h-3.5 ${theme === "light" ? "text-emerald-700" : "text-emerald-400"}`} />
              <span>1 Device Active</span>
            </div>

            {/* Notification Bell */}
            <div className="relative shrink-0">
              <button
                onClick={() => setShowNotifDropdown(!showNotifDropdown)}
                className={`w-9 h-9 sm:w-10 sm:h-10 flex items-center justify-center rounded-xl transition-colors relative border shrink-0 ${
                  theme === "light"
                    ? "text-slate-700 hover:text-slate-900 hover:bg-slate-100 border-slate-300 bg-white shadow-xs"
                    : "text-slate-400 hover:text-slate-200 hover:bg-slate-900 border-slate-800/80"
                }`}
                title="การแจ้งเตือนงาน"
                aria-label="การแจ้งเตือนงาน"
              >
                <Bell className="w-4 h-4" />
                {unreadNotifs.length > 0 && (
                  <span className="absolute top-1.5 right-1.5 w-2 h-2 rounded-full bg-orange-500 ring-2 ring-slate-950 animate-pulse"></span>
                )}
              </button>

              {/* Notification Popover */}
              {showNotifDropdown && (
                <>
                  <div 
                    className="fixed inset-0 z-40 bg-transparent" 
                    onClick={() => setShowNotifDropdown(false)} 
                    aria-hidden="true"
                  />
                  <div 
                    className={`absolute -right-10 sm:right-0 mt-2 w-80 max-w-[calc(100vw-1.5rem)] rounded-2xl shadow-2xl overflow-hidden z-50 animate-fadeIn border ${
                      theme === "light"
                        ? "bg-white border-slate-300 text-slate-900"
                        : "bg-slate-900 border-slate-800 text-slate-100"
                    }`}
                    onClick={(e) => e.stopPropagation()}
                  >
                    <div className={`px-4 py-3 border-b flex items-center justify-between ${
                      theme === "light"
                        ? "bg-slate-50 border-slate-200"
                        : "bg-slate-950/80 border-slate-800"
                    }`}>
                      <div className="flex items-center space-x-2">
                        <Bell className="w-4 h-4 text-orange-500" />
                        <h4 className="text-xs font-bold">การแจ้งเตือนงาน (Notifications)</h4>
                      </div>
                      <span className={`text-[10px] px-2 py-0.5 rounded-full font-bold ${
                        theme === "light"
                          ? "text-slate-700 bg-slate-200"
                          : "text-slate-400 bg-slate-800"
                      }`}>
                        {unreadNotifs.length} ใหม่
                      </span>
                    </div>
                    <div className={`max-h-80 overflow-y-auto divide-y ${
                      theme === "light" ? "divide-slate-200" : "divide-slate-800/60"
                    }`}>
                      {notifications.length === 0 ? (
                        <div className={`p-6 text-center text-xs ${theme === "light" ? "text-slate-500" : "text-slate-500"}`}>
                          ยังไม่มีการแจ้งเตือนในระบบ
                        </div>
                      ) : (
                        notifications.map((notif) => (
                          <div 
                            key={notif.id}
                            onClick={() => handleNotificationClick(notif)}
                            className={`p-3 text-xs transition-colors cursor-pointer ${
                              theme === "light"
                                ? !notif.isRead ? "bg-orange-50/80 hover:bg-orange-100/70" : "hover:bg-slate-50"
                                : !notif.isRead ? "bg-orange-950/20 hover:bg-slate-800/50" : "hover:bg-slate-800/50"
                            }`}
                          >
                            <div className="flex items-start justify-between gap-2">
                              <span className={`font-semibold ${theme === "light" ? "text-slate-900" : "text-slate-200"}`}>{notif.customerName}</span>
                              <span className={`text-[10px] font-mono ${theme === "light" ? "text-slate-500" : "text-slate-500"}`}>
                                {notif.timestamp?.toMillis ? new Date(notif.timestamp.toMillis()).toLocaleTimeString('th-TH', { hour: '2-digit', minute: '2-digit' }) : "เมื่อสักครู่"}
                              </span>
                            </div>
                            <p className={`mt-1 line-clamp-2 ${theme === "light" ? "text-slate-600" : "text-slate-400"}`}>{notif.message}</p>
                          </div>
                        ))
                      )}
                    </div>
                  </div>
                </>
              )}
            </div>

            {/* Clean "+ เปิดเคสใหม่" Button (Icon on mobile, Icon+Text on sm+) */}
            <button
              onClick={onOpenCreateModal}
              className="h-9 sm:h-10 px-2.5 sm:px-3.5 bg-orange-600 hover:bg-orange-500 active:scale-95 text-white rounded-xl text-xs font-bold flex items-center justify-center space-x-1.5 transition-all shadow-md shadow-orange-600/20 shrink-0"
              title="เปิดเคสงานซ่อมใหม่"
              aria-label="เปิดเคสงานซ่อมใหม่"
            >
              <PlusCircle className="w-4 h-4 text-white shrink-0" />
              <span className="hidden sm:inline text-white font-medium">เปิดเคสใหม่</span>
            </button>

            {/* Desktop Theme Toggle (Visible only on md+ desktop, available in mobile drawer on mobile) */}
            <button
              onClick={onToggleTheme}
              className={`hidden md:flex w-10 h-10 items-center justify-center rounded-xl transition-colors cursor-pointer border shrink-0 ${
                theme === "light"
                  ? "text-slate-700 hover:text-slate-900 hover:bg-slate-100 border-slate-300 bg-white"
                  : "text-slate-400 hover:text-slate-200 hover:bg-slate-900 border-slate-800/80"
              }`}
              title={theme === "dark" ? "เปลี่ยนเป็นโหมดสว่าง" : "เปลี่ยนเป็นโหมดมืด"}
              aria-label={theme === "dark" ? "เปลี่ยนเป็นโหมดสว่าง" : "เปลี่ยนเป็นโหมดมืด"}
            >
              {theme === "dark" ? (
                <Sun className="w-4 h-4 text-amber-400 animate-pulse" />
              ) : (
                <Moon className="w-4 h-4 text-indigo-600" />
              )}
            </button>

            {/* Desktop Logout Button (Visible only on md+ desktop, available in mobile drawer on mobile) */}
            <button
              onClick={onLogout}
              className={`hidden md:flex w-10 h-10 items-center justify-center rounded-xl transition-colors border shrink-0 ${
                theme === "light"
                  ? "text-rose-600 hover:text-rose-800 hover:bg-rose-50 border-rose-300 bg-rose-50/20"
                  : "text-rose-400 hover:text-rose-200 hover:bg-rose-950/40 border-rose-900/40"
              }`}
              title="ออกจากระบบทันที"
              aria-label="ออกจากระบบทันที"
            >
              <LogOut className={`w-4 h-4 ${theme === "light" ? "text-rose-600" : "text-rose-400"}`} />
            </button>

            {/* Mobile Hamburger Toggle (Always visible, firmly anchored to the right, easy to tap) */}
            <button
              onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
              className={`w-9 h-9 sm:w-10 sm:h-10 md:hidden rounded-xl border flex items-center justify-center transition-all shrink-0 active:scale-95 ${
                mobileMenuOpen
                  ? theme === "light"
                    ? "text-orange-600 bg-orange-50 border-orange-300 ring-2 ring-orange-500/20 shadow-xs"
                    : "text-orange-400 bg-orange-950/50 border-orange-500/50 ring-2 ring-orange-500/30"
                  : theme === "light"
                    ? "text-slate-800 hover:text-slate-950 hover:bg-slate-100 border-slate-300 bg-white shadow-xs"
                    : "text-slate-200 hover:text-white hover:bg-slate-800 border-slate-700/80 bg-slate-900"
              }`}
              aria-label={mobileMenuOpen ? "ปิดเมนูหลัก" : "เปิดเมนูหลัก"}
              aria-expanded={mobileMenuOpen}
              title={mobileMenuOpen ? "ปิดเมนูหลัก" : "เปิดเมนูหลัก"}
            >
              {mobileMenuOpen ? (
                <X className="w-5 h-5 transition-transform duration-200" />
              ) : (
                <Menu className="w-5 h-5 transition-transform duration-200" />
              )}
            </button>
          </div>
        </div>

        {/* Mobile Backdrop Overlay */}
        {mobileMenuOpen && (
          <div
            className="fixed inset-0 top-16 bg-black/50 backdrop-blur-xs z-30 md:hidden animate-fadeIn"
            onClick={() => setMobileMenuOpen(false)}
            aria-hidden="true"
          />
        )}

        {/* Mobile Navigation Drawer */}
        {mobileMenuOpen && (
          <div className={`md:hidden relative z-40 py-3.5 border-t space-y-3 animate-fadeIn max-h-[calc(100vh-4.25rem)] overflow-y-auto overscroll-contain ${
            theme === "light" ? "border-slate-200 bg-white" : "border-slate-800/80 bg-[#090D16]"
          }`}>
            <div className={`px-3 py-1.5 flex items-center justify-between text-xs font-mono rounded-xl border ${
              theme === "light"
                ? "bg-emerald-50 border-emerald-300 text-emerald-800 font-bold"
                : "bg-slate-900/80 border-slate-800 text-emerald-400"
            }`}>
              <span className="flex items-center space-x-1.5"><Shield className="w-3.5 h-3.5" /><span>สถานะระบบ:</span></span>
              <span className="font-bold">1 Device Active</span>
            </div>

            {/* Quick Action Button in Drawer */}
            <button
              onClick={() => {
                onOpenCreateModal();
                setMobileMenuOpen(false);
              }}
              className="w-full py-2.5 px-4 bg-orange-600 hover:bg-orange-500 active:scale-98 text-white rounded-xl text-xs font-bold flex items-center justify-center space-x-2 shadow-md shadow-orange-600/20"
            >
              <PlusCircle className="w-4 h-4 text-white" />
              <span className="text-white font-bold">เปิดเคสงานซ่อมใหม่</span>
            </button>

            {/* Navigation Tabs */}
            <div className="space-y-1">
              <div className={`text-[10px] uppercase font-bold tracking-wider px-2 py-0.5 ${
                theme === "light" ? "text-slate-500" : "text-slate-400"
              }`}>
                เมนูหลัก (Navigation)
              </div>
              {navItems.map((item) => (
                <button
                  key={item.id}
                  onClick={() => {
                    onTabChange(item.id);
                    setMobileMenuOpen(false);
                  }}
                  className={`w-full px-4 py-2.5 rounded-xl text-xs font-semibold flex items-center justify-between transition-colors ${
                    activeTab === item.id
                      ? theme === "light"
                        ? "bg-orange-50 text-orange-700 font-bold border border-orange-200"
                        : "bg-slate-800 text-white font-bold border border-slate-700"
                      : theme === "light"
                        ? "text-slate-700 hover:text-slate-900 hover:bg-slate-100"
                        : "text-slate-400 hover:text-slate-200 hover:bg-slate-900"
                  }`}
                >
                  <div className="flex items-center space-x-2.5">
                    <span className={activeTab === item.id ? (theme === "light" ? "text-orange-600" : "text-orange-400") : ""}>{item.icon}</span>
                    <span>{item.label}</span>
                  </div>
                  {item.badge !== undefined && item.badge > 0 && (
                    <span className={`px-2 py-0.5 rounded-full text-[10px] font-bold ${
                      theme === "light"
                        ? "bg-orange-100 text-orange-700 border border-orange-300"
                        : "bg-orange-500/20 text-orange-400 border border-orange-500/30"
                    }`}>
                      {item.badge}
                    </span>
                  )}
                </button>
              ))}
            </div>

            {/* Mobile Preferences & Logout Section */}
            <div className={`pt-2.5 border-t space-y-2 ${
              theme === "light" ? "border-slate-200" : "border-slate-800"
            }`}>
              <div className={`text-[10px] uppercase font-bold tracking-wider px-2 ${
                theme === "light" ? "text-slate-500" : "text-slate-400"
              }`}>
                การตั้งค่าและระบบ
              </div>

              {/* Theme Toggle Button in Drawer */}
              <button
                onClick={() => {
                  onToggleTheme();
                }}
                className={`w-full px-4 py-2.5 rounded-xl text-xs font-semibold flex items-center justify-between transition-colors border ${
                  theme === "light"
                    ? "bg-slate-50 hover:bg-slate-100 border-slate-300 text-slate-800"
                    : "bg-slate-900/60 hover:bg-slate-800 border-slate-800 text-slate-300"
                }`}
              >
                <div className="flex items-center space-x-2.5">
                  {theme === "dark" ? (
                    <Sun className="w-4 h-4 text-amber-400" />
                  ) : (
                    <Moon className="w-4 h-4 text-indigo-600" />
                  )}
                  <span>โหมดการแสดงผล (Theme)</span>
                </div>
                <span className={`text-[11px] px-2.5 py-0.5 rounded-md font-medium ${
                  theme === "light" ? "bg-slate-200 text-slate-800" : "bg-slate-800 text-slate-300"
                }`}>
                  {theme === "light" ? "โหมดสว่าง (Light)" : "โหมดมืด (Dark)"}
                </span>
              </button>

              {/* Logout Button in Drawer */}
              <button
                onClick={() => {
                  setMobileMenuOpen(false);
                  onLogout();
                }}
                className={`w-full px-4 py-2.5 rounded-xl text-xs font-bold flex items-center justify-center space-x-2 transition-colors border ${
                  theme === "light"
                    ? "bg-rose-50 hover:bg-rose-100 border-rose-200 text-rose-600"
                    : "bg-rose-950/30 hover:bg-rose-950/60 border-rose-900/50 text-rose-400"
                }`}
              >
                <LogOut className="w-4 h-4" />
                <span>ออกจากระบบ (Logout)</span>
              </button>
            </div>
          </div>
        )}
      </div>
    </header>
  );
};
