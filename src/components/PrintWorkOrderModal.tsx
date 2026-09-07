import React, { useState, useEffect } from "react";
import { X, Printer, Wrench, Calendar, Phone, MapPin, CheckCircle, Clock } from "lucide-react";
import { RepairCase } from "../types";
import { generateCaseCheckInQrCode } from "../utils/qrCode";

interface PrintWorkOrderModalProps {
  isOpen: boolean;
  repairCase: RepairCase | null;
  onClose: () => void;
}

interface PrintWorkOrderContentProps {
  repairCase: RepairCase;
  onClose: () => void;
}

const PrintWorkOrderContent: React.FC<PrintWorkOrderContentProps> = ({
  repairCase,
  onClose
}) => {
  const [qrCodeUrl, setQrCodeUrl] = useState<string | null>(null);

  useEffect(() => {
    let isMounted = true;
    if (repairCase?.caseId) {
      generateCaseCheckInQrCode(repairCase.caseId, { width: 180 })
        .then((url) => {
          if (isMounted) setQrCodeUrl(url);
        })
        .catch(console.error);
    }
    return () => {
      isMounted = false;
    };
  }, [repairCase?.caseId]);

  const handlePrint = () => {
    window.print();
  };

  const formatDate = (ts: any) => {
    if (!ts) return "-";
    if (ts.toDate) {
      return ts.toDate().toLocaleString("th-TH");
    }
    if (typeof ts === "number") {
      return new Date(ts).toLocaleString("th-TH");
    }
    return "-";
  };

  return (
    <div 
      id="print-work-order-backdrop"
      className="fixed inset-0 z-50 bg-slate-950/80 backdrop-blur-sm flex items-center justify-center p-4 overflow-y-auto"
      onClick={onClose}
    >
      <div 
        id="print-work-order-container"
        className="relative w-full max-w-3xl bg-white text-slate-900 rounded-2xl shadow-2xl overflow-hidden my-8"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Action Bar (Hidden when printing) */}
        <div className="flex items-center justify-between px-6 py-4 bg-slate-900 text-white print:hidden">
          <div className="flex items-center space-x-2">
            <Printer className="w-5 h-5 text-indigo-400" />
            <h3 className="font-semibold text-base">ใบสั่งซ่อมและรายงานส่งมอบงาน (Work Order)</h3>
          </div>
          <div className="flex items-center space-x-3">
            <button
              onClick={handlePrint}
              className="flex items-center space-x-2 px-4 py-2 bg-indigo-600 hover:bg-indigo-500 text-white rounded-lg text-sm font-medium transition-colors shadow-sm"
            >
              <Printer className="w-4 h-4" />
              <span>สั่งพิมพ์เอกสาร</span>
            </button>
            <button
              onClick={onClose}
              className="p-2 text-slate-400 hover:text-white rounded-lg hover:bg-slate-800 transition-colors"
            >
              <X className="w-5 h-5" />
            </button>
          </div>
        </div>

        {/* Printable Paper Content */}
        <div className="p-8 sm:p-12 space-y-6 text-sm bg-white font-sans text-slate-800 print:p-0">
          {/* Header */}
          <div className="flex items-start justify-between border-b-2 border-slate-900 pb-6">
            <div>
              <div className="flex items-center space-x-3">
                <div className="w-9 h-9 rounded-lg bg-[#0B0F19] flex items-center justify-center text-white">
                  <Wrench className="w-5 h-5 text-orange-500" />
                </div>
                <div>
                  <h1 className="text-xl font-bold text-slate-900 tracking-tight">WORKLOG · OPERATIONS</h1>
                  <p className="text-[11px] text-slate-500">ระบบบริหารจัดการและติดตามงานซ่อมบำรุง</p>
                </div>
              </div>
            </div>
            <div className="text-right flex items-center space-x-3">
              {qrCodeUrl && (
                <div className="flex flex-col items-center">
                  <img src={qrCodeUrl} alt="QR Code" className="w-16 h-16 border border-slate-300 rounded p-0.5" />
                  <span className="text-[9px] font-mono text-slate-500 mt-0.5">Scan Check-in</span>
                </div>
              )}
              <div className="text-right">
                <div className="inline-block px-3 py-1 bg-slate-100 border border-slate-300 rounded font-mono font-bold text-slate-900 text-sm">
                  {repairCase.caseId}
                </div>
                <div className="text-xs text-slate-500 mt-1">
                  สถานะ: <span className="font-semibold text-slate-800">{repairCase.status === "CLOSED" ? "ปิดงานแล้ว (CLOSED)" : "กำลังดำเนินการ (OPEN)"}</span>
                </div>
                <div className="text-xs text-slate-500">
                  ความสำคัญ: <span className="font-semibold">{repairCase.priority}</span>
                </div>
              </div>
            </div>
          </div>

          {/* Section 1: Customer & Job Info */}
          <div className="grid grid-cols-2 gap-6 bg-slate-50 p-4 rounded-xl border border-slate-200">
            <div className="space-y-2">
              <h4 className="text-xs font-bold uppercase tracking-wider text-slate-500">ข้อมูลผู้รับบริการ / ลูกค้า</h4>
              <p className="text-base font-bold text-slate-900">{repairCase.customerName}</p>
              <p className="flex items-center text-slate-600 text-xs">
                <Phone className="w-3.5 h-3.5 mr-1 text-slate-400" />
                {repairCase.phone}
              </p>
              <p className="flex items-start text-slate-600 text-xs">
                <MapPin className="w-3.5 h-3.5 mr-1 text-slate-400 shrink-0 mt-0.5" />
                <span>{repairCase.address}</span>
              </p>
            </div>

            <div className="space-y-2 border-l border-slate-200 pl-4">
              <h4 className="text-xs font-bold uppercase tracking-wider text-slate-500">ข้อมูลการจ่ายงาน & ช่าง</h4>
              <p className="text-xs text-slate-600">
                <span className="text-slate-400">หมวดหมู่งาน:</span> <span className="font-semibold text-slate-800">{repairCase.category}</span>
              </p>
              <p className="text-xs text-slate-600">
                <span className="text-slate-400">ช่างผู้รับผิดชอบ:</span> <span className="font-semibold text-slate-900">{repairCase.technicianName}</span>
              </p>
              <p className="text-xs text-slate-600">
                <span className="text-slate-400">วันที่เปิดงาน:</span> {formatDate(repairCase.createdAt || repairCase.timestamp)}
              </p>
              {repairCase.closedAt && (
                <p className="text-xs text-slate-600">
                  <span className="text-slate-400">วันที่ปิดงาน:</span> {formatDate(repairCase.closedAt)}
                </p>
              )}
            </div>
          </div>

          {/* Section 2: Problem Details */}
          <div className="space-y-2 border border-slate-200 rounded-xl p-4">
            <h4 className="text-xs font-bold uppercase tracking-wider text-slate-500">รายละเอียดปัญหา / อาการเสียที่แจ้ง</h4>
            <p className="text-slate-800 leading-relaxed bg-amber-50/50 p-3 rounded-lg border border-amber-200/50 text-xs">
              {repairCase.details || "-"}
            </p>
          </div>

          {/* Section 3: Solutions & Technical Notes */}
          <div className="space-y-2 border border-slate-200 rounded-xl p-4">
            <h4 className="text-xs font-bold uppercase tracking-wider text-slate-500">ผลการตรวจซ่อม / วิธีการแก้ไข (Technical Solution)</h4>
            <div className="min-h-[80px] bg-slate-50 p-3 rounded-lg border border-slate-200 text-xs leading-relaxed text-slate-800">
              {repairCase.solutions ? (
                <p>{repairCase.solutions}</p>
              ) : (
                <p className="text-slate-400 italic">อยู่ระหว่างดำเนินการซ่อม หรือยังไม่มีการลงบันทึกผลงาน</p>
              )}
            </div>
          </div>

          {/* Section 4: Photo Proofs */}
          <div className="space-y-2">
            <h4 className="text-xs font-bold uppercase tracking-wider text-slate-500">รูปภาพหลักฐานการทำงาน</h4>
            <div className="grid grid-cols-3 gap-3">
              <div className="border border-slate-200 rounded-lg p-2 text-center bg-slate-50">
                <p className="text-[11px] font-semibold text-slate-600 mb-1">1. รูปก่อนซ่อม</p>
                {repairCase.imageUrl ? (
                  <img src={repairCase.imageUrl} alt="Before" className="w-full h-24 object-cover rounded border border-slate-200" referrerPolicy="no-referrer" />
                ) : (
                  <div className="w-full h-24 flex items-center justify-center bg-slate-100 rounded text-slate-400 text-xs">ไม่มีรูป</div>
                )}
              </div>
              <div className="border border-slate-200 rounded-lg p-2 text-center bg-slate-50">
                <p className="text-[11px] font-semibold text-slate-600 mb-1">2. รูป Check-in เข้างาน</p>
                {repairCase.checkInImageUrl ? (
                  <img src={repairCase.checkInImageUrl} alt="CheckIn" className="w-full h-24 object-cover rounded border border-slate-200" referrerPolicy="no-referrer" />
                ) : (
                  <div className="w-full h-24 flex items-center justify-center bg-slate-100 rounded text-slate-400 text-xs">ยังไม่เช็คอิน</div>
                )}
              </div>
              <div className="border border-slate-200 rounded-lg p-2 text-center bg-slate-50">
                <p className="text-[11px] font-semibold text-slate-600 mb-1">3. รูปปิดงาน (Check-out)</p>
                {repairCase.checkOutImageUrl ? (
                  <img src={repairCase.checkOutImageUrl} alt="CheckOut" className="w-full h-24 object-cover rounded border border-slate-200" referrerPolicy="no-referrer" />
                ) : (
                  <div className="w-full h-24 flex items-center justify-center bg-slate-100 rounded text-slate-400 text-xs">ยังไม่ปิดงาน</div>
                )}
              </div>
            </div>
          </div>

          {/* Footer note */}
          <div className="text-center text-[10px] text-slate-400 pt-4">
            เอกสารนี้ออกโดยระบบบริหารจัดการงานซ่อม PRO-DISPATCH Dashboard • พิมพ์เมื่อ {new Date().toLocaleString("th-TH")}
          </div>
        </div>
      </div>
    </div>
  );
};

export const PrintWorkOrderModal: React.FC<PrintWorkOrderModalProps> = ({
  isOpen,
  repairCase,
  onClose
}) => {
  if (!isOpen || !repairCase) return null;
  return <PrintWorkOrderContent repairCase={repairCase} onClose={onClose} />;
};
