import React, { useState } from "react";
import { 
  X, 
  PlusCircle, 
  Upload, 
  User, 
  Phone, 
  MapPin, 
  AlertTriangle, 
  Wrench, 
  Check, 
  FileText, 
  Image as ImageIcon 
} from "lucide-react";
import { TechUser, CasePriority, REPAIR_CATEGORIES } from "../types";
import { createNewCase } from "../services/caseService";

interface CreateCaseModalProps {
  isOpen: boolean;
  onClose: () => void;
  technicians: TechUser[];
  onCaseCreated?: (caseId: string) => void;
}

export const CreateCaseModal: React.FC<CreateCaseModalProps> = ({
  isOpen,
  onClose,
  technicians,
  onCaseCreated
}) => {
  const [customerName, setCustomerName] = useState("");
  const [phone, setPhone] = useState("");
  const [address, setAddress] = useState("");
  const [category, setCategory] = useState<string>(REPAIR_CATEGORIES[0]);
  const [priority, setPriority] = useState<CasePriority>("ปกติ");
  const [details, setDetails] = useState("");
  const [selectedTechId, setSelectedTechId] = useState<string>(technicians[0]?.id || "");
  
  React.useEffect(() => {
    if (!selectedTechId && technicians.length > 0) {
      setSelectedTechId(technicians[0].id);
    }
  }, [technicians, selectedTechId]);
  
  // Image states
  const [imageFile, setImageFile] = useState<File | null>(null);
  const [imagePreview, setImagePreview] = useState<string>("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMsg, setErrorMsg] = useState("");

  if (!isOpen) return null;

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0]) {
      const file = e.target.files[0];
      setImageFile(file);
      const reader = new FileReader();
      reader.onloadend = () => {
        setImagePreview(reader.result as string);
      };
      reader.readAsDataURL(file);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMsg("");

    if (!customerName.trim()) {
      setErrorMsg("กรุณากรอกชื่อลูกค้า / หน่วยงาน");
      return;
    }
    if (!phone.trim()) {
      setErrorMsg("กรุณากรอกเบอร์โทรศัพท์ติดต่อ");
      return;
    }
    if (!address.trim()) {
      setErrorMsg("กรุณากรอกสถานที่ / แผนก / ที่อยู่ Onsite");
      return;
    }
    if (!details.trim()) {
      setErrorMsg("กรุณากรอกรายละเอียดงานหรือปัญหาที่แจ้ง");
      return;
    }

    const tech = technicians.find(t => t.id === selectedTechId);
    if (!tech) {
      setErrorMsg("กรุณาเลือกช่างผู้รับผิดชอบ");
      return;
    }

    setIsSubmitting(true);
    try {
      const createdCaseId = await createNewCase({
        customerName,
        phone,
        address,
        details,
        category,
        priority,
        technicianId: tech.id,
        technicianName: tech.name,
        imageFile: imageFile,
        imageUrl: imagePreview
      });

      // Reset form
      setCustomerName("");
      setPhone("");
      setAddress("");
      setDetails("");
      setImageFile(null);
      setImagePreview("");
      
      if (onCaseCreated) {
        onCaseCreated(createdCaseId);
      }
      onClose();
    } catch (err: any) {
      console.error("Error creating case:", err);
      setErrorMsg(err.message || "เกิดข้อผิดพลาดในการเปิดเคส");
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div 
      id="create-case-backdrop"
      className="fixed inset-0 z-50 bg-slate-950/80 backdrop-blur-sm flex items-center justify-center p-4 overflow-y-auto font-['Prompt']"
      onClick={onClose}
    >
      <div 
        id="create-case-modal"
        className="relative w-full max-w-2xl bg-slate-900 border border-slate-800 text-slate-100 rounded-2xl shadow-2xl overflow-hidden my-8"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-slate-800 bg-slate-900/90">
          <div className="flex items-center space-x-3">
            <div className="w-9 h-9 rounded-xl bg-orange-500/10 text-orange-400 flex items-center justify-center border border-orange-500/20">
              <PlusCircle className="w-5 h-5" />
            </div>
            <div>
              <h2 className="text-base font-bold text-slate-100">เปิดเคสงาน IT Support Onsite & มอบหมายงาน (Dispatch)</h2>
              <p className="text-xs text-slate-400">บันทึกข้อมูลเคสลง Firestore Database และแจ้งเตือนทีมช่างทันที</p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-1.5 text-slate-400 hover:text-slate-100 hover:bg-slate-800 rounded-lg transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Error Alert */}
        {errorMsg && (
          <div className="mx-6 mt-4 p-3 bg-rose-950/80 border border-rose-600/40 rounded-xl text-xs text-rose-300 flex items-center space-x-2">
            <AlertTriangle className="w-4 h-4 text-rose-400 shrink-0" />
            <span>{errorMsg}</span>
          </div>
        )}

        {/* Form Body */}
        <form onSubmit={handleSubmit} className="p-6 space-y-4 max-h-[75vh] overflow-y-auto">
          {/* Row 1: Customer Info */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold text-slate-300 mb-1.5 flex items-center">
                <User className="w-3.5 h-3.5 mr-1 text-orange-400" /> ชื่อลูกค้า / ผู้ติดต่อ / แผนก *
              </label>
              <input
                type="text"
                required
                placeholder="เช่น บจก. เอบีซี / คุณสมชาย"
                value={customerName}
                onChange={(e) => setCustomerName(e.target.value)}
                className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-xl text-xs text-slate-100 placeholder-slate-500 focus:outline-none focus:border-orange-500"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-300 mb-1.5 flex items-center">
                <Phone className="w-3.5 h-3.5 mr-1 text-orange-400" /> เบอร์โทรศัพท์ *
              </label>
              <input
                type="tel"
                required
                placeholder="เช่น 081-234-5678"
                value={phone}
                onChange={(e) => setPhone(e.target.value)}
                className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-xl text-xs text-slate-100 placeholder-slate-500 focus:outline-none focus:border-orange-500"
              />
            </div>
          </div>

          {/* Row 2: Location / Address */}
          <div>
            <label className="block text-xs font-semibold text-slate-300 mb-1.5 flex items-center">
              <MapPin className="w-3.5 h-3.5 mr-1 text-orange-400" /> สถานที่ Onsite / อาคาร / ห้อง *
            </label>
            <input
              type="text"
              required
              placeholder="เลขที่, อาคาร, ชั้น, แผนก หรือที่อยู่สำหรับเข้าหน้างาน"
              value={address}
              onChange={(e) => setAddress(e.target.value)}
              className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-xl text-xs text-slate-100 placeholder-slate-500 focus:outline-none focus:border-orange-500"
            />
          </div>

          {/* Row 3: Category */}
          <div>
            <label className="block text-xs font-semibold text-slate-300 mb-1.5 flex items-center">
              <Wrench className="w-3.5 h-3.5 mr-1 text-orange-400" /> หมวดหมู่งาน (Category) *
            </label>
            <select
              value={category}
              onChange={(e) => setCategory(e.target.value)}
              className="w-full px-3 py-2.5 bg-slate-950 border border-slate-800 rounded-xl text-xs text-slate-100 focus:outline-none focus:border-orange-500 font-medium"
            >
              {REPAIR_CATEGORIES.map((cat) => (
                <option key={cat} value={cat}>{cat}</option>
              ))}
            </select>
          </div>

          {/* Row 4: Technician Assignment */}
          <div>
            <label className="block text-xs font-semibold text-slate-300 mb-1.5 flex items-center justify-between">
              <span className="flex items-center">
                <User className="w-3.5 h-3.5 mr-1 text-orange-400" /> มอบหมายช่าง / เจ้าหน้าที่ Onsite *
              </span>
              <span className="text-[11px] text-slate-400 font-normal">ระบบจะส่งการแจ้งเตือนไปยังช่างทันที</span>
            </label>
            <select
              value={selectedTechId}
              onChange={(e) => setSelectedTechId(e.target.value)}
              className="w-full px-3 py-2.5 bg-slate-950 border border-slate-800 rounded-xl text-xs text-slate-100 focus:outline-none focus:border-orange-500 font-medium"
            >
              {technicians.length === 0 ? (
                <option value="">ไม่มีรายชื่อช่าง (กรุณาเพิ่มช่างในแท็บจัดการช่างเทคนิคก่อน)</option>
              ) : (
                technicians.map((t) => (
                  <option key={t.id} value={t.id}>
                    {t.name} ({t.id}) {t.isActive ? "• พร้อมรับงาน" : "• ปิดรับงาน"}
                  </option>
                ))
              )}
            </select>
          </div>

          {/* Row 5: Symptoms Details */}
          <div>
            <label className="block text-xs font-semibold text-slate-300 mb-1.5 flex items-center">
              <FileText className="w-3.5 h-3.5 mr-1 text-orange-400" /> รายละเอียดงาน / ปัญหา / อุปกรณ์ *
            </label>
            <textarea
              required
              rows={3}
              placeholder="ระบุรายละเอียดงาน IT Support เช่น ติดตั้ง Switch, ซ่อมบำรุง PC, ส่งมอบอุปกรณ์, หรือปัญหาที่พบ..."
              value={details}
              onChange={(e) => setDetails(e.target.value)}
              className="w-full p-3 bg-slate-950 border border-slate-800 rounded-xl text-xs text-slate-100 placeholder-slate-500 focus:outline-none focus:border-orange-500 leading-relaxed"
            />
          </div>

          {/* Row 6: Image Attachment / Camera Upload */}
          <div>
            <label className="block text-xs font-semibold text-slate-300 mb-1.5 flex items-center justify-between">
              <span className="flex items-center">
                <ImageIcon className="w-3.5 h-3.5 mr-1 text-orange-400" /> แนบรูปภาพหน้างาน / อุปกรณ์ (ถ้ามี)
              </span>
              {imagePreview && (
                <button
                  type="button"
                  onClick={() => { setImageFile(null); setImagePreview(""); }}
                  className="text-[11px] text-rose-400 hover:underline"
                >
                  ลบรูปภาพ
                </button>
              )}
            </label>

            {imagePreview ? (
              <div className="relative rounded-xl border border-slate-800 overflow-hidden bg-slate-950 p-2 flex items-center space-x-3">
                <img 
                  src={imagePreview} 
                  alt="Issue Preview" 
                  className="w-20 h-20 object-cover rounded-lg border border-slate-800" 
                  referrerPolicy="no-referrer"
                />
                <div className="text-xs text-slate-300">
                  <p className="font-semibold text-emerald-400 flex items-center">
                    <Check className="w-3.5 h-3.5 mr-1" /> รูปภาพพร้อมอัปโหลด
                  </p>
                  <p className="text-[11px] text-slate-400 mt-0.5">ภาพจะถูกบันทึกลงในระบบของเคสงาน</p>
                </div>
              </div>
            ) : (
              <label className="flex flex-col items-center justify-center p-4 border-2 border-dashed border-slate-800 hover:border-orange-500/50 bg-slate-950/60 rounded-xl cursor-pointer transition-colors group">
                <Upload className="w-6 h-6 text-slate-400 group-hover:text-orange-400 mb-1" />
                <span className="text-xs text-slate-300 group-hover:text-orange-200 font-medium">คลิกเพื่ออัปโหลดรูปภาพ หรือลากไฟล์มาวาง</span>
                <span className="text-[10px] text-slate-500 mt-0.5">รองรับ JPG, PNG, WEBP</span>
                <input type="file" accept="image/*" onChange={handleFileChange} className="hidden" />
              </label>
            )}
          </div>

          {/* Footer Submit Actions */}
          <div className="pt-4 border-t border-slate-800 flex items-center justify-end space-x-3">
            <button
              type="button"
              onClick={onClose}
              disabled={isSubmitting}
              className="px-4 py-2 bg-slate-800 hover:bg-slate-700 text-slate-300 rounded-xl text-xs font-semibold transition-colors"
            >
              ยกเลิก
            </button>
            <button
              type="submit"
              disabled={isSubmitting}
              className="px-6 py-2 bg-orange-500 hover:bg-orange-600 text-white rounded-xl text-xs font-semibold flex items-center space-x-2 transition-all shadow-lg shadow-orange-500/20 disabled:opacity-50"
            >
              <PlusCircle className="w-4 h-4" />
              <span>{isSubmitting ? "กำลังบันทึกและจ่ายงาน..." : "บันทึก & จ่ายงานทันที (Dispatch)"}</span>
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
