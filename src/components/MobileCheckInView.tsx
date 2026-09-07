import React, { useState, useEffect, useRef } from "react";
import { 
  Camera, 
  MapPin, 
  Phone, 
  CheckCircle2, 
  AlertTriangle, 
  Clock, 
  ExternalLink, 
  Loader2, 
  UploadCloud, 
  Check, 
  User, 
  ArrowLeft, 
  Navigation,
  ShieldCheck,
  CheckCircle,
  FileCheck
} from "lucide-react";
import { RepairCase } from "../types";
import { 
  subscribeToCaseById, 
  getCaseById, 
  recordCheckIn, 
  uploadCaseImage, 
  closeRepairCase 
} from "../services/caseService";
import { Logo } from "./Logo";
import { ImageLightbox } from "./ImageLightbox";

interface MobileCheckInViewProps {
  caseId: string;
  onBackToAdmin?: () => void;
}

export const MobileCheckInView: React.FC<MobileCheckInViewProps> = ({
  caseId,
  onBackToAdmin
}) => {
  const [repairCase, setRepairCase] = useState<RepairCase | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  // Check-in form state
  const [checkInFile, setCheckInFile] = useState<File | null>(null);
  const [checkInPreview, setCheckInPreview] = useState<string | null>(null);
  const [gpsLocation, setGpsLocation] = useState<{ lat: number; lng: number } | null>(null);
  const [isLocating, setIsLocating] = useState<boolean>(false);
  const [locationError, setLocationError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState<boolean>(false);
  const [checkInSuccess, setCheckInSuccess] = useState<boolean>(false);

  // Close case / checkout form state on mobile
  const [showCloseSection, setShowCloseSection] = useState<boolean>(false);
  const [closeSolution, setCloseSolution] = useState<string>("");
  const [closeFile, setCloseFile] = useState<File | null>(null);
  const [closePreview, setClosePreview] = useState<string | null>(null);
  const [isClosing, setIsClosing] = useState<boolean>(false);
  const [closeSuccess, setCloseSuccess] = useState<boolean>(false);

  // Lightbox
  const [lightboxImage, setLightboxImage] = useState<string | null>(null);
  const [lightboxTitle, setLightboxTitle] = useState<string>("");

  const fileInputRef = useRef<HTMLInputElement>(null);
  const closeFileInputRef = useRef<HTMLInputElement>(null);

  // Load and subscribe in real-time
  useEffect(() => {
    if (!caseId) {
      setError("ไม่พบรหัสเคสงานซ่อมใน URL");
      setIsLoading(false);
      return;
    }

    setIsLoading(true);
    setError(null);

    // Initial fetch fallback
    getCaseById(caseId)
      .then((data) => {
        if (data) {
          setRepairCase(data);
          setIsLoading(false);
        }
      })
      .catch((err) => {
        console.warn("Initial getCaseById error:", err);
      });

    // Real-time listener
    const unsubscribe = subscribeToCaseById(
      caseId,
      (data) => {
        setIsLoading(false);
        if (data) {
          setRepairCase(data);
          setError(null);
        } else {
          setError(`ไม่พบข้อมูลเคสหมายเลข ${caseId}`);
        }
      },
      (err) => {
        setIsLoading(false);
        setError("ไม่สามารถเชื่อมต่อข้อมูลเคสได้ กรุณาลองใหม่อีกครั้ง");
      }
    );

    return () => unsubscribe();
  }, [caseId]);

  // Request GPS Location
  const handleGetLocation = () => {
    if (!navigator.geolocation) {
      setLocationError("อุปกรณ์ของคุณไม่รองรับการระบุพิกัด GPS");
      return;
    }
    setIsLocating(true);
    setLocationError(null);
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        setGpsLocation({
          lat: pos.coords.latitude,
          lng: pos.coords.longitude
        });
        setIsLocating(false);
      },
      (err) => {
        console.warn("Geolocation error:", err);
        setLocationError("ไม่สามารถเข้าถึงตำแหน่ง GPS ได้ (กรุณาเปิดการแชร์ตำแหน่ง)");
        setIsLocating(false);
      },
      { enableHighAccuracy: true, timeout: 10000, maximumAge: 0 }
    );
  };

  // Handle Photo selection
  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0]) {
      const file = e.target.files[0];
      setCheckInFile(file);
      const reader = new FileReader();
      reader.onload = (event) => {
        setCheckInPreview(event.target?.result as string);
      };
      reader.readAsDataURL(file);
    }
  };

  // Submit Check-in
  const handleSubmitCheckIn = async () => {
    if (!repairCase) return;
    if (!checkInFile && !checkInPreview) {
      alert("กรุณาถ่ายรูปหรือเลือกรูปภาพหลักฐานการเข้าหน้างานก่อนยืนยัน");
      return;
    }

    setIsSubmitting(true);
    try {
      // 1. Upload photo
      let imageUrl = checkInPreview || "";
      if (checkInFile) {
        imageUrl = await uploadCaseImage(repairCase.caseId, checkInFile, "checkin");
      }

      // 2. Record check-in in Firestore
      await recordCheckIn(
        repairCase.caseId,
        imageUrl,
        repairCase.technicianName || "ช่างหน้างาน"
      );

      setCheckInSuccess(true);
      setTimeout(() => setCheckInSuccess(false), 5000);
    } catch (err: any) {
      console.error("Check-in submission failed:", err);
      alert("เกิดข้อผิดพลาดในการบันทึก Check-in: " + (err.message || err));
    } finally {
      setIsSubmitting(false);
    }
  };

  // Handle Close File selection
  const handleCloseFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0]) {
      const file = e.target.files[0];
      setCloseFile(file);
      const reader = new FileReader();
      reader.onload = (event) => {
        setClosePreview(event.target?.result as string);
      };
      reader.readAsDataURL(file);
    }
  };

  // Submit Case Close
  const handleSubmitCloseCase = async () => {
    if (!repairCase) return;
    if (!closeSolution.trim()) {
      alert("กรุณากรอกรายละเอียดผลการตรวจซ่อมหรือวิธีการแก้ไข");
      return;
    }

    setIsClosing(true);
    try {
      let closeImageUrl = "";
      if (closeFile) {
        closeImageUrl = await uploadCaseImage(repairCase.caseId, closeFile, "checkout");
      } else if (closePreview) {
        closeImageUrl = closePreview;
      }

      await closeRepairCase(
        repairCase.caseId,
        closeSolution.trim(),
        closeImageUrl,
        repairCase.technicianName || "ช่างผู้ปฏิบัติงาน"
      );

      setCloseSuccess(true);
      setShowCloseSection(false);
    } catch (err: any) {
      console.error("Close case failed:", err);
      alert("เกิดข้อผิดพลาดในการปิดเคส: " + (err.message || err));
    } finally {
      setIsClosing(false);
    }
  };

  const formatDate = (val: any) => {
    if (!val) return "—";
    try {
      const date = val?.toDate ? val.toDate() : (val?.toMillis ? new Date(val.toMillis()) : (typeof val === "number" ? new Date(val) : new Date(val)));
      if (isNaN(date.getTime())) return "—";
      return new Intl.DateTimeFormat("th-TH", {
        day: "2-digit",
        month: "short",
        year: "numeric",
        hour: "2-digit",
        minute: "2-digit"
      }).format(date);
    } catch {
      return "—";
    }
  };

  return (
    <div className="min-h-screen bg-[#090D16] text-slate-100 font-['Prompt'] flex flex-col selection:bg-orange-500 selection:text-white">
      {/* Mobile App Header */}
      <header className="sticky top-0 z-40 bg-[#0c121e]/95 backdrop-blur-md border-b border-slate-800 px-4 py-3">
        <div className="max-w-md mx-auto flex items-center justify-between">
          <div className="flex items-center space-x-2.5">
            <Logo size={28} />
            <div>
              <h1 className="text-sm font-bold text-slate-100 leading-tight flex items-center gap-1.5">
                <span>WORKLOG</span>
                <span className="text-[10px] px-1.5 py-0.5 rounded bg-orange-500/20 text-orange-400 font-semibold border border-orange-500/30">
                  MOBILE CHECK-IN
                </span>
              </h1>
              <p className="text-[10px] text-slate-400">ระบบบันทึกเช็คอินเข้าหน้างานสำหรับช่าง</p>
            </div>
          </div>

          {onBackToAdmin && (
            <button
              onClick={onBackToAdmin}
              className="text-xs text-slate-400 hover:text-slate-200 px-2.5 py-1 rounded-lg bg-slate-800/80 border border-slate-700 transition-colors"
            >
              แดชบอร์ด
            </button>
          )}
        </div>
      </header>

      {/* Main Content Area */}
      <main className="flex-1 max-w-md w-full mx-auto px-4 py-5 space-y-4 pb-12">
        {isLoading ? (
          <div className="py-24 flex flex-col items-center justify-center space-y-3 text-center">
            <Loader2 className="w-8 h-8 text-orange-500 animate-spin" />
            <p className="text-xs text-slate-400">กำลังโหลดข้อมูลเคส {caseId}...</p>
          </div>
        ) : error || !repairCase ? (
          <div className="bg-slate-900 border border-rose-600/40 rounded-2xl p-6 text-center space-y-3">
            <div className="w-12 h-12 rounded-full bg-rose-500/20 text-rose-400 mx-auto flex items-center justify-center">
              <AlertTriangle className="w-6 h-6" />
            </div>
            <h2 className="text-base font-bold text-slate-200">ไม่พบเคสงานซ่อม</h2>
            <p className="text-xs text-slate-400">{error || `เคส ${caseId} อาจถูกลบหรือไม่มีอยู่ในระบบ`}</p>
            {onBackToAdmin && (
              <button
                onClick={onBackToAdmin}
                className="mt-2 inline-flex items-center space-x-1.5 px-4 py-2 bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs rounded-xl font-medium"
              >
                <ArrowLeft className="w-3.5 h-3.5" />
                <span>กลับสู่ระบบหลัก</span>
              </button>
            )}
          </div>
        ) : (
          <>
            {/* Success Banner */}
            {checkInSuccess && (
              <div className="p-3.5 bg-emerald-500/20 border border-emerald-500/40 rounded-xl flex items-center space-x-2.5 text-emerald-300 animate-fadeIn">
                <CheckCircle2 className="w-5 h-5 shrink-0 text-emerald-400" />
                <div className="text-xs">
                  <p className="font-bold">บันทึก Check-in เรียบร้อยแล้ว!</p>
                  <p className="text-[11px] text-emerald-400/80">ระบบได้อัปเดตข้อมูลและส่งแจ้งเตือนไปยัง Admin แล้ว</p>
                </div>
              </div>
            )}

            {/* Case Header Pill */}
            <div className="bg-slate-900/80 border border-slate-800 rounded-2xl p-4 space-y-3 shadow-lg">
              <div className="flex items-center justify-between">
                <div className="flex items-center space-x-2">
                  <span className="font-mono text-sm font-bold text-orange-400 bg-orange-950/40 px-2.5 py-1 rounded-lg border border-orange-500/30">
                    {repairCase.caseId}
                  </span>
                  <span className={`text-[11px] px-2 py-0.5 rounded-full font-semibold ${
                    repairCase.status === "CLOSED" 
                      ? "bg-emerald-500/20 text-emerald-400 border border-emerald-500/30" 
                      : "bg-blue-500/20 text-blue-400 border border-blue-500/30"
                  }`}>
                    {repairCase.status === "CLOSED" ? "ปิดงานแล้ว" : "รอดำเนินการ"}
                  </span>
                </div>

                <span className={`text-[10px] px-2 py-0.5 rounded-full font-semibold ${
                  repairCase.priority === "ด่วนที่สุด" ? "bg-rose-500/20 text-rose-300 border border-rose-500/30" :
                  repairCase.priority === "ด่วน" ? "bg-amber-500/20 text-amber-300 border border-amber-500/30" :
                  "bg-slate-800 text-slate-300"
                }`}>
                  {repairCase.priority}
                </span>
              </div>

              {/* Customer & Location */}
              <div className="space-y-2 pt-1 border-t border-slate-800">
                <div>
                  <span className="text-[11px] text-slate-400 font-medium">ลูกค้า:</span>
                  <h3 className="text-base font-bold text-slate-100">{repairCase.customerName}</h3>
                </div>

                {repairCase.phone && (
                  <div className="flex items-center justify-between">
                    <span className="text-xs text-slate-300 flex items-center">
                      <Phone className="w-3.5 h-3.5 mr-1.5 text-slate-400" />
                      {repairCase.phone}
                    </span>
                    <a
                      href={`tel:${repairCase.phone}`}
                      className="inline-flex items-center space-x-1 px-3 py-1 bg-indigo-600 hover:bg-indigo-500 text-white rounded-lg text-xs font-medium shadow transition-colors"
                    >
                      <Phone className="w-3 h-3" />
                      <span>โทรออก</span>
                    </a>
                  </div>
                )}

                <div className="space-y-1.5">
                  <div className="flex items-start text-xs text-slate-300">
                    <MapPin className="w-4 h-4 mr-1.5 text-orange-400 shrink-0 mt-0.5" />
                    <span>{repairCase.address}</span>
                  </div>

                  {/* Navigation link */}
                  <a
                    href={
                      repairCase.latitude && repairCase.longitude
                        ? `https://www.google.com/maps?q=${repairCase.latitude},${repairCase.longitude}`
                        : `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(repairCase.address)}`
                    }
                    target="_blank"
                    rel="noopener noreferrer"
                    className="w-full inline-flex items-center justify-center space-x-1.5 py-1.5 bg-slate-800 hover:bg-slate-700 text-slate-200 border border-slate-700 rounded-xl text-xs font-medium transition-colors"
                  >
                    <Navigation className="w-3.5 h-3.5 text-emerald-400" />
                    <span>เปิดนำทาง Google Maps</span>
                    <ExternalLink className="w-3 h-3 opacity-60 ml-0.5" />
                  </a>
                </div>
              </div>

              {/* Details & Technician */}
              <div className="space-y-1.5 pt-2 border-t border-slate-800 text-xs">
                <div className="flex items-center justify-between text-slate-400">
                  <span>ช่างผู้รับผิดชอบ: <strong className="text-slate-200">{repairCase.technicianName}</strong></span>
                  <span>หมวด: <strong className="text-slate-200">{repairCase.category}</strong></span>
                </div>
                <div className="p-2.5 bg-slate-950/60 rounded-xl border border-slate-800/80">
                  <span className="text-[11px] text-slate-400 block mb-0.5">อาการเสียที่แจ้ง:</span>
                  <p className="text-xs text-slate-200 leading-relaxed whitespace-pre-wrap">{repairCase.details}</p>
                </div>
              </div>
            </div>

            {/* Check-In Action Section */}
            <div className="bg-slate-900/80 border border-slate-800 rounded-2xl p-4 space-y-4 shadow-lg">
              <div className="flex items-center justify-between">
                <h3 className="text-xs font-bold uppercase tracking-wider text-slate-300 flex items-center space-x-1.5">
                  <Camera className="w-4 h-4 text-orange-400" />
                  <span>หลักฐานการ Check-in เข้าหน้างาน</span>
                </h3>

                {repairCase.checkInImageUrl || repairCase.checkInAt ? (
                  <span className="inline-flex items-center text-[11px] text-emerald-400 bg-emerald-500/10 px-2 py-0.5 rounded-full font-semibold border border-emerald-500/20">
                    <CheckCircle className="w-3 h-3 mr-1" />
                    เช็คอินแล้ว
                  </span>
                ) : (
                  <span className="inline-flex items-center text-[11px] text-amber-400 bg-amber-500/10 px-2 py-0.5 rounded-full font-semibold border border-amber-500/20">
                    <Clock className="w-3 h-3 mr-1" />
                    รอเช็คอิน
                  </span>
                )}
              </div>

              {/* Already Checked-In State */}
              {repairCase.checkInImageUrl ? (
                <div className="space-y-3 bg-slate-950/50 p-3 rounded-xl border border-slate-800">
                  <div className="flex items-center justify-between text-xs text-slate-300">
                    <span className="flex items-center text-emerald-400 font-semibold">
                      <ShieldCheck className="w-4 h-4 mr-1 text-emerald-400" />
                      เช็คอินเข้าหน้างานเรียบร้อยแล้ว
                    </span>
                    <span className="text-[11px] text-slate-400">
                      {formatDate(repairCase.checkInAt)}
                    </span>
                  </div>

                  {/* Photo thumbnail */}
                  <div 
                    onClick={() => {
                      setLightboxImage(repairCase.checkInImageUrl || "");
                      setLightboxTitle("รูป Check-in เข้าหน้างาน");
                    }}
                    className="relative h-48 rounded-xl overflow-hidden border border-slate-700 cursor-pointer group bg-slate-950"
                  >
                    <img
                      src={repairCase.checkInImageUrl}
                      alt="Check-in Photo"
                      className="w-full h-full object-cover group-hover:scale-105 transition-transform"
                      referrerPolicy="no-referrer"
                    />
                    <div className="absolute inset-0 bg-slate-950/40 opacity-0 group-hover:opacity-100 flex items-center justify-center transition-opacity text-white text-xs font-semibold">
                      คลิกเพื่อซูมดูรูปภาพ
                    </div>
                  </div>

                  <p className="text-[11px] text-slate-400 text-center">
                    ช่างได้เช็คอินเข้าหน้างานและบันทึกเวลาเข้าระบบเรียบร้อยแล้ว
                  </p>
                </div>
              ) : (
                /* Not Checked-In Yet Form */
                <div className="space-y-3.5">
                  <p className="text-xs text-slate-300">
                    เมื่อเดินทางถึงหน้างาน กรุณาถ่ายภาพหรือเลือกรูปถ่ายหน้างานเพื่อยืนยันการเช็คอิน
                  </p>

                  {/* Photo Upload Area */}
                  <input
                    ref={fileInputRef}
                    type="file"
                    accept="image/*"
                    capture="environment"
                    onChange={handleFileChange}
                    className="hidden"
                  />

                  {checkInPreview ? (
                    <div className="relative rounded-xl overflow-hidden border border-orange-500/40 bg-slate-950">
                      <img
                        src={checkInPreview}
                        alt="Preview"
                        className="w-full h-48 object-cover"
                      />
                      <div className="absolute top-2 right-2 flex space-x-1.5">
                        <button
                          type="button"
                          onClick={() => fileInputRef.current?.click()}
                          className="px-2.5 py-1 bg-slate-900/80 hover:bg-slate-900 text-white rounded-lg text-xs backdrop-blur font-medium"
                        >
                          เปลี่ยนรูป
                        </button>
                      </div>
                    </div>
                  ) : (
                    <button
                      type="button"
                      onClick={() => fileInputRef.current?.click()}
                      className="w-full h-36 border-2 border-dashed border-slate-700 hover:border-orange-500/60 rounded-xl bg-slate-950/60 flex flex-col items-center justify-center space-y-2 text-slate-400 hover:text-orange-400 transition-colors cursor-pointer p-4 text-center"
                    >
                      <div className="w-11 h-11 rounded-full bg-orange-500/10 text-orange-400 flex items-center justify-center">
                        <Camera className="w-6 h-6" />
                      </div>
                      <span className="text-xs font-semibold text-slate-200">
                        กดเพื่อถ่ายรูปภาพ หรือเลือกรูปจากเครื่อง
                      </span>
                      <span className="text-[10px] text-slate-500">
                        รองรับกล้องมือถือและไฟล์รูปภาพ JPG, PNG
                      </span>
                    </button>
                  )}

                  {/* GPS Coordinates Button */}
                  <div className="flex items-center justify-between p-2.5 bg-slate-950/60 rounded-xl border border-slate-800 text-xs">
                    <div className="flex items-center space-x-2">
                      <MapPin className="w-4 h-4 text-slate-400" />
                      <div>
                        <span className="block text-[11px] text-slate-300">พิกัดสถานที่ปัจจุบัน:</span>
                        <span className="text-[10px] text-slate-400">
                          {gpsLocation 
                            ? `${gpsLocation.lat.toFixed(5)}, ${gpsLocation.lng.toFixed(5)}` 
                            : "ยังไม่ได้ระบุพิกัด"}
                        </span>
                      </div>
                    </div>
                    <button
                      type="button"
                      onClick={handleGetLocation}
                      disabled={isLocating}
                      className="px-2.5 py-1 bg-slate-800 hover:bg-slate-700 text-slate-200 rounded-lg text-[11px] font-medium transition-colors"
                    >
                      {isLocating ? "กำลังค้นหา..." : gpsLocation ? "อัปเดตพิกัด" : "จับพิกัด GPS"}
                    </button>
                  </div>
                  {locationError && (
                    <p className="text-[11px] text-amber-400">{locationError}</p>
                  )}

                  {/* Submit Check-In Button */}
                  <button
                    type="button"
                    onClick={handleSubmitCheckIn}
                    disabled={isSubmitting || (!checkInFile && !checkInPreview)}
                    className="w-full py-3 bg-gradient-to-r from-orange-600 to-amber-600 hover:from-orange-500 hover:to-amber-500 text-white rounded-xl text-sm font-bold shadow-lg shadow-orange-600/30 flex items-center justify-center space-x-2 disabled:opacity-50 disabled:cursor-not-allowed transition-all"
                  >
                    {isSubmitting ? (
                      <>
                        <Loader2 className="w-4 h-4 animate-spin" />
                        <span>กำลังบันทึก Check-in...</span>
                      </>
                    ) : (
                      <>
                        <CheckCircle2 className="w-4 h-4" />
                        <span>ยืนยัน Check-in เข้าหน้างาน</span>
                      </>
                    )}
                  </button>
                </div>
              )}
            </div>

            {/* Mobile Close Case Section (If checked in and still open) */}
            {repairCase.status === "OPEN" && repairCase.checkInImageUrl && (
              <div className="bg-slate-900/80 border border-slate-800 rounded-2xl p-4 space-y-3 shadow-lg">
                <div className="flex items-center justify-between">
                  <h3 className="text-xs font-bold uppercase tracking-wider text-slate-300 flex items-center space-x-1.5">
                    <FileCheck className="w-4 h-4 text-emerald-400" />
                    <span>ปิดงานซ่อมแซม (Check-out)</span>
                  </h3>
                  {!showCloseSection && (
                    <button
                      type="button"
                      onClick={() => setShowCloseSection(true)}
                      className="px-2.5 py-1 bg-emerald-600 hover:bg-emerald-500 text-white rounded-lg text-xs font-semibold"
                    >
                      บันทึกปิดงาน
                    </button>
                  )}
                </div>

                {showCloseSection && (
                  <div className="space-y-3 pt-2">
                    <div>
                      <label className="text-[11px] text-slate-300 block mb-1">
                        สรุปวิธีการแก้ไขปัญหา / ผลการซ่อม *
                      </label>
                      <textarea
                        rows={3}
                        placeholder="เช่น เปลี่ยนอะไหล่และทดสอบการทำงานเรียบร้อย..."
                        value={closeSolution}
                        onChange={(e) => setCloseSolution(e.target.value)}
                        className="w-full p-2.5 bg-slate-950 border border-slate-700 rounded-xl text-xs text-slate-100 placeholder-slate-500 focus:border-emerald-500 outline-none"
                      />
                    </div>

                    <div>
                      <label className="text-[11px] text-slate-300 block mb-1">
                        รูปภาพหลังซ่อมเสร็จ (ปิดงาน)
                      </label>
                      <input
                        ref={closeFileInputRef}
                        type="file"
                        accept="image/*"
                        capture="environment"
                        onChange={handleCloseFileChange}
                        className="hidden"
                      />
                      {closePreview ? (
                        <div className="relative rounded-xl overflow-hidden border border-emerald-500/40">
                          <img src={closePreview} alt="Close Preview" className="w-full h-36 object-cover" />
                          <button
                            type="button"
                            onClick={() => closeFileInputRef.current?.click()}
                            className="absolute top-2 right-2 px-2 py-1 bg-slate-900/80 text-white text-[10px] rounded"
                          >
                            เปลี่ยนรูป
                          </button>
                        </div>
                      ) : (
                        <button
                          type="button"
                          onClick={() => closeFileInputRef.current?.click()}
                          className="w-full py-2.5 border border-dashed border-slate-700 rounded-xl bg-slate-950/60 text-xs text-slate-400 hover:text-emerald-400 flex items-center justify-center space-x-1.5"
                        >
                          <Camera className="w-4 h-4" />
                          <span>ถ่ายรูปผลงานหลังซ่อม</span>
                        </button>
                      )}
                    </div>

                    <div className="flex space-x-2 pt-1">
                      <button
                        type="button"
                        onClick={() => setShowCloseSection(false)}
                        className="flex-1 py-2 bg-slate-800 text-slate-300 rounded-xl text-xs"
                      >
                        ยกเลิก
                      </button>
                      <button
                        type="button"
                        onClick={handleSubmitCloseCase}
                        disabled={isClosing || !closeSolution.trim()}
                        className="flex-1 py-2 bg-emerald-600 hover:bg-emerald-500 text-white rounded-xl text-xs font-bold shadow disabled:opacity-50"
                      >
                        {isClosing ? "กำลังบันทึก..." : "ยืนยันปิดงาน"}
                      </button>
                    </div>
                  </div>
                )}
              </div>
            )}

            {/* Case Completed Banner */}
            {repairCase.status === "CLOSED" && (
              <div className="p-4 bg-emerald-950/40 border border-emerald-500/40 rounded-2xl space-y-2">
                <div className="flex items-center space-x-2 text-emerald-400 text-xs font-bold">
                  <CheckCircle2 className="w-4 h-4" />
                  <span>งานซ่อมนี้ปิดงานเสร็จสิ้นแล้ว</span>
                </div>
                {repairCase.solutions && (
                  <p className="text-xs text-slate-200 whitespace-pre-wrap">{repairCase.solutions}</p>
                )}
                {repairCase.checkOutImageUrl && (
                  <div 
                    onClick={() => {
                      setLightboxImage(repairCase.checkOutImageUrl || "");
                      setLightboxTitle("รูปปิดงานซ่อมเสร็จสมบูรณ์");
                    }}
                    className="h-32 rounded-xl overflow-hidden border border-slate-700 cursor-pointer"
                  >
                    <img src={repairCase.checkOutImageUrl} alt="Check-out" className="w-full h-full object-cover" />
                  </div>
                )}
              </div>
            )}
          </>
        )}
      </main>

      {/* Lightbox */}
      <ImageLightbox
        isOpen={!!lightboxImage}
        imageUrl={lightboxImage || ""}
        title={lightboxTitle}
        onClose={() => setLightboxImage(null)}
      />
    </div>
  );
};
