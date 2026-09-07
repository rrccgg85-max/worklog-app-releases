import React, { useState, useMemo } from "react";
import { 
  Clock, 
  CheckCircle2, 
  Users, 
  ArrowRight,
  Activity,
  Layers,
  Search
} from "lucide-react";
import { 
  BarChart, 
  Bar, 
  XAxis, 
  YAxis, 
  Tooltip, 
  ResponsiveContainer
} from "recharts";
import { RepairCase, TechUser } from "../types";

interface OverviewDashboardProps {
  cases: RepairCase[];
  technicians: TechUser[];
  onSelectCase: (repairCase: RepairCase) => void;
  onSwitchToMonitoring: () => void;
}

export const OverviewDashboard: React.FC<OverviewDashboardProps> = ({
  cases,
  technicians,
  onSelectCase,
  onSwitchToMonitoring
}) => {
  // Statistics
  const totalCases = cases.length;
  const openCases = useMemo(() => cases.filter(c => c.status === "OPEN"), [cases]);
  const closedCases = useMemo(() => cases.filter(c => c.status === "CLOSED"), [cases]);
  const activeTechs = useMemo(() => technicians.filter(t => t.isActive), [technicians]);
  const completionRate = totalCases > 0 ? Math.round((closedCases.length / totalCases) * 100) : 0;

  // Search & Filter state for the table
  const [searchTerm, setSearchTerm] = useState("");
  const [statusFilter, setStatusFilter] = useState<"ALL" | "OPEN" | "CLOSED">("ALL");

  const filteredCases = useMemo(() => {
    return cases.filter(c => {
      const matchSearch = 
        c.caseId.toLowerCase().includes(searchTerm.toLowerCase()) ||
        c.customerName.toLowerCase().includes(searchTerm.toLowerCase()) ||
        c.details.toLowerCase().includes(searchTerm.toLowerCase()) ||
        c.technicianName.toLowerCase().includes(searchTerm.toLowerCase());
      const matchStatus = statusFilter === "ALL" ? true : c.status === statusFilter;
      return matchSearch && matchStatus;
    });
  }, [cases, searchTerm, statusFilter]);

  const categoryChartData = useMemo(() => {
    const counts: Record<string, number> = {};
    cases.forEach((c) => {
      counts[c.category] = (counts[c.category] || 0) + 1;
    });
    return Object.entries(counts)
      .map(([cat, count]) => ({
        name: cat.length > 10 ? cat.slice(0, 9) + ".." : cat,
        fullName: cat,
        count
      }))
      .slice(0, 6);
  }, [cases]);

  const getTechActiveJobCount = (techId: string) => {
    return cases.filter(c => c.technicianId === techId && c.status === "OPEN").length;
  };

  return (
    <div id="overview-clean-dashboard" className="space-y-6 animate-fadeIn font-['Prompt']">
      
      {/* Clean Top Bar Header */}
      <div className="pb-1">
        <h1 className="text-xl font-bold text-white tracking-tight">
          ภาพรวมงานซ่อม & การจ่ายงาน
        </h1>
        <p className="text-xs text-slate-400 mt-0.5">
          ระบบติดตามสถานะงานซ่อมแบบเรียลไทม์ ซิงค์อัตโนมัติกับ Firestore
        </p>
      </div>

      {/* 3 Clean Minimal KPI Metric Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-3.5 sm:gap-4">
        {/* Total Cases */}
        <div className="bg-slate-900/90 border border-slate-800/80 rounded-xl p-4 flex flex-col justify-between transition-colors hover:border-slate-700">
          <div className="flex items-center justify-between">
            <span className="text-xs font-medium text-slate-400">เคสทั้งหมด</span>
            <div className="w-7 h-7 rounded-lg bg-slate-800 text-slate-300 flex items-center justify-center">
              <Layers className="w-3.5 h-3.5" />
            </div>
          </div>
          <div className="mt-2 flex items-baseline gap-2">
            <span className="text-2xl font-bold text-white font-mono">{totalCases}</span>
            <span className="text-[11px] text-slate-400">รายการ</span>
          </div>
        </div>

        {/* Pending (OPEN) */}
        <div className="bg-slate-900/90 border border-slate-800/80 rounded-xl p-4 flex flex-col justify-between transition-colors hover:border-amber-500/40">
          <div className="flex items-center justify-between">
            <span className="text-xs font-medium text-amber-400">รอดำเนินการ</span>
            <div className="w-7 h-7 rounded-lg bg-amber-500/10 text-amber-400 flex items-center justify-center">
              <Clock className="w-3.5 h-3.5" />
            </div>
          </div>
          <div className="mt-2 flex items-baseline gap-2">
            <span className="text-2xl font-bold text-amber-400 font-mono">{openCases.length}</span>
            <span className="text-[11px] text-amber-400/80">เคสเปิด</span>
          </div>
        </div>

        {/* Closed */}
        <div className="bg-slate-900/90 border border-slate-800/80 rounded-xl p-4 flex flex-col justify-between transition-colors hover:border-emerald-500/40">
          <div className="flex items-center justify-between">
            <span className="text-xs font-medium text-emerald-400">ปิดงานสำเร็จ</span>
            <div className="w-7 h-7 rounded-lg bg-emerald-500/10 text-emerald-400 flex items-center justify-center">
              <CheckCircle2 className="w-3.5 h-3.5" />
            </div>
          </div>
          <div className="mt-2 flex items-baseline gap-2">
            <span className="text-2xl font-bold text-emerald-400 font-mono">{closedCases.length}</span>
            <span className="text-[11px] text-emerald-400 font-medium">({completionRate}%)</span>
          </div>
        </div>
      </div>

      {/* Main Full-Width Live Cases Table */}
      <div className="bg-slate-900/80 border border-slate-800/80 rounded-xl flex flex-col overflow-hidden">
        
        {/* Table Header & Search Filters */}
        <div className="p-4 border-b border-slate-800 flex flex-col sm:flex-row sm:items-center justify-between gap-3">
          <div className="flex items-center gap-2">
            <h2 className="font-semibold text-white text-sm">รายการเคสล่าสุด</h2>
            <span className="text-xs text-slate-500 font-mono">({filteredCases.length})</span>
          </div>

          {/* Filter and Search Bar */}
          <div className="flex items-center gap-2">
            <div className="relative">
              <Search className="w-3.5 h-3.5 text-slate-500 absolute left-2.5 top-1/2 -translate-y-1/2" />
              <input
                type="text"
                placeholder="ค้นหาเคส..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                className="bg-slate-950 border border-slate-800 rounded-lg pl-8 pr-3 py-1.5 text-xs text-slate-200 placeholder-slate-500 focus:outline-none focus:border-slate-700 w-40 sm:w-56"
              />
            </div>

            {/* Status Filter Toggle */}
            <div className="flex rounded-lg bg-slate-950 border border-slate-800 p-0.5 text-[11px]">
              <button
                onClick={() => setStatusFilter("ALL")}
                className={`px-2.5 py-1 rounded-md transition-colors ${statusFilter === "ALL" ? "bg-slate-800 text-white font-medium" : "text-slate-400 hover:text-slate-200"}`}
              >
                ทั้งหมด
              </button>
              <button
                onClick={() => setStatusFilter("OPEN")}
                className={`px-2.5 py-1 rounded-md transition-colors ${statusFilter === "OPEN" ? "bg-amber-500/20 text-amber-300 font-medium" : "text-slate-400 hover:text-slate-200"}`}
              >
                เปิด
              </button>
              <button
                onClick={() => setStatusFilter("CLOSED")}
                className={`px-2.5 py-1 rounded-md transition-colors ${statusFilter === "CLOSED" ? "bg-emerald-500/20 text-emerald-300 font-medium" : "text-slate-400 hover:text-slate-200"}`}
              >
                ปิด
              </button>
            </div>

            <button
              onClick={onSwitchToMonitoring}
              className="text-xs text-orange-400 hover:text-orange-300 font-medium flex items-center gap-1 pl-1"
            >
              <span>ดูทั้งหมด</span>
              <ArrowRight className="w-3 h-3" />
            </button>
          </div>
        </div>

        {/* Table Body */}
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead className="bg-slate-950/60 text-slate-400 text-[11px] uppercase tracking-wider border-b border-slate-800/60">
              <tr>
                <th className="px-4 py-3 font-medium">รหัสเคส</th>
                <th className="px-4 py-3 font-medium">ลูกค้า & รายละเอียด</th>
                <th className="px-4 py-3 font-medium">หมวดหมู่</th>
                <th className="px-4 py-3 font-medium">ช่างผู้รับผิดชอบ</th>
                <th className="px-4 py-3 font-medium text-right">สถานะ</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800/50">
              {filteredCases.length === 0 ? (
                <tr>
                  <td colSpan={5} className="py-12 text-center text-slate-500 text-xs">
                    {searchTerm || statusFilter !== "ALL" ? "ไม่พบเคสที่ตรงกับเงื่อนไขการค้นหา" : "ยังไม่มีรายการเคสงานซ่อมในระบบ — กด \"+ เปิดเคสใหม่\" ด้านบนเพื่อเริ่มต้น"}
                  </td>
                </tr>
              ) : (
                filteredCases.slice(0, 10).map((c) => (
                  <tr
                    key={c.caseId}
                    onClick={() => onSelectCase(c)}
                    className="hover:bg-slate-800/40 cursor-pointer transition-colors"
                  >
                    <td className="px-4 py-3.5 whitespace-nowrap font-mono text-xs font-semibold text-orange-400">
                      {c.caseId}
                    </td>
                    <td className="px-4 py-3.5 max-w-[280px]">
                      <div className="font-medium text-slate-200 truncate">{c.customerName}</div>
                      <div className="text-[11px] text-slate-400 truncate">{c.details}</div>
                    </td>
                    <td className="px-4 py-3.5 whitespace-nowrap text-slate-300 text-xs">
                      {c.category}
                    </td>
                    <td className="px-4 py-3.5 whitespace-nowrap text-slate-300 text-xs">
                      {c.technicianName}
                    </td>
                    <td className="px-4 py-3.5 text-right whitespace-nowrap">
                      {c.status === "CLOSED" ? (
                        <span className="px-2.5 py-1 bg-emerald-500/10 text-emerald-400 rounded-md text-[10px] font-medium border border-emerald-500/30">
                          เสร็จสิ้น
                        </span>
                      ) : (
                        <span className="px-2.5 py-1 bg-amber-500/10 text-amber-400 rounded-md text-[10px] font-medium border border-amber-500/30">
                          รอดำเนินการ
                        </span>
                      )}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Bottom Row: Technician Status Summary & Simple Chart */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        
        {/* Technicians Available */}
        <div className="bg-slate-900/80 border border-slate-800/80 rounded-xl p-4">
          <div className="flex items-center justify-between mb-3">
            <h2 className="text-xs font-semibold text-slate-300 flex items-center gap-1.5">
              <Users className="w-3.5 h-3.5 text-orange-400" />
              สถานะช่างเทคนิค
            </h2>
            <span className="text-[11px] text-slate-500 font-mono">ออนไลน์ {activeTechs.length}/{technicians.length}</span>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 max-h-44 overflow-y-auto pr-1">
            {technicians.map((t) => {
              const activeJobs = getTechActiveJobCount(t.id);
              return (
                <div key={t.id} className="p-2.5 rounded-lg bg-slate-950 border border-slate-800/80 flex items-center justify-between">
                  <div className="min-w-0 pr-2">
                    <div className="text-xs font-medium text-slate-200 truncate">{t.name}</div>
                    <div className="text-[10px] text-slate-500">{t.skills?.[0] || "ช่างทั่วไป"}</div>
                  </div>
                  <span className={`text-[10px] px-2 py-0.5 rounded-md font-medium shrink-0 ${
                    activeJobs > 0 ? "bg-amber-500/10 text-amber-400 border border-amber-500/20" : "bg-emerald-500/10 text-emerald-400 border border-emerald-500/20"
                  }`}>
                    {activeJobs > 0 ? `${activeJobs} งาน` : "ว่าง"}
                  </span>
                </div>
              );
            })}
          </div>
        </div>

        {/* Categories Distribution */}
        <div className="bg-slate-900/80 border border-slate-800/80 rounded-xl p-4">
          <div className="flex items-center justify-between mb-2">
            <h2 className="text-xs font-semibold text-slate-300 flex items-center gap-1.5">
              <Activity className="w-3.5 h-3.5 text-orange-400" />
              สถิติหมวดหมู่งาน
            </h2>
          </div>

          <div className="h-36 w-full">
            {cases.length === 0 ? (
              <div className="h-full flex items-center justify-center text-slate-500 text-xs">
                ยังไม่มีข้อมูลงานซ่อม
              </div>
            ) : (
              <ResponsiveContainer width="100%" height="100%">
                <BarChart 
                  data={categoryChartData} 
                  margin={{ top: 5, right: 5, left: -25, bottom: 5 }}
                >
                  <XAxis dataKey="name" stroke="#64748b" fontSize={10} />
                  <YAxis stroke="#64748b" fontSize={10} allowDecimals={false} />
                  <Tooltip 
                    contentStyle={{ backgroundColor: "#0b0f19", borderColor: "#1e293b", borderRadius: "8px", fontSize: "11px", color: "#f8fafc" }}
                  />
                  <Bar dataKey="count" radius={[4, 4, 0, 0]} fill="#f97316" />
                </BarChart>
              </ResponsiveContainer>
            )}
          </div>
        </div>

      </div>

    </div>
  );
};
