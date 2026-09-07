import React, { useMemo } from "react";
import { RepairCase, TechUser } from "../types";
import { 
  Users, 
  CheckCircle2, 
  Clock, 
  Award, 
  BarChart2, 
  Activity,
  Wrench,
  TrendingUp,
  ShieldCheck
} from "lucide-react";
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  Tooltip,
  ResponsiveContainer,
  CartesianGrid,
  Cell
} from "recharts";

interface TechPerformanceAnalyticsProps {
  cases: RepairCase[];
  technicians: TechUser[];
}

export const TechPerformanceAnalytics: React.FC<TechPerformanceAnalyticsProps> = ({
  cases,
  technicians
}) => {
  // Compute performance metrics per technician
  const techStats = useMemo(() => {
    return technicians.map(tech => {
      const techCases = cases.filter(c => c.technicianId === tech.id);
      const totalAssigned = techCases.length;
      const closedCases = techCases.filter(c => c.status === "CLOSED");
      const openCases = techCases.filter(c => c.status === "OPEN");
      const completionRate = totalAssigned > 0 ? Math.round((closedCases.length / totalAssigned) * 100) : 0;

      // Calculate average repair duration (checkInAt -> closedAt) in minutes
      let totalDurationMinutes = 0;
      let durationCount = 0;

      closedCases.forEach(c => {
        if (c.checkInAt && c.closedAt) {
          const checkInTime = typeof c.checkInAt === "number" ? c.checkInAt : (c.checkInAt?.toMillis ? c.checkInAt.toMillis() : new Date(c.checkInAt).getTime());
          const closeTime = typeof c.closedAt === "number" ? c.closedAt : (c.closedAt?.toMillis ? c.closedAt.toMillis() : new Date(c.closedAt).getTime());
          if (checkInTime && closeTime && closeTime > checkInTime) {
            const diffMin = (closeTime - checkInTime) / (1000 * 60);
            totalDurationMinutes += diffMin;
            durationCount++;
          }
        }
      });

      const avgDurationMin = durationCount > 0 ? Math.round(totalDurationMinutes / durationCount) : 0;
      const avgHours = Math.floor(avgDurationMin / 60);
      const avgMins = avgDurationMin % 60;
      const avgDurationText = durationCount > 0 ? (avgHours > 0 ? `${avgHours} ชม. ${avgMins} น.` : `${avgMins} นาที`) : "ไม่มีข้อมูลเวลา";

      return {
        id: tech.id,
        name: tech.name,
        email: tech.email,
        phone: tech.phone,
        isActive: tech.isActive,
        totalAssigned,
        closedCount: closedCases.length,
        openCount: openCases.length,
        completionRate,
        avgDurationMin,
        avgDurationText
      };
    });
  }, [cases, technicians]);

  // Overall statistics
  const totalCompleted = cases.filter(c => c.status === "CLOSED").length;
  const totalOpen = cases.filter(c => c.status === "OPEN").length;
  const overallCompletionRate = cases.length > 0 ? Math.round((totalCompleted / cases.length) * 100) : 0;

  // Chart data for completed cases per tech
  const chartData = useMemo(() => {
    return techStats.map(t => ({
      name: t.name.length > 12 ? t.name.slice(0, 11) + ".." : t.name,
      fullName: t.name,
      closed: t.closedCount,
      active: t.openCount,
      total: t.totalAssigned
    }));
  }, [techStats]);

  const COLORS = ["#f97316", "#3b82f6", "#10b981", "#8b5cf6", "#ec4899", "#06b6d4"];

  return (
    <div className="space-y-6 animate-fadeIn font-['Prompt']">
      
      {/* Header */}
      <div>
        <h1 className="text-xl font-bold text-white tracking-tight">
          รายงานสรุปผลงานช่าง & สถิติเชิงลึก (Performance Analytics)
        </h1>
        <p className="text-xs text-slate-400 mt-0.5">
          ประเมินประสิทธิภาพการปฏิบัติงาน, อัตราความสำเร็จ และระยะเวลาเฉลี่ยในการซ่อมแซมของช่างแต่ละท่าน
        </p>
      </div>

      {/* KPI Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3.5">
        <div className="bg-slate-900/90 border border-slate-800 rounded-xl p-4 flex flex-col justify-between">
          <div className="flex items-center justify-between">
            <span className="text-xs font-medium text-slate-400">ช่างทั้งหมดในระบบ</span>
            <div className="w-7 h-7 rounded-lg bg-blue-500/10 text-blue-400 flex items-center justify-center">
              <Users className="w-3.5 h-3.5" />
            </div>
          </div>
          <div className="mt-2 flex items-baseline gap-2">
            <span className="text-2xl font-bold text-white font-mono">{technicians.length}</span>
            <span className="text-[11px] text-emerald-400 font-medium">({technicians.filter(t => t.isActive).length} Active)</span>
          </div>
        </div>

        <div className="bg-slate-900/90 border border-slate-800 rounded-xl p-4 flex flex-col justify-between">
          <div className="flex items-center justify-between">
            <span className="text-xs font-medium text-emerald-400">เคสที่ปิดสำเร็จรวม</span>
            <div className="w-7 h-7 rounded-lg bg-emerald-500/10 text-emerald-400 flex items-center justify-center">
              <CheckCircle2 className="w-3.5 h-3.5" />
            </div>
          </div>
          <div className="mt-2 flex items-baseline gap-2">
            <span className="text-2xl font-bold text-emerald-400 font-mono">{totalCompleted}</span>
            <span className="text-[11px] text-slate-400">รายการ</span>
          </div>
        </div>

        <div className="bg-slate-900/90 border border-slate-800 rounded-xl p-4 flex flex-col justify-between">
          <div className="flex items-center justify-between">
            <span className="text-xs font-medium text-amber-400">เคสกำลังดำเนินการ</span>
            <div className="w-7 h-7 rounded-lg bg-amber-500/10 text-amber-400 flex items-center justify-center">
              <Clock className="w-3.5 h-3.5" />
            </div>
          </div>
          <div className="mt-2 flex items-baseline gap-2">
            <span className="text-2xl font-bold text-amber-400 font-mono">{totalOpen}</span>
            <span className="text-[11px] text-slate-400">เคสเปิด</span>
          </div>
        </div>

        <div className="bg-slate-900/90 border border-slate-800 rounded-xl p-4 flex flex-col justify-between">
          <div className="flex items-center justify-between">
            <span className="text-xs font-medium text-indigo-400">อัตราความสำเร็จรวม</span>
            <div className="w-7 h-7 rounded-lg bg-indigo-500/10 text-indigo-400 flex items-center justify-center">
              <TrendingUp className="w-3.5 h-3.5" />
            </div>
          </div>
          <div className="mt-2 flex items-baseline gap-2">
            <span className="text-2xl font-bold text-indigo-400 font-mono">{overallCompletionRate}%</span>
            <span className="text-[11px] text-slate-400">ภาพรวม</span>
          </div>
        </div>
      </div>

      {/* Chart Section */}
      <div className="bg-slate-900/85 border border-slate-800 rounded-xl p-4 sm:p-5">
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center space-x-2">
            <BarChart2 className="w-4 h-4 text-orange-400" />
            <h3 className="text-sm font-bold text-white">เปรียบเทียบผลงานการปิดเคสและเคสคงค้างรายบุคคล</h3>
          </div>
          <span className="text-[11px] text-slate-400">อัปเดตแบบเรียลไทม์</span>
        </div>

        <div className="h-64 sm:h-72 w-full">
          <ResponsiveContainer width="100%" height="100%">
            <BarChart data={chartData} margin={{ top: 10, right: 10, left: -20, bottom: 25 }}>
              <CartesianGrid strokeDasharray="3 3" stroke="#1e293b" vertical={false} />
              <XAxis 
                dataKey="name" 
                stroke="#64748b" 
                fontSize={11} 
                tickLine={false}
                interval={0}
                angle={-20}
                textAnchor="end"
              />
              <YAxis stroke="#64748b" fontSize={11} tickLine={false} allowDecimals={false} />
              <Tooltip 
                contentStyle={{ backgroundColor: "#0f172a", borderColor: "#334155", borderRadius: "12px", fontSize: "12px", color: "#f8fafc" }}
                formatter={(value: any, name: string) => [
                  value, 
                  name === "closed" ? "ปิดงานสำเร็จ" : name === "active" ? "กำลังดำเนินการ" : "ทั้งหมด"
                ]}
              />
              <Bar dataKey="closed" name="closed" fill="#10b981" radius={[4, 4, 0, 0]} maxBarSize={32} />
              <Bar dataKey="active" name="active" fill="#f97316" radius={[4, 4, 0, 0]} maxBarSize={32} />
            </BarChart>
          </ResponsiveContainer>
        </div>
      </div>

      {/* Detailed Table per Technician */}
      <div className="bg-slate-900/85 border border-slate-800 rounded-xl overflow-hidden">
        <div className="p-4 border-b border-slate-800 flex items-center justify-between">
          <div className="flex items-center space-x-2">
            <Award className="w-4 h-4 text-orange-400" />
            <h3 className="text-sm font-bold text-white">ตารางสรุปผลงานและเวลาปฏิบัติงาน (Performance Metrics)</h3>
          </div>
          <span className="text-xs text-slate-400 font-mono">{techStats.length} ช่างเทคนิค</span>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead className="bg-slate-950/80 text-slate-400 uppercase font-mono tracking-wider border-b border-slate-800 text-[10px]">
              <tr>
                <th className="px-4 py-3">ช่างเทคนิค</th>
                <th className="px-4 py-3 text-center">รับมอบหมาย (เคส)</th>
                <th className="px-4 py-3 text-center">ปิดงานแล้ว</th>
                <th className="px-4 py-3 text-center">กำลังทำ</th>
                <th className="px-4 py-3 text-center">อัตราสำเร็จ</th>
                <th className="px-4 py-3 text-center">เวลาซ่อมเฉลี่ย (Check-In ➔ Close)</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800/60">
              {techStats.map((tech, idx) => (
                <tr key={tech.id} className="hover:bg-slate-800/40 transition-colors">
                  <td className="px-4 py-3.5">
                    <div className="flex items-center space-x-2.5">
                      <div className="w-8 h-8 rounded-full bg-slate-800 border border-slate-700 flex items-center justify-center font-bold text-orange-400 shrink-0">
                        {tech.name.slice(0, 1)}
                      </div>
                      <div>
                        <div className="font-semibold text-slate-100 flex items-center gap-1.5">
                          {tech.name}
                          <span className={`w-2 h-2 rounded-full ${tech.isActive ? "bg-emerald-500" : "bg-slate-500"}`} title={tech.isActive ? "Active" : "Inactive"}></span>
                        </div>
                        <div className="text-[11px] text-slate-400 font-mono">{tech.id} • {tech.phone || "-"}</div>
                      </div>
                    </div>
                  </td>
                  <td className="px-4 py-3.5 text-center font-mono font-medium text-slate-200">
                    {tech.totalAssigned}
                  </td>
                  <td className="px-4 py-3.5 text-center font-mono font-bold text-emerald-400">
                    {tech.closedCount}
                  </td>
                  <td className="px-4 py-3.5 text-center font-mono text-amber-400">
                    {tech.openCount}
                  </td>
                  <td className="px-4 py-3.5 text-center">
                    <div className="flex items-center justify-center space-x-2">
                      <div className="w-16 bg-slate-800 rounded-full h-1.5 overflow-hidden">
                        <div 
                          className={`h-full rounded-full ${tech.completionRate >= 80 ? "bg-emerald-500" : tech.completionRate >= 50 ? "bg-amber-500" : "bg-orange-500"}`}
                          style={{ width: `${tech.completionRate}%` }}
                        ></div>
                      </div>
                      <span className="font-mono text-xs font-bold text-slate-200">{tech.completionRate}%</span>
                    </div>
                  </td>
                  <td className="px-4 py-3.5 text-center font-mono text-slate-300">
                    <span className="px-2.5 py-1 rounded-lg bg-slate-800/80 border border-slate-700/60 text-[11px]">
                      {tech.avgDurationText}
                    </span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

    </div>
  );
};
