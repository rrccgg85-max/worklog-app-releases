import React, { useState } from "react";
import { 
  FileCode2, 
  Search, 
  Filter, 
  Clock, 
  User, 
  ArrowUpRight, 
  CheckCircle, 
  PlusCircle, 
  Trash2, 
  RefreshCw, 
  AlertCircle,
  X,
  MapPin,
  Wrench,
  Info
} from "lucide-react";
import { SystemLog, RepairCase } from "../types";

interface SystemLogsViewProps {
  logs: SystemLog[];
  cases?: RepairCase[];
  onSelectCaseId?: (caseId: string) => void;
}

export const SystemLogsView: React.FC<SystemLogsViewProps> = ({
  logs,
  cases = [],
  onSelectCaseId
}) => {
  const [searchQuery, setSearchQuery] = useState("");
  const [actionFilter, setActionFilter] = useState<string>("ALL");
  const [selectedLog, setSelectedLog] = useState<SystemLog | null>(null);

  const filteredLogs = logs.filter(l => {
    const matchesSearch = 
      (l.caseId && l.caseId.toLowerCase().includes(searchQuery.toLowerCase())) ||
      (l.actor && l.actor.toLowerCase().includes(searchQuery.toLowerCase())) ||
      (l.details && l.details.toLowerCase().includes(searchQuery.toLowerCase()));

    const matchesAction = actionFilter === "ALL" || l.action === actionFilter;

    return matchesSearch && matchesAction;
  });

  const getActionBadge = (action: SystemLog["action"]) => {
    switch (action) {
      case "CREATE_CASE":
        return <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-indigo-500/20 text-indigo-300 border border-indigo-500/30">เปิดเคสใหม่</span>;
      case "CLOSE_CASE":
      case "TECH_CLOSE":
        return <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-emerald-500/20 text-emerald-300 border border-emerald-500/30">ปิดเคสสำเร็จ</span>;
      case "TECH_CHECKIN":
        return <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-amber-500/20 text-amber-300 border border-amber-500/30">ช่าง Check-in</span>;
      case "REASSIGN_TECH":
        return <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-purple-500/20 text-purple-300 border border-purple-500/30">โอนย้ายช่าง</span>;
      case "DELETE_CASE":
      case "DELETE_TECH":
        return <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-rose-500/20 text-rose-300 border border-rose-500/30">ลบข้อมูล</span>;
      case "ADD_TECH":
      case "UPDATE_TECH":
        return <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-sky-500/20 text-sky-300 border border-sky-500/30">จัดการช่าง</span>;
      default:
        return <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-slate-700 text-slate-300">อัปเดต</span>;
    }
  };

  const formatDate = (ts: any) => {
    if (!ts) return "เมื่อสักครู่";
    if (ts.toDate) return ts.toDate().toLocaleString("th-TH");
    if (typeof ts === "number") return new Date(ts).toLocaleString("th-TH");
    return "-";
  };

  // Find associated case if log has caseId
  const getAssociatedCase = (caseId?: string): RepairCase | undefined => {
    if (!caseId) return undefined;
    return cases.find(c => c.caseId === caseId);
  };

  return (
    <div id="system-logs-view" className="space-y-4">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-lg font-bold text-slate-100 flex items-center">
            <FileCode2 className="w-5 h-5 mr-2 text-indigo-400" />
            บันทึกประวัติการทำงานของระบบ (System Audit & Dispatch Logs)
          </h2>
          <p className="text-xs text-slate-400 mt-0.5">
            คลิกที่รายการ Log เพื่อตรวจสอบรายละเอียด (ใครทำอะไร, อุปกรณ์, เวลา, สถานที่)
          </p>
        </div>

        {/* Filter Controls */}
        <div className="flex items-center space-x-3">
          <div className="relative">
            <Search className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
            <input
              type="text"
              placeholder="ค้นหา Case ID, ผู้ดำเนินการ, รายละเอียด..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="pl-9 pr-3 py-2 bg-slate-900 border border-slate-800 rounded-xl text-xs text-slate-200 placeholder-slate-500 focus:outline-none focus:border-indigo-500 w-48 sm:w-64"
            />
          </div>

          <select
            value={actionFilter}
            onChange={(e) => setActionFilter(e.target.value)}
            className="px-3 py-2 bg-slate-900 border border-slate-800 rounded-xl text-xs text-slate-300 focus:outline-none focus:border-indigo-500"
          >
            <option value="ALL">เหตุการณ์ทั้งหมด</option>
            <option value="CREATE_CASE">เปิดเคสใหม่ (Create)</option>
            <option value="TECH_CHECKIN">ช่าง Check-in หน้างาน</option>
            <option value="CLOSE_CASE">ปิดงาน (Close)</option>
            <option value="REASSIGN_TECH">โอนย้ายช่าง (Reassign)</option>
            <option value="DELETE_CASE">ลบเคส (Delete)</option>
            <option value="ADD_TECH">เพิ่ม/แก้ไขช่าง (Tech Management)</option>
          </select>
        </div>
      </div>

      {/* Logs Table */}
      <div className="bg-slate-900/90 border border-slate-800 rounded-2xl overflow-hidden shadow-xl">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs text-slate-300">
            <thead className="bg-slate-950/80 text-[11px] text-slate-400 uppercase tracking-wider border-b border-slate-800">
              <tr>
                <th className="py-3 px-4">วัน-เวลา</th>
                <th className="py-3 px-4">ประเภทเหตุการณ์</th>
                <th className="py-3 px-4">ผู้ดำเนินการ (Actor)</th>
                <th className="py-3 px-4">รหัสเคส</th>
                <th className="py-3 px-4">รายละเอียด</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800/60 font-sans">
              {filteredLogs.length === 0 ? (
                <tr>
                  <td colSpan={5} className="py-12 text-center text-slate-500">
                    ไม่พบรายการ Log ตามเงื่อนไขการค้นหา
                  </td>
                </tr>
              ) : (
                filteredLogs.map((log, idx) => {
                  const assocCase = getAssociatedCase(log.caseId);
                  return (
                    <tr 
                      key={log.id || idx} 
                      onClick={() => setSelectedLog(log)}
                      className="hover:bg-slate-800/50 transition-colors cursor-pointer group"
                    >
                      <td className="py-3 px-4 whitespace-nowrap text-slate-400 text-[11px]">
                        {formatDate(log.timestamp)}
                      </td>
                      <td className="py-3 px-4 whitespace-nowrap">
                        {getActionBadge(log.action)}
                      </td>
                      <td className="py-3 px-4 whitespace-nowrap font-medium text-slate-200">
                        {log.actor || "-"}
                      </td>
                      <td className="py-3 px-4 whitespace-nowrap">
                        {log.caseId ? (
                          <span className="font-mono text-indigo-400 font-semibold flex items-center space-x-1">
                            <span>{log.caseId}</span>
                            <ArrowUpRight className="w-3 h-3 group-hover:translate-x-0.5 group-hover:-translate-y-0.5 transition-transform" />
                          </span>
                        ) : (
                          <span className="text-slate-600">-</span>
                        )}
                      </td>
                      <td className="py-3 px-4 text-slate-300 truncate max-w-xs">
                        {assocCase && (
                          <span className="text-[10px] bg-slate-800 text-indigo-300 px-1.5 py-0.5 rounded mr-1.5 font-mono">
                            {assocCase.category}
                          </span>
                        )}
                        {log.details}
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Detailed Inspection Modal for Selected Log */}
      {selectedLog && (
        <div 
          className="fixed inset-0 z-50 bg-slate-950/80 backdrop-blur-sm flex items-center justify-center p-4 animate-fadeIn"
          onClick={() => setSelectedLog(null)}
        >
          <div 
            className="bg-slate-900 border border-slate-800 rounded-2xl w-full max-w-lg shadow-2xl overflow-hidden text-slate-100"
            onClick={(e) => e.stopPropagation()}
          >
            {/* Modal Header */}
            <div className="px-5 py-4 bg-slate-950/80 border-b border-slate-800 flex items-center justify-between">
              <div className="flex items-center space-x-2.5">
                <div className="p-2 bg-indigo-500/20 text-indigo-400 rounded-xl border border-indigo-500/30">
                  <FileCode2 className="w-5 h-5" />
                </div>
                <div>
                  <h3 className="text-sm font-bold">รายละเอียดบันทึกเหตุการณ์ (Log Detail)</h3>
                  <p className="text-[11px] text-slate-400 font-mono">ID: {selectedLog.id || "system-event"}</p>
                </div>
              </div>
              <button
                onClick={() => setSelectedLog(null)}
                className="p-1.5 text-slate-400 hover:text-slate-100 hover:bg-slate-800 rounded-xl transition-colors"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            {/* Modal Body */}
            <div className="p-5 space-y-4 text-xs">
              {/* 1. ใครทำอะไร (Actor & Action) */}
              <div className="bg-slate-950/60 p-3.5 rounded-xl border border-slate-800 space-y-2">
                <div className="flex items-center justify-between text-slate-400 border-b border-slate-800/80 pb-2">
                  <span className="flex items-center font-semibold text-slate-300">
                    <User className="w-4 h-4 mr-1.5 text-indigo-400" /> 1. ใครเป็นผู้ดำเนินการ (Actor):
                  </span>
                  <span className="font-bold text-slate-100 bg-slate-800 px-2.5 py-1 rounded-lg">
                    {selectedLog.actor || "ระบบอัตโนมัติ (System)"}
                  </span>
                </div>
                <div className="flex items-center justify-between pt-1">
                  <span className="text-slate-400">ประเภทการกระทำ (Action):</span>
                  <div>{getActionBadge(selectedLog.action)}</div>
                </div>
                <div className="pt-1 text-slate-300">
                  <span className="text-slate-400 block mb-1">รายละเอียดเหตุการณ์:</span>
                  <p className="p-2.5 bg-slate-900 rounded-lg border border-slate-800 font-sans text-slate-200">
                    {selectedLog.details}
                  </p>
                </div>
              </div>

              {/* 2. อุปกรณ์ชื่ออะไร / หมวดหมู่ (Equipment Name / Category) */}
              <div className="bg-slate-950/60 p-3.5 rounded-xl border border-slate-800 space-y-2">
                <span className="flex items-center font-semibold text-slate-300">
                  <Wrench className="w-4 h-4 mr-1.5 text-orange-400" /> 2. อุปกรณ์ / ประเภทงาน (Equipment / Category):
                </span>
                {selectedLog.caseId && getAssociatedCase(selectedLog.caseId) ? (
                  <div className="bg-slate-900 p-2.5 rounded-lg border border-slate-800 space-y-1.5">
                    <div className="flex justify-between">
                      <span className="text-slate-400">รหัสเคส:</span>
                      <span className="font-mono font-bold text-indigo-400">{selectedLog.caseId}</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-slate-400">หมวดหมู่/อุปกรณ์:</span>
                      <span className="font-semibold text-white bg-indigo-950/80 px-2 py-0.5 rounded">
                        {getAssociatedCase(selectedLog.caseId)?.category}
                      </span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-slate-400">ชื่อลูกค้า:</span>
                      <span className="text-slate-200">{getAssociatedCase(selectedLog.caseId)?.customerName}</span>
                    </div>
                  </div>
                ) : (
                  <p className="text-slate-500 italic p-2">ไม่มีข้อมูลอุปกรณ์หรือเคสที่เกี่ยวข้องโดยตรง (General System Log)</p>
                )}
              </div>

              {/* 3. ตอนไหน (When) */}
              <div className="bg-slate-950/60 p-3.5 rounded-xl border border-slate-800 flex items-center justify-between">
                <span className="flex items-center font-semibold text-slate-300">
                  <Clock className="w-4 h-4 mr-1.5 text-emerald-400" /> 3. เกิดขึ้นตอนไหน (Timestamp):
                </span>
                <span className="font-mono text-emerald-300 font-bold bg-emerald-950/40 px-2.5 py-1 rounded-lg border border-emerald-900/40">
                  {formatDate(selectedLog.timestamp)}
                </span>
              </div>

              {/* 4. จากไหน (From where / Location) */}
              <div className="bg-slate-950/60 p-3.5 rounded-xl border border-slate-800 space-y-1.5">
                <span className="flex items-center font-semibold text-slate-300">
                  <MapPin className="w-4 h-4 mr-1.5 text-rose-400" /> 4. มาจากไหน / สถานที่ตั้ง (From Where / Location):
                </span>
                <p className="text-slate-300 bg-slate-900 p-2.5 rounded-lg border border-slate-800">
                  {selectedLog.caseId && getAssociatedCase(selectedLog.caseId)?.address ? (
                    getAssociatedCase(selectedLog.caseId)?.address
                  ) : (
                    <span className="text-slate-500 italic">ดำเนินการจากระบบส่วนกลาง / แอดมินคอนโซล</span>
                  )}
                </p>
              </div>
            </div>

            {/* Modal Footer */}
            <div className="px-5 py-3 bg-slate-950/80 border-t border-slate-800 flex items-center justify-between">
              {selectedLog.caseId && onSelectCaseId ? (
                <button
                  onClick={() => {
                    const cid = selectedLog.caseId;
                    setSelectedLog(null);
                    if (cid) onSelectCaseId(cid);
                  }}
                  className="px-3 py-1.5 bg-indigo-600 hover:bg-indigo-500 text-white rounded-xl font-semibold flex items-center space-x-1 transition-colors"
                >
                  <span>เปิดดูเคสนี้แบบเต็ม</span>
                  <ArrowUpRight className="w-3.5 h-3.5" />
                </button>
              ) : (
                <span className="text-[11px] text-slate-500">เหตุการณ์ระบบทั่วไป</span>
              )}
              <button
                onClick={() => setSelectedLog(null)}
                className="px-4 py-1.5 bg-slate-800 hover:bg-slate-700 text-slate-300 rounded-xl transition-colors"
              >
                ปิดหน้าต่าง
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

