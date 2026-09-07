import React, { useState, useEffect } from "react";
import { 
  X, 
  Phone, 
  MapPin, 
  Calendar, 
  User, 
  Wrench, 
  AlertTriangle, 
  Clock, 
  CheckCircle2, 
  Camera, 
  Edit3, 
  Trash2, 
  Printer, 
  RefreshCw, 
  ExternalLink, 
  Copy, 
  Check, 
  ArrowRight,
  ShieldAlert,
  Send,
  Smartphone,
  Globe,
  Download,
  Paperclip,
  Image,
  Eye,
  QrCode
} from "lucide-react";
import { RepairCase, TechUser, CasePriority, REPAIR_CATEGORIES } from "../types";
import { 
  updateCaseData, 
  reassignTechnician, 
  closeRepairCase, 
  reopenCase, 
  deleteRepairCase, 
  recordCheckIn 
} from "../services/caseService";
import { ImageLightbox } from "./ImageLightbox";
import { 
  generateCaseCheckInQrCode, 
  getMobileCheckInUrl, 
  downloadQrCodeImage 
} from "../utils/qrCode";

interface CaseDetailModalProps {
  isOpen: boolean;
  repairCase: RepairCase | null;
  technicians: TechUser[];
  onClose: () => void;
  onPrintRequest?: (repairCase: RepairCase) => void;
}

interface CaseDetailModalContentProps {
  repairCase: RepairCase;
  technicians: TechUser[];
  onClose: () => void;
  onPrintRequest?: (repairCase: RepairCase) => void;
}

const CaseDetailModalContent: React.FC<CaseDetailModalContentProps> = ({
  repairCase,
  technicians,
  onClose,
  onPrintRequest
}) => {
  // Debug: Inspect the image URLs passed to the component
  console.log(`[DEBUG] CaseDetailModal render - Case ID: ${repairCase.caseId}`);
  console.log(`[DEBUG] Check-In Image URL:`, repairCase.checkInImageUrl);
  console.log(`[DEBUG] Check-Out Image URL:`, repairCase.checkOutImageUrl);
  console.log(`[DEBUG] Initial Image URL:`, repairCase.imageUrl);
  
  // Edit Mode state
  const [isEditing, setIsEditing] = useState(false);
  const [editCustomerName, setEditCustomerName] = useState(repairCase.customerName);
  const [editPhone, setEditPhone] = useState(repairCase.phone);
  const [editAddress, setEditAddress] = useState(repairCase.address);
  const [editDetails, setEditDetails] = useState(repairCase.details);
  const [editCategory, setEditCategory] = useState(repairCase.category);
  const [editPriority, setEditPriority] = useState<CasePriority>(repairCase.priority);

  // Reassign state
  const [reassignTechId, setReassignTechId] = useState(repairCase.technicianId);
  const [isReassigning, setIsReassigning] = useState(false);

  // Close Case Form state
  const [showCloseForm, setShowCloseForm] = useState(false);
  const [solutionNotes, setSolutionNotes] = useState(repairCase.solutions || "");
  const [checkOutPhoto, setCheckOutPhoto] = useState(repairCase.checkOutImageUrl || "");

  // Lightbox state
  const [lightboxImage, setLightboxImage] = useState<string | null>(null);
  const [lightboxTitle, setLightboxTitle] = useState<string>("");

  // Delete Confirm Modal
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);

  // Copied feedback
  const [copied, setCopied] = useState(false);
  const [loadingAction, setLoadingAction] = useState(false);

  // QR Code for Mobile Check-in state
  const [qrCodeDataUrl, setQrCodeDataUrl] = useState<string | null>(null);
  const [isGeneratingQr, setIsGeneratingQr] = useState<boolean>(true);
  const [copiedCheckInUrl, setCopiedCheckInUrl] = useState<boolean>(false);

  // Generate QR Code whenever repairCase.caseId changes
  useEffect(() => {
    let isMounted = true;
    setIsGeneratingQr(true);
    generateCaseCheckInQrCode(repairCase.caseId, { width: 320 })
      .then((dataUrl) => {
        if (isMounted) {
          setQrCodeDataUrl(dataUrl);
          setIsGeneratingQr(false);
        }
      })
      .catch((err) => {
        console.error("Failed to generate QR code for case:", err);
        if (isMounted) setIsGeneratingQr(false);
      });
    return () => {
      isMounted = false;
    };
  }, [repairCase.caseId]);

  const handleCopyCheckInUrl = () => {
    const url = getMobileCheckInUrl(repairCase.caseId);
    navigator.clipboard.writeText(url);
    setCopiedCheckInUrl(true);
    setTimeout(() => setCopiedCheckInUrl(false), 2500);
  };

  const handleDownloadQr = () => {
    if (qrCodeDataUrl) {
      downloadQrCodeImage(qrCodeDataUrl, `qr_checkin_${repairCase.caseId}.png`);
    }
  };

  const handleOpenMobileCheckInView = () => {
    const url = getMobileCheckInUrl(repairCase.caseId);
    window.open(url, "_blank");
  };

  const handleCopyCaseId = () => {
    navigator.clipboard.writeText(repairCase.caseId);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const handleOpenLightbox = (url: string, title: string) => {
    setLightboxImage(url);
    setLightboxTitle(title);
  };

  const handleSaveEdit = async () => {
    setLoadingAction(true);
    try {
      await updateCaseData(repairCase.caseId, {
        customerName: editCustomerName.trim(),
        phone: editPhone.trim(),
        address: editAddress.trim(),
        details: editDetails.trim(),
        category: editCategory,
        priority: editPriority
      });
      setIsEditing(false);
    } catch (e) {
      console.error(e);
    } finally {
      setLoadingAction(false);
    }
  };

  const handleReassign = async () => {
    const tech = technicians.find(t => t.id === reassignTechId);
    if (!tech || tech.id === repairCase.technicianId) return;

    setLoadingAction(true);
    try {
      await reassignTechnician(repairCase.caseId, tech.id, tech.name, repairCase.customerName);
      setIsReassigning(false);
    } catch (e) {
      console.error(e);
    } finally {
      setLoadingAction(false);
    }
  };

  const handleCloseCase = async () => {
    if (!solutionNotes.trim()) {
      alert("กรุณาระบุสรุปวิธีการแก้ไขปัญหา / ผลการตรวจซ่อม");
      return;
    }

    setLoadingAction(true);
    try {
      await closeRepairCase(
        repairCase.caseId, 
        solutionNotes, 
        checkOutPhoto, 
        "Web Admin (ฝ่ายจ่ายงาน)"
      );
      setShowCloseForm(false);
    } catch (e) {
      console.error(e);
    } finally {
      setLoadingAction(false);
    }
  };

  const handleReopen = async () => {
    if (!confirm("คุณต้องการเปิดเคสนี้กลับมาดำเนินการใหม่อีกครั้งใช่หรือไม่?")) return;
    setLoadingAction(true);
    try {
      await reopenCase(repairCase.caseId, "Web Admin");
    } catch (e) {
      console.error(e);
    } finally {
      setLoadingAction(false);
    }
  };

  const handleDelete = async () => {
    setLoadingAction(true);
    try {
      await deleteRepairCase(repairCase.caseId);
      setShowDeleteConfirm(false);
      onClose();
    } catch (e) {
      console.error(e);
    } finally {
      setLoadingAction(false);
    }
  };

  const formatDate = (ts: any) => {
    if (!ts) return "-";
    if (ts.toDate) return ts.toDate().toLocaleString("th-TH");
    if (typeof ts === "number") return new Date(ts).toLocaleString("th-TH");
    return "-";
  };

  const getWorkingDurationMinutes = () => {
    if (!repairCase.checkInAt || !repairCase.closedAt) return null;
    const checkInMs = repairCase.checkInAt?.toMillis ? repairCase.checkInAt.toMillis() : (typeof repairCase.checkInAt === "number" ? repairCase.checkInAt : new Date(repairCase.checkInAt).getTime());
    const closedMs = repairCase.closedAt?.toMillis ? repairCase.closedAt.toMillis() : (typeof repairCase.closedAt === "number" ? repairCase.closedAt : new Date(repairCase.closedAt).getTime());
    
    if (isNaN(checkInMs) || isNaN(closedMs) || closedMs <= checkInMs) return null;
    const diffMs = closedMs - checkInMs;
    const mins = Math.floor(diffMs / 60000);
    const hours = Math.floor(mins / 60);
    const remMins = mins % 60;
    if (hours > 0) {
      return `${hours} ชม. ${remMins} นาที (${mins} นาที)`;
    }
    return `${mins} นาที`;
  };

  const handleDownloadImage = async (
    imageUrl: string,
    fileName: string
  ) => {
    try {
      const response = await fetch(imageUrl);
      const blob = await response.blob();
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = fileName;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);
    } catch (error) {
      console.error('Download failed:', error);
      // fallback: เปิดในแท็บใหม่แทน
      window.open(imageUrl, '_blank');
    }
  };

  return (
    <>
      <div 
        id="case-detail-backdrop"
        className="fixed inset-0 z-50 bg-slate-950/80 backdrop-blur-sm flex items-center justify-center p-4 overflow-y-auto"
        onClick={onClose}
      >
        <div 
          id="case-detail-modal"
          className="relative w-full max-w-3xl bg-slate-900 border border-slate-800 text-slate-100 rounded-2xl shadow-2xl overflow-hidden my-8"
          onClick={(e) => e.stopPropagation()}
        >
          {/* Header */}
          <div className="flex items-center justify-between px-6 py-4 border-b border-slate-800 bg-slate-900/90">
            <div className="flex items-center space-x-3">
              <span className={`px-2.5 py-1 rounded-lg text-xs font-bold ${
                repairCase.status === "CLOSED" 
                  ? "bg-emerald-500/20 text-emerald-300 border border-emerald-500/40" 
                  : "bg-amber-500/20 text-amber-300 border border-amber-500/40"
              }`}>
                {repairCase.status === "CLOSED" ? "ปิดงานแล้ว (CLOSED)" : "กำลังดำเนินการ (OPEN)"}
              </span>
              <div className="flex items-center space-x-2">
                <span className="font-mono text-sm font-bold text-indigo-400">{repairCase.caseId}</span>
                {repairCase.createdSource === "APP" ? (
                  <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-bold bg-cyan-950/90 text-cyan-300 border border-cyan-800" title="เคสนี้ถูกเปิดผ่านแอปช่าง (Mobile App)">
                    <Smartphone className="w-3 h-3 mr-1" />
                    แอปช่าง
                  </span>
                ) : (
                  <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-bold bg-slate-800 text-slate-300 border border-slate-700" title="เคสนี้ถูกเปิดผ่านเว็บ Admin">
                    <Globe className="w-3 h-3 mr-1 text-slate-400" />
                    เว็บ Admin
                  </span>
                )}
                <button
                  onClick={handleCopyCaseId}
                  title="คัดลอกรหัสเคส"
                  className="p-1 text-slate-400 hover:text-slate-200 hover:bg-slate-800 rounded transition-colors"
                >
                  {copied ? <Check className="w-3.5 h-3.5 text-emerald-400" /> : <Copy className="w-3.5 h-3.5" />}
                </button>
              </div>
            </div>

            <div className="flex items-center space-x-2">
              {onPrintRequest && (
                <button
                  onClick={() => onPrintRequest(repairCase)}
                  className="px-3 py-1.5 bg-slate-800 hover:bg-slate-700 text-slate-200 rounded-lg text-xs font-medium flex items-center space-x-1.5 transition-colors"
                  title="พิมพ์ใบสั่งซ่อม"
                >
                  <Printer className="w-3.5 h-3.5 text-indigo-400" />
                  <span className="hidden sm:inline">พิมพ์ใบงาน</span>
                </button>
              )}
              <button
                onClick={onClose}
                className="p-1.5 text-slate-400 hover:text-slate-100 hover:bg-slate-800 rounded-lg transition-colors"
              >
                <X className="w-5 h-5" />
              </button>
            </div>
          </div>

          {/* Modal Body */}
          <div className="p-6 space-y-6 max-h-[75vh] overflow-y-auto">
            
            {/* Top Cards: Customer & Job Info */}
            {!isEditing ? (
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {/* Customer Box */}
                <div className="bg-slate-800/60 border border-slate-700/80 rounded-xl p-4 space-y-2">
                  <div className="flex items-center justify-between text-xs text-slate-400 font-semibold uppercase tracking-wider">
                    <span>ข้อมูลลูกค้า</span>
                    <button
                      onClick={() => setIsEditing(true)}
                      className="text-indigo-400 hover:text-indigo-300 flex items-center space-x-1 lowercase font-normal"
                    >
                      <Edit3 className="w-3 h-3" />
                      <span>แก้ไข</span>
                    </button>
                  </div>
                  <h3 className="text-base font-bold text-slate-100">{repairCase.customerName}</h3>
                  <div className="flex items-center space-x-2 text-xs">
                    <a 
                      href={`tel:${repairCase.phone}`} 
                      className="inline-flex items-center space-x-1 px-2.5 py-1 bg-indigo-600/20 text-indigo-300 border border-indigo-500/30 rounded-lg hover:bg-indigo-600/30 transition-colors"
                    >
                      <Phone className="w-3.5 h-3.5" />
                      <span>{repairCase.phone}</span>
                    </a>
                  </div>
                  <div className="flex items-start text-xs text-slate-300 pt-1">
                    <MapPin className="w-4 h-4 mr-1 text-slate-400 shrink-0 mt-0.5" />
                    <span>{repairCase.address}</span>
                  </div>

                  {repairCase.latitude !== undefined && repairCase.longitude !== undefined && (
                    <div className="pt-2">
                      <a 
                        href={`https://www.google.com/maps?q=${repairCase.latitude},${repairCase.longitude}`}
                        target="_blank" 
                        rel="noopener noreferrer"
                        className="inline-flex items-center space-x-1.5 px-3 py-1.5 bg-emerald-600/20 text-emerald-300 border border-emerald-500/30 rounded-lg hover:bg-emerald-600/30 text-xs font-medium transition-colors"
                      >
                        <MapPin className="w-3.5 h-3.5 text-emerald-400" />
                        <span>เปิดแผนที่ Google Maps ({repairCase.latitude}, {repairCase.longitude})</span>
                        <ExternalLink className="w-3 h-3 ml-0.5 opacity-70" />
                      </a>
                    </div>
                  )}
                </div>

                {/* Dispatch & Technician Box */}
                <div className="bg-slate-800/60 border border-slate-700/80 rounded-xl p-4 space-y-2">
                  <div className="flex items-center justify-between text-xs text-slate-400 font-semibold uppercase tracking-wider">
                    <span>ช่างผู้รับผิดชอบ</span>
                    <button
                      onClick={() => setIsReassigning(!isReassigning)}
                      className="text-amber-400 hover:text-amber-300 flex items-center space-x-1 lowercase font-normal"
                    >
                      <RefreshCw className="w-3 h-3" />
                      <span>โอนย้ายช่าง</span>
                    </button>
                  </div>

                  {isReassigning ? (
                    <div className="space-y-2 pt-1">
                      <select
                        value={reassignTechId}
                        onChange={(e) => setReassignTechId(e.target.value)}
                        className="w-full px-3 py-1.5 bg-slate-900 border border-slate-700 rounded-lg text-xs text-slate-100"
                      >
                        {technicians.map((t) => (
                          <option key={t.id} value={t.id}>
                            {t.name} ({t.id}) {t.isActive ? "• พร้อม" : "• ปิดรับ"}
                          </option>
                        ))}
                      </select>
                      <div className="flex space-x-2">
                        <button
                          onClick={handleReassign}
                          disabled={loadingAction}
                          className="px-3 py-1 bg-amber-600 hover:bg-amber-500 text-white rounded text-xs font-medium"
                        >
                          ยืนยันย้ายงาน
                        </button>
                        <button
                          onClick={() => setIsReassigning(false)}
                          className="px-3 py-1 bg-slate-700 text-slate-300 rounded text-xs"
                        >
                          ยกเลิก
                        </button>
                      </div>
                    </div>
                  ) : (
                    <div>
                      <h4 className="text-sm font-bold text-indigo-300 flex items-center">
                        <User className="w-4 h-4 mr-1.5 text-indigo-400" />
                        {repairCase.technicianName}
                      </h4>
                      <p className="text-xs text-slate-400 mt-1">รหัสช่าง: {repairCase.technicianId}</p>
                    </div>
                  )}

                  <div className="pt-2 border-t border-slate-700/60 flex items-center justify-between text-[11px] text-slate-400">
                    <span>หมวดหมู่: <strong className="text-slate-200">{repairCase.category}</strong></span>
                    <span>ความสำคัญ: <strong className={
                      repairCase.priority === "ด่วนที่สุด" ? "text-rose-400" :
                      repairCase.priority === "ด่วน" ? "text-amber-400" : "text-sky-400"
                    }>{repairCase.priority}</strong></span>
                  </div>
                </div>
              </div>
            ) : (
              /* Inline Edit Form */
              <div className="bg-slate-800/80 border border-indigo-500/40 rounded-xl p-4 space-y-3">
                <h4 className="text-xs font-bold text-indigo-300 uppercase">แก้ไขข้อมูลเคส</h4>
                <div className="grid grid-cols-2 gap-3">
                  <div>
                    <label className="text-[11px] text-slate-400 block mb-1">ชื่อลูกค้า</label>
                    <input
                      type="text"
                      value={editCustomerName}
                      onChange={(e) => setEditCustomerName(e.target.value)}
                      className="w-full px-2.5 py-1.5 bg-slate-900 border border-slate-700 rounded-lg text-xs"
                    />
                  </div>
                  <div>
                    <label className="text-[11px] text-slate-400 block mb-1">เบอร์โทรศัพท์</label>
                    <input
                      type="text"
                      value={editPhone}
                      onChange={(e) => setEditPhone(e.target.value)}
                      className="w-full px-2.5 py-1.5 bg-slate-900 border border-slate-700 rounded-lg text-xs"
                    />
                  </div>
                </div>
                <div>
                  <label className="text-[11px] text-slate-400 block mb-1">สถานที่ / ที่อยู่</label>
                  <input
                    type="text"
                    value={editAddress}
                    onChange={(e) => setEditAddress(e.target.value)}
                    className="w-full px-2.5 py-1.5 bg-slate-900 border border-slate-700 rounded-lg text-xs"
                  />
                </div>
                <div className="grid grid-cols-2 gap-3">
                  <div>
                    <label className="text-[11px] text-slate-400 block mb-1">หมวดหมู่</label>
                    <select
                      value={editCategory}
                      onChange={(e) => setEditCategory(e.target.value)}
                      className="w-full px-2.5 py-1.5 bg-slate-900 border border-slate-700 rounded-lg text-xs"
                    >
                      {REPAIR_CATEGORIES.map(c => <option key={c} value={c}>{c}</option>)}
                    </select>
                  </div>
                  <div>
                    <label className="text-[11px] text-slate-400 block mb-1">ความสำคัญ</label>
                    <select
                      value={editPriority}
                      onChange={(e) => setEditPriority(e.target.value as CasePriority)}
                      className="w-full px-2.5 py-1.5 bg-slate-900 border border-slate-700 rounded-lg text-xs"
                    >
                      <option value="ปกติ">ปกติ</option>
                      <option value="ด่วน">ด่วน</option>
                      <option value="ด่วนที่สุด">ด่วนที่สุด</option>
                    </select>
                  </div>
                </div>
                <div>
                  <label className="text-[11px] text-slate-400 block mb-1">รายละเอียดอาการเสีย</label>
                  <textarea
                    rows={2}
                    value={editDetails}
                    onChange={(e) => setEditDetails(e.target.value)}
                    className="w-full p-2 bg-slate-900 border border-slate-700 rounded-lg text-xs"
                  />
                </div>
                <div className="flex justify-end space-x-2 pt-2">
                  <button
                    onClick={() => setIsEditing(false)}
                    className="px-3 py-1 bg-slate-700 text-slate-300 rounded-lg text-xs"
                  >
                    ยกเลิก
                  </button>
                  <button
                    onClick={handleSaveEdit}
                    disabled={loadingAction}
                    className="px-4 py-1 bg-indigo-600 hover:bg-indigo-500 text-white rounded-lg text-xs font-semibold"
                  >
                    บันทึกการแก้ไข
                  </button>
                </div>
              </div>
            )}

            {/* Problem Details */}
            <div className="bg-slate-800/40 border border-slate-800 rounded-xl p-4 space-y-1.5">
              <h4 className="text-xs font-semibold text-slate-300">รายละเอียดอาการเสีย / ปัญหาที่แจ้ง:</h4>
              <p className="text-xs text-slate-200 leading-relaxed whitespace-pre-wrap">{repairCase.details}</p>
            </div>

            {/* QR Code Section for Mobile Check-in */}
            <div className="bg-gradient-to-br from-slate-900 via-slate-850 to-slate-900 border border-slate-700/80 rounded-2xl p-4 sm:p-5 shadow-lg space-y-4">
              <div className="flex flex-wrap items-center justify-between gap-2 border-b border-slate-800 pb-3">
                <div className="flex items-center space-x-2.5">
                  <div className="w-8 h-8 rounded-lg bg-orange-500/20 border border-orange-500/30 flex items-center justify-center text-orange-400 shrink-0">
                    <QrCode className="w-4 h-4" />
                  </div>
                  <div>
                    <h4 className="text-xs font-bold uppercase tracking-wider text-slate-100 flex items-center gap-1.5">
                      <span>QR Code สแกน Check-in หน้างาน</span>
                      <span className="text-[10px] font-mono font-semibold px-1.5 py-0.5 bg-orange-500/20 text-orange-400 border border-orange-500/30 rounded">
                        MOBILE CHECK-IN
                      </span>
                    </h4>
                    <p className="text-[11px] text-slate-400">
                      ช่างสามารถใช้กล้องมือถือสแกน เพื่อเปิดหน้าเช็คอินและถ่ายรูปยืนยันได้ทันที
                    </p>
                  </div>
                </div>

                {repairCase.checkInImageUrl || repairCase.checkInAt ? (
                  <span className="inline-flex items-center text-[11px] font-semibold text-emerald-400 bg-emerald-500/10 border border-emerald-500/30 px-2.5 py-1 rounded-full">
                    <CheckCircle2 className="w-3.5 h-3.5 mr-1 text-emerald-400" />
                    เช็คอินแล้ว
                  </span>
                ) : (
                  <span className="inline-flex items-center text-[11px] font-semibold text-amber-400 bg-amber-500/10 border border-amber-500/30 px-2.5 py-1 rounded-full">
                    <Clock className="w-3.5 h-3.5 mr-1 text-amber-400" />
                    รอสแกนเช็คอิน
                  </span>
                )}
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-12 gap-4 items-center bg-slate-950/70 border border-slate-800/90 rounded-xl p-4">
                {/* QR Code Canvas/Image */}
                <div className="sm:col-span-4 flex flex-col items-center justify-center">
                  <div className="bg-white p-2.5 rounded-xl shadow-md border-2 border-slate-700/60 relative">
                    {isGeneratingQr ? (
                      <div className="w-32 h-32 flex flex-col items-center justify-center text-slate-700 space-y-2">
                        <RefreshCw className="w-5 h-5 animate-spin text-orange-500" />
                        <span className="text-[10px] text-slate-500">สร้าง QR Code...</span>
                      </div>
                    ) : qrCodeDataUrl ? (
                      <>
                        <img
                          src={qrCodeDataUrl}
                          alt={`QR Code Case ${repairCase.caseId}`}
                          className="w-32 h-32 object-contain block rounded cursor-pointer"
                          onClick={() => handleOpenLightbox(qrCodeDataUrl, `QR Code เคส ${repairCase.caseId}`)}
                          title="คลิกเพื่อขยาย QR Code"
                        />
                        <div className="text-[9px] text-center font-mono font-bold text-slate-800 mt-1">
                          {repairCase.caseId}
                        </div>
                      </>
                    ) : (
                      <div className="w-32 h-32 flex items-center justify-center text-rose-500 text-xs">
                        ไม่สามารถสร้าง QR ได้
                      </div>
                    )}
                  </div>
                  <span className="text-[10px] text-slate-400 mt-1.5 flex items-center">
                    <Smartphone className="w-3 h-3 mr-1 text-slate-500" /> สแกนด้วยกล้องมือถือ
                  </span>
                </div>

                {/* Direct Link & Action Controls */}
                <div className="sm:col-span-8 space-y-3">
                  <div>
                    <label className="text-[11px] text-slate-400 block mb-1 font-medium">
                      ลิงก์ตรงสำหรับ Mobile Check-in (Direct URL):
                    </label>
                    <div className="flex items-center space-x-2">
                      <input
                        type="text"
                        readOnly
                        value={getMobileCheckInUrl(repairCase.caseId)}
                        className="flex-1 px-2.5 py-1.5 bg-slate-900 border border-slate-700/80 rounded-lg text-xs font-mono text-indigo-300 select-all outline-none"
                      />
                      <button
                        type="button"
                        onClick={handleCopyCheckInUrl}
                        className={`px-3 py-1.5 rounded-lg text-xs font-medium flex items-center space-x-1 transition-colors shrink-0 ${
                          copiedCheckInUrl
                            ? "bg-emerald-600 text-white"
                            : "bg-slate-800 hover:bg-slate-700 text-slate-200 border border-slate-700"
                        }`}
                      >
                        {copiedCheckInUrl ? (
                          <>
                            <Check className="w-3.5 h-3.5 text-white" />
                            <span>คัดลอกแล้ว</span>
                          </>
                        ) : (
                          <>
                            <Copy className="w-3.5 h-3.5" />
                            <span>คัดลอกลิงก์</span>
                          </>
                        )}
                      </button>
                    </div>
                  </div>

                  <div className="flex flex-wrap gap-2 pt-0.5">
                    <button
                      type="button"
                      onClick={handleDownloadQr}
                      disabled={!qrCodeDataUrl || isGeneratingQr}
                      className="px-3 py-1.5 bg-slate-800 hover:bg-slate-700 text-slate-200 border border-slate-700/90 rounded-lg text-xs font-medium flex items-center space-x-1.5 transition-colors disabled:opacity-50"
                    >
                      <Download className="w-3.5 h-3.5 text-orange-400" />
                      <span>บันทึกรูป QR Code</span>
                    </button>

                    <button
                      type="button"
                      onClick={handleOpenMobileCheckInView}
                      className="px-3 py-1.5 bg-indigo-600/20 hover:bg-indigo-600/30 text-indigo-300 border border-indigo-500/30 rounded-lg text-xs font-medium flex items-center space-x-1.5 transition-colors"
                    >
                      <ExternalLink className="w-3.5 h-3.5 text-indigo-400" />
                      <span>ทดสอบเปิดหน้า Check-in</span>
                    </button>

                    <a
                      href={`https://line.me/R/msg/text/?${encodeURIComponent(
                        `แจ้งงานซ่อมเคส ${repairCase.caseId}\nลูกค้า: ${repairCase.customerName}\nสถานที่: ${repairCase.address}\n\nสแกน/คลิกลิงก์เพื่อ Check-in หน้างาน:\n${getMobileCheckInUrl(repairCase.caseId)}`
                      )}`}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="px-3 py-1.5 bg-emerald-600/20 hover:bg-emerald-600/30 text-emerald-300 border border-emerald-500/30 rounded-lg text-xs font-medium flex items-center space-x-1.5 transition-colors"
                    >
                      <Send className="w-3.5 h-3.5 text-emerald-400" />
                      <span>ส่ง LINE ให้ช่าง</span>
                    </a>
                  </div>
                </div>
              </div>
            </div>

            {/* Photo Progression Timeline - Changed to compact email attachment list style */}
            <div className="bg-slate-800/30 border border-slate-800 rounded-xl p-4 space-y-3">
              <h4 className="text-xs font-bold uppercase tracking-wider text-slate-400 flex items-center">
                <Paperclip className="w-4 h-4 mr-1.5 text-orange-400" />
                รูปภาพหลักฐานการทำงาน (รายการไฟล์แนบ)
              </h4>

              <div className="bg-slate-900/60 border border-slate-800 rounded-xl divide-y divide-slate-800/80 overflow-hidden">
                {/* 1. รูปแจ้งซ่อม */}
                {repairCase.imageUrl ? (
                  <div className="p-3 flex items-center justify-between text-xs hover:bg-slate-800/20 transition-colors">
                    <div className="flex items-center space-x-3 min-w-0">
                      <Paperclip className="w-4 h-4 text-slate-400 flex-shrink-0" />
                      <div className="min-w-0">
                        <span className="font-semibold text-slate-200 truncate block">before_{repairCase.caseId}.jpg</span>
                        <span className="text-[10px] text-slate-500 block">ขนาดไฟล์: — | รูปแจ้งซ่อม</span>
                      </div>
                    </div>
                    <div className="flex items-center space-x-2 flex-shrink-0 ml-3">
                      <button
                        onClick={() => handleOpenLightbox(repairCase.imageUrl!, "รูปแจ้งซ่อม")}
                        className="px-2.5 py-1.5 bg-slate-800 hover:bg-slate-700 text-slate-300 hover:text-white rounded-md text-[11px] font-medium transition-colors cursor-pointer flex items-center space-x-1"
                      >
                        <Eye className="w-3.5 h-3.5" />
                        <span>ดู</span>
                      </button>
                      <button
                        onClick={() => handleDownloadImage(repairCase.imageUrl!, `before_${repairCase.caseId}.jpg`)}
                        className="px-2.5 py-1.5 bg-slate-800 hover:bg-slate-700 text-slate-300 hover:text-white rounded-md text-[11px] font-medium transition-colors cursor-pointer flex items-center space-x-1"
                      >
                        <Download className="w-3.5 h-3.5" />
                        <span>ดาวน์โหลด</span>
                      </button>
                    </div>
                  </div>
                ) : (
                  <div className="p-3 flex items-center text-xs text-slate-500 bg-slate-900/30">
                    <Clock className="w-4 h-4 text-slate-600 mr-2.5 flex-shrink-0" />
                    <span>ไม่มีรูปแจ้งซ่อม</span>
                  </div>
                )}

                {/* 2. รูป Check-in */}
                {repairCase.checkInImageUrl ? (
                  <div className="p-3 flex items-center justify-between text-xs hover:bg-slate-800/20 transition-colors">
                    <div className="flex items-center space-x-3 min-w-0">
                      <Paperclip className="w-4 h-4 text-slate-400 flex-shrink-0" />
                      <div className="min-w-0">
                        <span className="font-semibold text-slate-200 truncate block">checkin_{repairCase.caseId}.jpg</span>
                        <span className="text-[10px] text-slate-500 block">ขนาดไฟล์: — | รูป Check-in</span>
                      </div>
                    </div>
                    <div className="flex items-center space-x-2 flex-shrink-0 ml-3">
                      <button
                        onClick={() => handleOpenLightbox(repairCase.checkInImageUrl!, "รูป Check-in หน้างาน")}
                        className="px-2.5 py-1.5 bg-slate-800 hover:bg-slate-700 text-slate-300 hover:text-white rounded-md text-[11px] font-medium transition-colors cursor-pointer flex items-center space-x-1"
                      >
                        <Eye className="w-3.5 h-3.5" />
                        <span>ดู</span>
                      </button>
                      <button
                        onClick={() => handleDownloadImage(repairCase.checkInImageUrl!, `checkin_${repairCase.caseId}.jpg`)}
                        className="px-2.5 py-1.5 bg-slate-800 hover:bg-slate-700 text-slate-300 hover:text-white rounded-md text-[11px] font-medium transition-colors cursor-pointer flex items-center space-x-1"
                      >
                        <Download className="w-3.5 h-3.5" />
                        <span>ดาวน์โหลด</span>
                      </button>
                    </div>
                  </div>
                ) : (
                  <div className="p-3 flex items-center text-xs text-slate-500 bg-slate-900/30">
                    <Clock className="w-4 h-4 text-slate-600 mr-2.5 flex-shrink-0" />
                    <span>รอช่างเช็คอิน</span>
                  </div>
                )}

                {/* 3. รูปปิดงาน */}
                {repairCase.checkOutImageUrl ? (
                  <div className="p-3 flex items-center justify-between text-xs hover:bg-slate-800/20 transition-colors">
                    <div className="flex items-center space-x-3 min-w-0">
                      <Paperclip className="w-4 h-4 text-slate-400 flex-shrink-0" />
                      <div className="min-w-0">
                        <span className="font-semibold text-slate-200 truncate block">checkout_{repairCase.caseId}.jpg</span>
                        <span className="text-[10px] text-slate-500 block">ขนาดไฟล์: — | รูปปิดงาน</span>
                      </div>
                    </div>
                    <div className="flex items-center space-x-2 flex-shrink-0 ml-3">
                      <button
                        onClick={() => handleOpenLightbox(repairCase.checkOutImageUrl!, "รูปปิดงาน (Check-out)")}
                        className="px-2.5 py-1.5 bg-slate-800 hover:bg-slate-700 text-slate-300 hover:text-white rounded-md text-[11px] font-medium transition-colors cursor-pointer flex items-center space-x-1"
                      >
                        <Eye className="w-3.5 h-3.5" />
                        <span>ดู</span>
                      </button>
                      <button
                        onClick={() => handleDownloadImage(repairCase.checkOutImageUrl!, `checkout_${repairCase.caseId}.jpg`)}
                        className="px-2.5 py-1.5 bg-slate-800 hover:bg-slate-700 text-slate-300 hover:text-white rounded-md text-[11px] font-medium transition-colors cursor-pointer flex items-center space-x-1"
                      >
                        <Download className="w-3.5 h-3.5" />
                        <span>ดาวน์โหลด</span>
                      </button>
                    </div>
                  </div>
                ) : (
                  <div className="p-3 flex items-center text-xs text-slate-500 bg-slate-900/30">
                    <Clock className="w-4 h-4 text-slate-600 mr-2.5 flex-shrink-0" />
                    <span>รอปิดงาน</span>
                  </div>
                )}
              </div>
            </div>

            {/* Solutions / Work Results */}
            {repairCase.solutions ? (
              <div className="bg-emerald-950/30 border border-emerald-500/30 rounded-xl p-4 space-y-1.5">
                <div className="flex items-center justify-between text-xs text-emerald-400 font-semibold">
                  <span className="flex items-center">
                    <CheckCircle2 className="w-4 h-4 mr-1" /> ผลการตรวจซ่อม / วิธีการแก้ไข (Solutions):
                  </span>
                  {repairCase.closedBy && <span className="text-[11px] text-slate-400">ปิดโดย: {repairCase.closedBy}</span>}
                </div>
                <p className="text-xs text-slate-200 leading-relaxed whitespace-pre-wrap">{repairCase.solutions}</p>
              </div>
            ) : null}

            {/* Close Case Form (Collapsible) */}
            {showCloseForm && (
              <div className="bg-slate-800/90 border border-emerald-500/50 rounded-xl p-4 space-y-3 animate-fadeIn">
                <div className="flex items-center justify-between text-xs font-bold text-emerald-400">
                  <span>บันทึกสรุปผลงาน & ปิดเคส (Close Case)</span>
                  <button onClick={() => setShowCloseForm(false)} className="text-slate-400 hover:text-white">ยกเลิก</button>
                </div>
                <div>
                  <label className="text-[11px] text-slate-300 block mb-1">สรุปวิธีการแก้ไขปัญหา / ผลการซ่อมแซม *</label>
                  <textarea
                    rows={3}
                    required
                    placeholder="เช่น ทำการเปลี่ยนคาปาซิเตอร์คอมเพรสเซอร์ ล้างฟิลเตอร์ และเติมน้ำยา R32 ปริมาณ 1.5 ปอนด์ ทดสอบความเย็นทำงานปกติ..."
                    value={solutionNotes}
                    onChange={(e) => setSolutionNotes(e.target.value)}
                    className="w-full p-2.5 bg-slate-900 border border-slate-700 rounded-lg text-xs text-slate-100"
                  />
                </div>
                <div className="flex justify-end space-x-2">
                  <button
                    onClick={handleCloseCase}
                    disabled={loadingAction}
                    className="px-4 py-2 bg-emerald-600 hover:bg-emerald-500 text-white rounded-lg text-xs font-semibold flex items-center space-x-1.5"
                  >
                    <CheckCircle2 className="w-4 h-4" />
                    <span>ยืนยันปิดเคสงานซ่อม</span>
                  </button>
                </div>
              </div>
            )}

            {/* Timestamps info */}
            <div className="bg-slate-950/60 rounded-xl p-3 border border-slate-800 text-[11px] text-slate-400 grid grid-cols-2 sm:grid-cols-3 gap-2">
              <div>
                <span>สร้างเคสเมื่อ:</span>
                <p className="text-slate-200 font-medium">{formatDate(repairCase.createdAt || repairCase.timestamp)}</p>
              </div>
              <div>
                <span>เช็คอินเข้างาน:</span>
                <p className="text-slate-200 font-medium">{formatDate(repairCase.checkInAt)}</p>
              </div>
              <div>
                <span>ปิดงานเมื่อ:</span>
                <p className="text-slate-200 font-medium">{formatDate(repairCase.closedAt)}</p>
              </div>
            </div>

            {/* Working duration info */}
            {getWorkingDurationMinutes() && (
              <div className="bg-indigo-950/40 border border-indigo-500/40 rounded-xl p-3 flex items-center justify-between text-xs">
                <span className="flex items-center text-indigo-300 font-semibold">
                  <Clock className="w-4 h-4 mr-1.5" /> ระยะเวลาปฏิบัติงานจริง (Duration):
                </span>
                <span className="font-bold text-indigo-200 bg-indigo-900/60 px-2.5 py-1 rounded-lg">
                  {getWorkingDurationMinutes()}
                </span>
              </div>
            )}

          </div>

          {/* Footer Actions */}
          <div className="px-6 py-4 border-t border-slate-800 bg-slate-900/90 flex flex-wrap items-center justify-between gap-3">
            <button
              onClick={() => setShowDeleteConfirm(true)}
              className="px-3 py-1.5 text-rose-400 hover:text-rose-300 hover:bg-rose-950/30 rounded-lg text-xs font-medium flex items-center space-x-1.5 transition-colors border border-rose-900/30"
            >
              <Trash2 className="w-3.5 h-3.5" />
              <span>ลบเคส</span>
            </button>

            <div className="flex items-center space-x-2">
              {repairCase.status === "OPEN" ? (
                <button
                  onClick={() => setShowCloseForm(true)}
                  className="px-4 py-2 bg-emerald-600 hover:bg-emerald-500 text-white rounded-xl text-xs font-semibold flex items-center space-x-1.5 transition-all shadow-md shadow-emerald-600/30"
                >
                  <CheckCircle2 className="w-4 h-4" />
                  <span>ปิดเคสงานนี้</span>
                </button>
              ) : (
                <button
                  onClick={handleReopen}
                  className="px-4 py-2 bg-slate-800 hover:bg-slate-700 text-amber-300 rounded-xl text-xs font-semibold flex items-center space-x-1.5 transition-colors border border-amber-500/30"
                >
                  <RefreshCw className="w-4 h-4" />
                  <span>เปิดเคสใหม่อีกครั้ง (Reopen)</span>
                </button>
              )}
            </div>
          </div>
        </div>
      </div>

      {/* Delete Confirmation Alert */}
      {showDeleteConfirm && (
        <div className="fixed inset-0 z-60 bg-slate-950/90 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-slate-900 border border-rose-600/50 rounded-2xl p-6 max-w-sm w-full space-y-4 shadow-2xl text-slate-100">
            <div className="flex items-center space-x-3 text-rose-400">
              <ShieldAlert className="w-6 h-6" />
              <h4 className="font-bold text-base">ยืนยันการลบเคสงานซ่อม?</h4>
            </div>
            <p className="text-xs text-slate-300 leading-relaxed">
              คุณแน่ใจหรือไม่ว่าต้องการลบเคส <strong className="text-indigo-300">{repairCase.caseId}</strong> ออกจาก Firestore? เอกสารจะถูกลบและซิงค์ออกจากแอปช่างทันที
            </p>
            <div className="flex justify-end space-x-2 pt-2">
              <button
                onClick={() => setShowDeleteConfirm(false)}
                className="px-3 py-1.5 bg-slate-800 text-slate-300 rounded-lg text-xs"
              >
                ยกเลิก
              </button>
              <button
                onClick={handleDelete}
                disabled={loadingAction}
                className="px-4 py-1.5 bg-rose-600 hover:bg-rose-500 text-white rounded-lg text-xs font-semibold"
              >
                {loadingAction ? "กำลังลบ..." : "ยืนยันการลบ"}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Lightbox Component */}
      <ImageLightbox
        isOpen={!!lightboxImage}
        imageUrl={lightboxImage || ""}
        title={lightboxTitle}
        onClose={() => setLightboxImage(null)}
      />
    </>
  );
};

export const CaseDetailModal: React.FC<CaseDetailModalProps> = ({
  isOpen,
  repairCase,
  technicians,
  onClose,
  onPrintRequest
}) => {
  if (!isOpen || !repairCase) return null;

  return (
    <CaseDetailModalContent
      repairCase={repairCase}
      technicians={technicians}
      onClose={onClose}
      onPrintRequest={onPrintRequest}
    />
  );
};
