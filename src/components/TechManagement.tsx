import React, { useState } from "react";
import { 
  Users, 
  UserPlus, 
  Phone, 
  Mail, 
  Key, 
  Eye, 
  EyeOff, 
  ShieldCheck, 
  CheckCircle, 
  XCircle, 
  Edit, 
  Trash2, 
  Wrench,
  Search,
  Plus
} from "lucide-react";
import { TechUser, REPAIR_CATEGORIES } from "../types";
import { addTechnician, updateTechnician, toggleTechStatus, deleteTechnician } from "../services/techService";

interface TechManagementProps {
  technicians: TechUser[];
  activeCasesCountMap: Record<string, number>;
}

export const TechManagement: React.FC<TechManagementProps> = ({
  technicians,
  activeCasesCountMap
}) => {
  const [searchQuery, setSearchQuery] = useState("");
  const [visiblePins, setVisiblePins] = useState<Record<string, boolean>>({});

  // Modal State for Add / Edit Tech
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingTech, setEditingTech] = useState<TechUser | null>(null);

  // Form fields
  const [name, setName] = useState("");
  const [phone, setPhone] = useState("");
  const [email, setEmail] = useState("");
  const [pin, setPin] = useState("");
  const [skills, setSkills] = useState<string[]>([]);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const togglePinVisibility = (techId: string) => {
    setVisiblePins(prev => ({ ...prev, [techId]: !prev[techId] }));
  };

  const handleOpenAddModal = () => {
    setEditingTech(null);
    setName("");
    setPhone("");
    setEmail("");
    // Generate a random 6 digit PIN
    const randomPin = String(Math.floor(100000 + Math.random() * 900000));
    setPin(randomPin);
    setSkills(["งานทั่วไป"]);
    setIsModalOpen(true);
  };

  const handleOpenEditModal = (tech: TechUser) => {
    setEditingTech(tech);
    setName(tech.name);
    setPhone(tech.phone);
    setEmail(tech.email || "");
    setPin(tech.pin || "123456");
    setSkills(tech.skills || []);
    setIsModalOpen(true);
  };

  const handleToggleSkill = (skill: string) => {
    if (skills.includes(skill)) {
      setSkills(skills.filter(s => s !== skill));
    } else {
      setSkills([...skills, skill]);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim() || !phone.trim() || !pin.trim()) {
      alert("กรุณากรอกข้อมูลสำคัญให้ครบถ้วน (ชื่อ, เบอร์โทร, PIN 6 หลัก)");
      return;
    }

    if (pin.length !== 6 || !/^\d+$/.test(pin)) {
      alert("รหัส PIN ต้องเป็นตัวเลข 6 หลักเท่านั้น");
      return;
    }

    setIsSubmitting(true);
    try {
      if (editingTech) {
        // Update existing
        await updateTechnician(editingTech.id, {
          name: name.trim(),
          phone: phone.trim(),
          email: email.trim(),
          pin: pin.trim(),
          skills: skills
        });
      } else {
        // Generate new Tech ID: TECH-00X
        const nextNum = String(technicians.length + 1).padStart(3, '0');
        const newTechId = `TECH-${nextNum}`;
        await addTechnician({
          id: newTechId,
          name: name.trim(),
          phone: phone.trim(),
          email: email.trim() || `${newTechId.toLowerCase()}@repair.com`,
          pin: pin.trim(),
          role: "technician",
          isActive: true,
          skills: skills
        });
      }
      setIsModalOpen(false);
    } catch (err) {
      console.error(err);
      alert("เกิดข้อผิดพลาดในการบันทึกข้อมูลช่าง");
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleDelete = async (tech: TechUser) => {
    if (!confirm(`คุณต้องการลบช่าง "${tech.name}" (${tech.id}) ออกจากระบบหรือไม่?`)) return;
    try {
      await deleteTechnician(tech.id, tech.name);
    } catch (e) {
      console.error(e);
    }
  };

  const filteredTechs = technicians.filter(t => 
    t.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
    t.id.toLowerCase().includes(searchQuery.toLowerCase()) ||
    t.phone.includes(searchQuery)
  );

  return (
    <div id="tech-management-view" className="space-y-6">
      {/* Header & Controls */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-lg font-bold text-slate-100 flex items-center">
            <Users className="w-5 h-5 mr-2 text-indigo-400" />
            จัดการรายชื่อและบัญชีช่าง (Technician Roster & Auth)
          </h2>
          <p className="text-xs text-slate-400 mt-0.5">
            กำหนดสิทธิ์การจ่ายงาน และรหัส PIN 6 หลักสำหรับช่างใช้ล็อกอินผ่านแอป Android
          </p>
        </div>

        <div className="flex items-center space-x-3">
          <div className="relative">
            <Search className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
            <input
              type="text"
              placeholder="ค้นหาชื่อ, รหัส, เบอร์โทร..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="pl-9 pr-3 py-2 bg-slate-900 border border-slate-800 rounded-xl text-xs text-slate-200 placeholder-slate-500 focus:outline-none focus:border-indigo-500 w-48 sm:w-64"
            />
          </div>

          <button
            onClick={handleOpenAddModal}
            className="px-4 py-2 bg-indigo-600 hover:bg-indigo-500 text-white rounded-xl text-xs font-semibold flex items-center space-x-1.5 transition-all shadow-md shadow-indigo-600/30 shrink-0"
          >
            <UserPlus className="w-4 h-4" />
            <span>เพิ่มช่างใหม่</span>
          </button>
        </div>
      </div>

      {/* Grid of Technician Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {filteredTechs.map((tech) => {
          const activeJobs = activeCasesCountMap[tech.id] || 0;
          const showPin = visiblePins[tech.id];

          return (
            <div
              key={tech.id}
              className={`bg-slate-900/90 border rounded-2xl p-5 space-y-4 shadow-lg transition-all ${
                tech.isActive ? "border-slate-800 hover:border-indigo-500/50" : "border-slate-800/60 opacity-70 bg-slate-950/60"
              }`}
            >
              {/* Top Row: Info & Status */}
              <div className="flex items-start justify-between">
                <div className="flex items-center space-x-3">
                  <div className={`w-11 h-11 rounded-2xl flex items-center justify-center font-bold text-sm border ${
                    tech.isActive 
                      ? "bg-indigo-600/20 text-indigo-400 border-indigo-500/30" 
                      : "bg-slate-800 text-slate-400 border-slate-700"
                  }`}>
                    {tech.name.substring(0, 2)}
                  </div>
                  <div>
                    <h3 className="font-bold text-sm text-slate-100">{tech.name}</h3>
                    <span className="font-mono text-[11px] text-indigo-400 font-semibold">{tech.id}</span>
                  </div>
                </div>

                {/* Active Toggle Switch */}
                <button
                  onClick={() => toggleTechStatus(tech.id, tech.isActive, tech.name)}
                  title={tech.isActive ? "กดเพื่อปิดรับงานชั่วคราว" : "กดเพื่อเปิดสถานะพร้อมรับงาน"}
                  className={`px-2.5 py-1 rounded-full text-[11px] font-semibold flex items-center space-x-1 border transition-colors ${
                    tech.isActive
                      ? "bg-emerald-500/20 text-emerald-300 border-emerald-500/40"
                      : "bg-slate-800 text-slate-400 border-slate-700"
                  }`}
                >
                  {tech.isActive ? (
                    <>
                      <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-pulse"></span>
                      <span>พร้อมรับงาน</span>
                    </>
                  ) : (
                    <>
                      <XCircle className="w-3 h-3" />
                      <span>พักงาน</span>
                    </>
                  )}
                </button>
              </div>

              {/* Contact & PIN */}
              <div className="bg-slate-950/50 rounded-xl p-3 border border-slate-800/80 space-y-2 text-xs">
                <div className="flex items-center justify-between text-slate-300">
                  <span className="flex items-center text-slate-400">
                    <Phone className="w-3.5 h-3.5 mr-1.5 text-indigo-400" />
                    {tech.phone}
                  </span>
                  <a href={`tel:${tech.phone}`} className="text-indigo-400 hover:underline text-[11px]">โทรด่วน</a>
                </div>

                <div className="flex items-center justify-between text-slate-300">
                  <span className="flex items-center text-slate-400">
                    <Key className="w-3.5 h-3.5 mr-1.5 text-amber-400" />
                    PIN มือถือ:
                  </span>
                  <div className="flex items-center space-x-1.5 font-mono font-bold text-amber-300 bg-slate-900 px-2 py-0.5 rounded border border-slate-800">
                    <span>{showPin ? tech.pin : "••••••"}</span>
                    <button
                      onClick={() => togglePinVisibility(tech.id)}
                      className="text-slate-400 hover:text-slate-200"
                      title={showPin ? "ซ่อน PIN" : "แสดง PIN"}
                    >
                      {showPin ? <EyeOff className="w-3 h-3" /> : <Eye className="w-3 h-3" />}
                    </button>
                  </div>
                </div>
              </div>

              {/* Skills Tags */}
              <div className="space-y-1.5">
                <span className="text-[10px] font-semibold text-slate-400 uppercase tracking-wider block">ความเชี่ยวชาญ / ทักษะ:</span>
                <div className="flex flex-wrap gap-1.5">
                  {(tech.skills || ["งานซ่อมทั่วไป"]).map((sk, idx) => (
                    <span
                      key={idx}
                      className="px-2 py-0.5 bg-slate-800 border border-slate-700/70 text-slate-300 rounded-lg text-[10px]"
                    >
                      {sk}
                    </span>
                  ))}
                </div>
              </div>

              {/* Active Jobs Counter & Card Actions */}
              <div className="pt-2 border-t border-slate-800 flex items-center justify-between">
                <div className="text-xs">
                  <span className="text-slate-400">งานค้างรับผิดชอบ: </span>
                  <span className={`font-bold ${activeJobs > 0 ? "text-amber-400" : "text-slate-300"}`}>
                    {activeJobs} เคส
                  </span>
                </div>

                <div className="flex items-center space-x-1">
                  <button
                    onClick={() => handleOpenEditModal(tech)}
                    className="p-1.5 text-slate-400 hover:text-slate-200 hover:bg-slate-800 rounded-lg transition-colors"
                    title="แก้ไขข้อมูลช่าง"
                  >
                    <Edit className="w-4 h-4" />
                  </button>
                  <button
                    onClick={() => handleDelete(tech)}
                    className="p-1.5 text-slate-400 hover:text-rose-400 hover:bg-rose-950/30 rounded-lg transition-colors"
                    title="ลบช่าง"
                  >
                    <Trash2 className="w-4 h-4" />
                  </button>
                </div>
              </div>
            </div>
          );
        })}
      </div>

      {/* Add / Edit Technician Modal */}
      {isModalOpen && (
        <div className="fixed inset-0 z-50 bg-slate-950/80 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-slate-900 border border-slate-800 rounded-2xl max-w-md w-full p-6 text-slate-100 shadow-2xl space-y-4">
            <div className="flex items-center justify-between border-b border-slate-800 pb-3">
              <h3 className="text-base font-bold flex items-center">
                <UserPlus className="w-5 h-5 mr-2 text-indigo-400" />
                {editingTech ? "แก้ไขข้อมูลช่าง" : "เพิ่มช่างซ่อมคนใหม่"}
              </h3>
              <button onClick={() => setIsModalOpen(false)} className="text-slate-400 hover:text-white">
                <XCircle className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleSubmit} className="space-y-3.5">
              <div>
                <label className="text-xs font-medium text-slate-300 block mb-1">ชื่อ-นามสกุลช่าง *</label>
                <input
                  type="text"
                  required
                  placeholder="เช่น ช่างสมหมาย ใจดี"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  className="w-full px-3 py-2 bg-slate-800 border border-slate-700 rounded-xl text-xs text-slate-100 focus:outline-none focus:border-indigo-500"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="text-xs font-medium text-slate-300 block mb-1">เบอร์โทรศัพท์ *</label>
                  <input
                    type="tel"
                    required
                    placeholder="08x-xxx-xxxx"
                    value={phone}
                    onChange={(e) => setPhone(e.target.value)}
                    className="w-full px-3 py-2 bg-slate-800 border border-slate-700 rounded-xl text-xs text-slate-100 focus:outline-none focus:border-indigo-500"
                  />
                </div>

                <div>
                  <label className="text-xs font-medium text-slate-300 block mb-1">PIN 6 หลัก (Android) *</label>
                  <input
                    type="text"
                    maxLength={6}
                    required
                    placeholder="123456"
                    value={pin}
                    onChange={(e) => setPin(e.target.value)}
                    className="w-full px-3 py-2 bg-slate-800 border border-slate-700 rounded-xl text-xs font-mono font-bold tracking-wider text-amber-300 text-center focus:outline-none focus:border-amber-500"
                  />
                </div>
              </div>

              <div>
                <label className="text-xs font-medium text-slate-300 block mb-1">อีเมล</label>
                <input
                  type="email"
                  placeholder="tech@repair.com"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  className="w-full px-3 py-2 bg-slate-800 border border-slate-700 rounded-xl text-xs text-slate-100 focus:outline-none focus:border-indigo-500"
                />
              </div>

              <div>
                <label className="text-xs font-medium text-slate-300 block mb-1.5">หมวดหมู่ความเชี่ยวชาญ:</label>
                <div className="grid grid-cols-2 gap-1.5 max-h-32 overflow-y-auto p-1 bg-slate-950/40 rounded-xl border border-slate-800">
                  {REPAIR_CATEGORIES.map((cat) => (
                    <button
                      type="button"
                      key={cat}
                      onClick={() => handleToggleSkill(cat)}
                      className={`text-left px-2 py-1 rounded text-[11px] truncate transition-colors ${
                        skills.includes(cat)
                          ? "bg-indigo-600/30 text-indigo-300 border border-indigo-500/40"
                          : "text-slate-400 hover:text-slate-200"
                      }`}
                    >
                      {skills.includes(cat) ? "✓ " : "+ "}
                      {cat}
                    </button>
                  ))}
                </div>
              </div>

              <div className="pt-3 border-t border-slate-800 flex justify-end space-x-2">
                <button
                  type="button"
                  onClick={() => setIsModalOpen(false)}
                  className="px-4 py-2 bg-slate-800 text-slate-300 rounded-xl text-xs font-medium"
                >
                  ยกเลิก
                </button>
                <button
                  type="submit"
                  disabled={isSubmitting}
                  className="px-5 py-2 bg-indigo-600 hover:bg-indigo-500 text-white rounded-xl text-xs font-semibold shadow-md shadow-indigo-600/30"
                >
                  {isSubmitting ? "กำลังบันทึก..." : editingTech ? "บันทึกการแก้ไข" : "เพิ่มช่างใหม่"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
