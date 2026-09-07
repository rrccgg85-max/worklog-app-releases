import React, { useState, useMemo } from "react";
import { 
  Search, 
  Filter, 
  Download, 
  PlusCircle, 
  Wrench, 
  Phone, 
  MapPin, 
  Clock, 
  CheckCircle2, 
  AlertTriangle, 
  Eye, 
  User, 
  Trash2, 
  LayoutGrid, 
  List, 
  RefreshCw, 
  ExternalLink,
  Printer,
  Smartphone,
  Globe
} from "lucide-react";
import { RepairCase, TechUser, CasePriority, CaseStatus, REPAIR_CATEGORIES } from "../types";

interface CaseMonitoringProps {
  cases: RepairCase[];
  technicians: TechUser[];
  onSelectCase: (repairCase: RepairCase) => void;
  onOpenCreateModal: () => void;
  onPrintRequest: (repairCase: RepairCase) => void;
}

export const CaseMonitoring: React.FC<CaseMonitoringProps> = ({
  cases,
  technicians,
  onSelectCase,
  onOpenCreateModal,
  onPrintRequest
}) => {
  const [searchQuery, setSearchQuery] = useState("");
  const [statusFilter, setStatusFilter] = useState<"ALL" | CaseStatus>("ALL");
  const [priorityFilter, setPriorityFilter] = useState<"ALL" | CasePriority>("ALL");
  const [categoryFilter, setCategoryFilter] = useState<string>("ALL");
  const [technicianFilter, setTechnicianFilter] = useState<string>("ALL");
  const [sourceFilter, setSourceFilter] = useState<"ALL" | "WEB" | "APP">("ALL");
  const [viewMode, setViewMode] = useState<"TABLE" | "CARDS">("TABLE");

  // Instant Realtime Filtering Logic
  const filteredCases = useMemo(() => {
    return cases.filter((c) => {
      // 1. Instant Search matching Case ID, Customer Name, Phone, Tech Name, or Symptoms
      const q = searchQuery.toLowerCase().trim();
      const matchesSearch = !q || (
        c.caseId.toLowerCase().includes(q) ||
        c.customerName.toLowerCase().includes(q) ||
        c.phone.replace(/[-\s]/g, "").includes(q.replace(/[-\s]/g, "")) ||
        c.technicianName.toLowerCase().includes(q) ||
        c.details.toLowerCase().includes(q) ||
        c.address.toLowerCase().includes(q)
      );

      // 2. Status Match
      const matchesStatus = statusFilter === "ALL" || c.status === statusFilter;

      // 3. Priority Match
      const matchesPriority = priorityFilter === "ALL" || c.priority === priorityFilter;

      // 4. Category Match
      const matchesCategory = categoryFilter === "ALL" || c.category === categoryFilter;

      // 5. Technician Match
      const matchesTech = technicianFilter === "ALL" || c.technicianId === technicianFilter;

      // 6. Source/Channel Match (WEB / APP)
      const matchesSource = sourceFilter === "ALL" || (c.createdSource || "WEB") === sourceFilter;

      return matchesSearch && matchesStatus && matchesPriority && matchesCategory && matchesTech && matchesSource;
    });
  }, [cases, searchQuery, statusFilter, priorityFilter, categoryFilter, technicianFilter, sourceFilter]);

  // Export to CSV Function
  const exportToCSV = () => {
    if (filteredCases.length === 0) {
      alert("ไม่มีข้อมูลเคสสำหรับส่งออก CSV");
      return;
    }

    const headers = ["Case ID", "ลูกค้า", "เบอร์โทรศัพท์", "หมวดหมู่งาน", "ระดับความสำคัญ", "ช่างผู้รับผิดชอบ", "สถานะ", "ที่อยู่", "อาการเสีย", "ผลการซ่อม"];
    const rows = filteredCases.map(c => [
      `"${c.caseId}"`,
      `"${c.customerName}"`,
      `"${c.phone}"`,
      `"${c.category}"`,
      `"${c.priority}"`,
      `"${c.technicianName}"`,
      `"${c.status}"`,
      `"${c.address.replace(/"/g, '""')}"`,
      `"${c.details.replace(/"/g, '""')}"`,
      `"${(c.solutions || "").replace(/"/g, '""')}"`
    ]);

    const csvContent = "\uFEFF" + [headers.join(","), ...rows.map(r => r.join(","))].join("\n");
    const blob = new Blob([csvContent], { type: "text/csv;charset=utf-8;" });
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.setAttribute("href", url);
    link.setAttribute("download", `repair_cases_${new Date().toISOString().slice(0, 10)}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  const formatDate = (ts: any) => {
    if (!ts) return "-";
    if (ts.toDate) return ts.toDate().toLocaleString("th-TH");
    if (typeof ts === "number") return new Date(ts).toLocaleString("th-TH");
    return "-";
  };

  return (
    <div id="case-monitoring-view" className="space-y-4">
      {/* Header & Controls Bar */}
      <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-4">
        <div>
          <h2 className="text-lg font-bold text-slate-100 flex items-center">
            <Wrench className="w-5 h-5 mr-2 text-indigo-400" />
            ติดตามและจ่ายงานเคสซ่อมแบบ Real-time (Live Dispatch & Cases)
          </h2>
          <p className="text-xs text-slate-400 mt-0.5">
            ค้นหาแบบ Instant Filter ทันที พร้อมแสดงผลการซิงค์กับแอปช่างแบบไม่ต้องรีเฟรช
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-2.5">
          {/* Table / Card View Mode */}
          <div className="bg-slate-900 border border-slate-800 rounded-xl p-1 flex items-center">
            <button
              onClick={() => setViewMode("TABLE")}
              className={`p-1.5 rounded-lg transition-colors ${
                viewMode === "TABLE" ? "bg-indigo-600 text-white" : "text-slate-400 hover:text-slate-200"
              }`}
              title="มุมมองตาราง (Table)"
            >
              <List className="w-4 h-4" />
            </button>
            <button
              onClick={() => setViewMode("CARDS")}
              className={`p-1.5 rounded-lg transition-colors ${
                viewMode === "CARDS" ? "bg-indigo-600 text-white" : "text-slate-400 hover:text-slate-200"
              }`}
              title="มุมมองการ์ด (Cards)"
            >
              <LayoutGrid className="w-4 h-4" />
            </button>
          </div>

          <button
            onClick={exportToCSV}
            className="px-3.5 py-2 bg-slate-900 hover:bg-slate-800 text-slate-300 border border-slate-800 rounded-xl text-xs font-semibold flex items-center space-x-1.5 transition-colors"
            title="ส่งออกไฟล์ CSV"
          >
            <Download className="w-4 h-4 text-emerald-400" />
            <span>Export CSV</span>
          </button>

          <button
            onClick={onOpenCreateModal}
            className="px-4 py-2 bg-indigo-600 hover:bg-indigo-500 text-white rounded-xl text-xs font-bold flex items-center space-x-1.5 transition-all shadow-md shadow-indigo-600/30"
          >
            <PlusCircle className="w-4 h-4" />
            <span>เปิดเคสใหม่</span>
          </button>
        </div>
      </div>

      {/* Instant Search & Multi-Filter Toolbar */}
      <div className="bg-slate-900/90 border border-slate-800 rounded-2xl p-4 shadow-xl space-y-3">
        {/* Instant Search Bar */}
        <div className="relative">
          <Search className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
          <input
            type="text"
            placeholder="ค้นหาทันทีด้วย: รหัสเคส (CASE-...), ชื่อลูกค้า, เบอร์โทร, ชื่อช่าง หรืออาการเสีย..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full pl-10 pr-10 py-2.5 bg-slate-950/70 border border-slate-700/80 rounded-xl text-xs text-slate-100 placeholder-slate-500 focus:outline-none focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500 shadow-inner"
          />
          {searchQuery && (
            <button
              onClick={() => setSearchQuery("")}
              className="absolute right-3.5 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-200 text-xs font-semibold"
            >
              ล้าง
            </button>
          )}
        </div>

        {/* Filter Dropdowns & Pills */}
        <div className="grid grid-cols-2 sm:grid-cols-5 gap-2.5 pt-1">
          {/* Channel / Source Filter */}
          <div>
            <label className="text-[10px] uppercase font-bold text-slate-400 block mb-1">ช่องทางเปิดเคส:</label>
            <select
              value={sourceFilter}
              onChange={(e) => setSourceFilter(e.target.value as any)}
              className="w-full px-2.5 py-1.5 bg-slate-950 border border-slate-800 rounded-xl text-xs text-slate-200 focus:outline-none focus:border-indigo-500"
            >
              <option value="ALL">ทุกช่องทาง</option>
              <option value="WEB">🌐 เว็บ Admin</option>
              <option value="APP">📱 แอปช่าง</option>
            </select>
          </div>

          {/* Status Filter */}
          <div>
            <label className="text-[10px] uppercase font-bold text-slate-400 block mb-1">สถานะงาน:</label>
            <select
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value as any)}
              className="w-full px-2.5 py-1.5 bg-slate-950 border border-slate-800 rounded-xl text-xs text-slate-200 focus:outline-none focus:border-indigo-500"
            >
              <option value="ALL">สถานะทั้งหมด</option>
              <option value="OPEN">กำลังดำเนินการ (OPEN)</option>
              <option value="CLOSED">ปิดงานแล้ว (CLOSED)</option>
            </select>
          </div>

          {/* Priority Filter */}
          <div>
            <label className="text-[10px] uppercase font-bold text-slate-400 block mb-1">ความสำคัญ:</label>
            <select
              value={priorityFilter}
              onChange={(e) => setPriorityFilter(e.target.value as any)}
              className="w-full px-2.5 py-1.5 bg-slate-950 border border-slate-800 rounded-xl text-xs text-slate-200 focus:outline-none focus:border-indigo-500"
            >
              <option value="ALL">ความสำคัญทั้งหมด</option>
              <option value="ด่วนที่สุด">ด่วนที่สุด (Urgent High)</option>
              <option value="ด่วน">ด่วน (Medium)</option>
              <option value="ปกติ">ปกติ (Normal)</option>
            </select>
          </div>

          {/* Category Filter */}
          <div>
            <label className="text-[10px] uppercase font-bold text-slate-400 block mb-1">หมวดหมู่งาน:</label>
            <select
              value={categoryFilter}
              onChange={(e) => setCategoryFilter(e.target.value)}
              className="w-full px-2.5 py-1.5 bg-slate-950 border border-slate-800 rounded-xl text-xs text-slate-200 focus:outline-none focus:border-indigo-500 truncate"
            >
              <option value="ALL">หมวดหมู่ทั้งหมด</option>
              {REPAIR_CATEGORIES.map(cat => (
                <option key={cat} value={cat}>{cat}</option>
              ))}
            </select>
          </div>

          {/* Technician Filter */}
          <div>
            <label className="text-[10px] uppercase font-bold text-slate-400 block mb-1">ช่างผู้รับผิดชอบ:</label>
            <select
              value={technicianFilter}
              onChange={(e) => setTechnicianFilter(e.target.value)}
              className="w-full px-2.5 py-1.5 bg-slate-950 border border-slate-800 rounded-xl text-xs text-slate-200 focus:outline-none focus:border-indigo-500 truncate"
            >
              <option value="ALL">ช่างทั้งหมด</option>
              {technicians.map(t => (
                <option key={t.id} value={t.id}>{t.name}</option>
              ))}
            </select>
          </div>
        </div>

        {/* Results summary bar */}
        <div className="flex items-center justify-between text-xs text-slate-400 pt-1 border-t border-slate-800/80">
          <span>
            แสดง <strong className="text-slate-200">{filteredCases.length}</strong> จากทั้งหมด {cases.length} เคส
          </span>
          {(searchQuery || statusFilter !== "ALL" || priorityFilter !== "ALL" || categoryFilter !== "ALL" || technicianFilter !== "ALL" || sourceFilter !== "ALL") && (
            <button
              onClick={() => {
                setSearchQuery("");
                setStatusFilter("ALL");
                setPriorityFilter("ALL");
                setCategoryFilter("ALL");
                setTechnicianFilter("ALL");
                setSourceFilter("ALL");
              }}
              className="text-indigo-400 hover:underline text-[11px]"
            >
              รีเซ็ตตัวกรองทั้งหมด
            </button>
          )}
        </div>
      </div>

      {/* Main Content: TABLE VIEW */}
      {viewMode === "TABLE" ? (
        <div className="bg-slate-900/90 border border-slate-800 rounded-2xl overflow-hidden shadow-xl">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs text-slate-300">
              <thead className="bg-slate-950/80 text-[11px] text-slate-400 uppercase tracking-wider border-b border-slate-800">
                <tr>
                  <th className="py-3.5 px-4 font-semibold">รหัสเคส</th>
                  <th className="py-3.5 px-4 font-semibold">ความสำคัญ</th>
                  <th className="py-3.5 px-4 font-semibold">ลูกค้า & สถานที่</th>
                  <th className="py-3.5 px-4 font-semibold">หมวดหมู่ & ปัญหา</th>
                  <th className="py-3.5 px-4 font-semibold">ช่างผู้รับผิดชอบ</th>
                  <th className="py-3.5 px-4 font-semibold">สถานะ</th>
                  <th className="py-3.5 px-4 font-semibold text-center">รูปหลักฐาน</th>
                  <th className="py-3.5 px-4 font-semibold text-right">จัดการ</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/60 font-sans">
                {filteredCases.length === 0 ? (
                  <tr>
                    <td colSpan={8} className="py-16 text-center text-slate-500">
                      <Wrench className="w-8 h-8 mx-auto text-slate-600 mb-2 opacity-50" />
                      <p className="text-sm font-semibold text-slate-400">ไม่พบรายการเคสซ่อมตามเงื่อนไข</p>
                      <p className="text-xs text-slate-500 mt-1">ลองเปลี่ยนคำค้นหา หรือเปิดเคสงานซ่อมใหม่</p>
                    </td>
                  </tr>
                ) : (
                  filteredCases.map((c) => (
                    <tr 
                      key={c.caseId} 
                      onClick={() => onSelectCase(c)}
                      className="hover:bg-slate-800/50 cursor-pointer transition-colors group"
                    >
                      {/* Case ID */}
                      <td className="py-3.5 px-4 whitespace-nowrap">
                        <div className="flex items-center space-x-1.5">
                          <span className="font-mono text-xs font-bold text-indigo-400 group-hover:text-indigo-300">
                            {c.caseId}
                          </span>
                          {c.createdSource === "APP" ? (
                            <span className="inline-flex items-center px-1.5 py-0.5 rounded text-[10px] font-bold bg-cyan-950/90 text-cyan-300 border border-cyan-800" title="เคสเปิดจากแอปช่าง (Mobile App)">
                              <Smartphone className="w-3 h-3 mr-0.5 text-cyan-400" />
                              แอปช่าง
                            </span>
                          ) : (
                            <span className="inline-flex items-center px-1.5 py-0.5 rounded text-[10px] font-bold bg-slate-800 text-slate-300 border border-slate-700" title="เคสเปิดจากเว็บ Admin (Web Admin)">
                              <Globe className="w-3 h-3 mr-0.5 text-slate-400" />
                              เว็บ
                            </span>
                          )}
                        </div>
                        <span className="text-[10px] text-slate-500 block mt-0.5">{formatDate(c.createdAt || c.timestamp).split(" ")[0]}</span>
                      </td>

                      {/* Priority Badge */}
                      <td className="py-3.5 px-4 whitespace-nowrap">
                        {c.priority === "ด่วนที่สุด" ? (
                          <span className="inline-flex items-center px-2 py-0.5 rounded-full text-[10px] font-extrabold bg-rose-950 text-rose-300 border border-rose-600 animate-pulse">
                            <span className="w-1.5 h-1.5 rounded-full bg-rose-400 mr-1 animate-ping"></span>
                            ด่วนที่สุด
                          </span>
                        ) : c.priority === "ด่วน" ? (
                          <span className="inline-flex items-center px-2 py-0.5 rounded-full text-[10px] font-bold bg-amber-950 text-amber-300 border border-amber-600">
                            ด่วน
                          </span>
                        ) : (
                          <span className="inline-flex items-center px-2 py-0.5 rounded-full text-[10px] font-medium bg-slate-800 text-slate-300 border border-slate-700">
                            ปกติ
                          </span>
                        )}
                      </td>

                      {/* Customer & Location */}
                      <td className="py-3.5 px-4">
                        <div className="font-bold text-slate-100">{c.customerName}</div>
                        <div className="flex items-center text-slate-400 text-[11px] mt-0.5">
                          <Phone className="w-3 h-3 mr-1 text-slate-500" />
                          <span>{c.phone}</span>
                        </div>
                      </td>

                      {/* Category & Problem */}
                      <td className="py-3.5 px-4 max-w-xs">
                        <span className="px-1.5 py-0.5 rounded bg-slate-800 text-indigo-300 border border-slate-700 text-[10px] font-semibold">
                          {c.category}
                        </span>
                        <p className="text-xs text-slate-300 truncate mt-1">{c.details}</p>
                      </td>

                      {/* Technician */}
                      <td className="py-3.5 px-4 whitespace-nowrap">
                        <div className="flex items-center space-x-1.5">
                          <div className="w-6 h-6 rounded-full bg-indigo-600/20 text-indigo-400 flex items-center justify-center font-bold text-[10px]">
                            {c.technicianName.substring(0, 1)}
                          </div>
                          <div>
                            <span className="font-semibold text-slate-200 text-xs block">{c.technicianName}</span>
                            <span className="text-[10px] text-slate-500 font-mono">{c.technicianId}</span>
                          </div>
                        </div>
                      </td>

                      {/* Status */}
                      <td className="py-3.5 px-4 whitespace-nowrap">
                        <span className={`inline-flex items-center px-2.5 py-1 rounded-full text-[10px] font-bold ${
                          c.status === "CLOSED"
                            ? "bg-emerald-500/20 text-emerald-300 border border-emerald-500/40"
                            : "bg-amber-500/20 text-amber-300 border border-amber-500/40"
                        }`}>
                          {c.status === "CLOSED" ? (
                            <>
                              <CheckCircle2 className="w-3 h-3 mr-1 text-emerald-400" />
                              CLOSED (เสร็จสิ้น)
                            </>
                          ) : (
                            <>
                              <Clock className="w-3 h-3 mr-1 text-amber-400" />
                              OPEN (กำลังดำเนินการ)
                            </>
                          )}
                        </span>
                      </td>

                      {/* Photos Indicator */}
                      <td className="py-3.5 px-4 text-center whitespace-nowrap">
                        <div className="flex items-center justify-center space-x-1">
                          <span className={`w-2 h-2 rounded-full ${c.imageUrl ? "bg-indigo-400" : "bg-slate-700"}`} title="รูปแจ้งซ่อม"></span>
                          <span className={`w-2 h-2 rounded-full ${c.checkInImageUrl ? "bg-amber-400" : "bg-slate-700"}`} title="รูป Check-in"></span>
                          <span className={`w-2 h-2 rounded-full ${c.checkOutImageUrl ? "bg-emerald-400" : "bg-slate-700"}`} title="รูป Check-out"></span>
                        </div>
                      </td>

                      {/* Action Buttons */}
                      <td className="py-3.5 px-4 text-right whitespace-nowrap">
                        <div className="flex items-center justify-end space-x-1" onClick={(e) => e.stopPropagation()}>
                          <button
                            onClick={() => onPrintRequest(c)}
                            title="พิมพ์ใบสั่งซ่อม"
                            className="p-1.5 text-slate-400 hover:text-indigo-300 hover:bg-slate-800 rounded-lg transition-colors"
                          >
                            <Printer className="w-4 h-4" />
                          </button>
                          <button
                            onClick={() => onSelectCase(c)}
                            title="ดูรายละเอียดเคส"
                            className="p-1.5 text-slate-400 hover:text-slate-100 hover:bg-slate-800 rounded-lg transition-colors"
                          >
                            <Eye className="w-4 h-4" />
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>
      ) : (
        /* CARDS / KANBAN-STYLE GRID VIEW */
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {filteredCases.map((c) => (
            <div
              key={c.caseId}
              onClick={() => onSelectCase(c)}
              className="bg-slate-900/90 hover:bg-slate-900 border border-slate-800 hover:border-indigo-500/50 rounded-2xl p-4 shadow-lg cursor-pointer transition-all space-y-3 group"
            >
              <div className="flex items-start justify-between">
                <div>
                  <div className="flex items-center space-x-1.5">
                    <span className="font-mono text-xs font-bold text-indigo-400 group-hover:text-indigo-300">
                      {c.caseId}
                    </span>
                    {c.createdSource === "APP" ? (
                      <span className="inline-flex items-center px-1.5 py-0.5 rounded text-[10px] font-bold bg-cyan-950/90 text-cyan-300 border border-cyan-800" title="แอปช่าง (Mobile App)">
                        <Smartphone className="w-2.5 h-2.5 mr-0.5 text-cyan-400" />
                        แอปช่าง
                      </span>
                    ) : (
                      <span className="inline-flex items-center px-1.5 py-0.5 rounded text-[10px] font-bold bg-slate-800 text-slate-300 border border-slate-700" title="เว็บ Admin">
                        <Globe className="w-2.5 h-2.5 mr-0.5 text-slate-400" />
                        เว็บ
                      </span>
                    )}
                  </div>
                  <h4 className="text-sm font-bold text-slate-100 mt-0.5">{c.customerName}</h4>
                </div>

                <span className={`px-2 py-0.5 rounded-full text-[10px] font-bold ${
                  c.status === "CLOSED" ? "bg-emerald-500/20 text-emerald-300 border border-emerald-500/30" : "bg-amber-500/20 text-amber-300 border border-amber-500/30"
                }`}>
                  {c.status}
                </span>
              </div>

              <div className="text-xs text-slate-300 space-y-1">
                <p className="flex items-center text-slate-400">
                  <Phone className="w-3.5 h-3.5 mr-1 text-slate-500" />
                  {c.phone}
                </p>
                <p className="flex items-start text-slate-400">
                  <MapPin className="w-3.5 h-3.5 mr-1 text-slate-500 shrink-0 mt-0.5" />
                  <span className="line-clamp-1">{c.address}</span>
                </p>
              </div>

              <div className="bg-slate-950/60 p-2.5 rounded-xl border border-slate-800/80 text-xs text-slate-300">
                <span className="text-[10px] text-indigo-300 font-semibold block mb-0.5">{c.category}</span>
                <p className="line-clamp-2">{c.details}</p>
              </div>

              <div className="pt-2 border-t border-slate-800 flex items-center justify-between text-xs">
                <div className="flex items-center space-x-1.5">
                  <User className="w-3.5 h-3.5 text-indigo-400" />
                  <span className="text-slate-300 font-medium text-[11px]">{c.technicianName}</span>
                </div>

                <div className="flex items-center space-x-2">
                  <span className={`text-[10px] px-1.5 py-0.5 rounded font-bold ${
                    c.priority === "ด่วนที่สุด" ? "bg-rose-950 text-rose-300 border border-rose-800" :
                    c.priority === "ด่วน" ? "bg-amber-950 text-amber-300 border border-amber-800" : "bg-slate-800 text-slate-400"
                  }`}>
                    {c.priority}
                  </span>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};
