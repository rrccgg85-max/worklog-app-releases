import React, { useState } from "react";
import { auth, db } from "../firebase";
import { signInWithEmailAndPassword } from "firebase/auth";
import { doc, getDoc } from "firebase/firestore";
import { ShieldAlert, Lock, Mail, Loader2, ArrowRight, CheckCircle2, Eye, EyeOff } from "lucide-react";
import { registerAdminSession } from "../services/sessionService";

interface LoginScreenProps {
  onLoginSuccess: (adminEmail: string) => void;
}

export const LoginScreen: React.FC<LoginScreenProps> = ({ onLoginSuccess }) => {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(false);

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    if (!email.trim() || !password.trim()) {
      setError("กรุณากรอก Email และ Password ให้ครบถ้วน");
      return;
    }

    setIsLoading(true);

    try {
      // Authenticate with Firebase Auth
      const userCredential = await signInWithEmailAndPassword(auth, email.trim(), password);
      const user = userCredential.user;

      // Check if user is an admin by trying to read their document in "admins" collection
      const adminDocRef = doc(db, "admins", user.uid);
      const adminDocSnap = await getDoc(adminDocRef);

      if (adminDocSnap.exists()) {
        // User is an admin
        await registerAdminSession(user.email || email.trim().toLowerCase());
        onLoginSuccess(user.email || email.trim().toLowerCase());
      } else {
        // User authenticated but is not an admin (e.g. technician)
        // Sign out to prevent unauthorized access
        await auth.signOut();
        setError("บัญชีนี้ไม่มีสิทธิ์การเข้าถึงระดับผู้ดูแลระบบ (Admin)");
      }
    } catch (err: any) {
      console.error("Login verification error:", err);
      // Determine error message
      let errorMessage = "เกิดข้อผิดพลาดในการเข้าสู่ระบบ";
      if (err.code === "auth/invalid-credential" || err.code === "auth/user-not-found" || err.code === "auth/wrong-password") {
        errorMessage = "อีเมลหรือรหัสผ่านไม่ถูกต้อง";
      } else if (err.code === "auth/too-many-requests") {
        errorMessage = "พยายามเข้าสู่ระบบบ่อยเกินไป กรุณารอสักครู่";
      }
      
      setError(errorMessage);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-slate-950 flex flex-col items-center justify-center p-4 sm:p-6 relative overflow-hidden">
      {/* Background ambient lighting */}
      <div className="absolute top-1/4 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[500px] h-[500px] bg-orange-600/10 rounded-full blur-3xl pointer-events-none" />
      
      <div className="w-full max-w-md relative z-10">
        {/* Header / Logo branding */}
        <div className="text-center mb-8">
          <div className="inline-flex items-center justify-center w-16 h-16 rounded-2xl bg-orange-600/20 border border-orange-500/30 text-orange-500 mb-4 shadow-xl shadow-orange-950/50">
            <ShieldAlert className="w-8 h-8" />
          </div>
          <h1 className="text-2xl sm:text-3xl font-extrabold text-slate-100 tracking-tight">
            WorkLog Admin Portal
          </h1>
          <p className="text-sm text-slate-400 mt-2">
            ระบบจัดการงานซ่อมและบริการภาคสนาม (Secure Admin Access)
          </p>
        </div>

        {/* Login Card */}
        <div className="bg-slate-900/90 border border-slate-800 rounded-3xl p-6 sm:p-8 shadow-2xl backdrop-blur-xl">
          <form onSubmit={handleLogin} autoComplete="off" data-lpignore="true" data-form-type="other" className="space-y-5">
            {error && (
              <div className="p-3.5 rounded-2xl bg-rose-950/60 border border-rose-500/40 text-rose-300 text-xs flex items-center space-x-2.5 animate-shake">
                <span className="w-2 h-2 rounded-full bg-rose-500 shrink-0" />
                <span>{error}</span>
              </div>
            )}

            <div>
              <label className="block text-xs font-bold uppercase tracking-wider text-slate-400 mb-2">
                Admin Email
              </label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-slate-500">
                  <Mail className="w-4 h-4" />
                </div>
                <input
                  type="email"
                  required
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="email"
                  autoComplete="off"
                  data-lpignore="true"
                  data-form-type="other"
                  name="worklog_admin_email_field_x99"
                  readOnly
                  onFocus={(e) => e.target.removeAttribute('readonly')}
                  className="w-full pl-10 pr-4 py-3 bg-slate-950/80 border border-slate-800 rounded-xl text-slate-100 text-sm placeholder-slate-600 focus:outline-none focus:border-orange-500 focus:ring-1 focus:ring-orange-500 transition-all"
                />
              </div>
            </div>

            <div>
              <label className="block text-xs font-bold uppercase tracking-wider text-slate-400 mb-2">
                Password
              </label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-slate-500">
                  <Lock className="w-4 h-4" />
                </div>
                <input
                  type={showPassword ? "text" : "password"}
                  required
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="password"
                  autoComplete="one-time-code"
                  data-lpignore="true"
                  data-form-type="other"
                  name="custom_pass_secure_field"
                  className="w-full pl-10 pr-12 py-3 bg-slate-950/80 border border-slate-800 rounded-xl text-slate-100 text-sm placeholder-slate-600 focus:outline-none focus:border-orange-500 focus:ring-1 focus:ring-orange-500 transition-all"
                />
                <button
                  type="button"
                  onMouseDown={() => setShowPassword(true)}
                  onMouseUp={() => setShowPassword(false)}
                  onMouseLeave={() => setShowPassword(false)}
                  onTouchStart={() => setShowPassword(true)}
                  onTouchEnd={() => setShowPassword(false)}
                  className="absolute inset-y-0 right-0 pr-3.5 flex items-center text-slate-500 hover:text-slate-300 transition-colors cursor-pointer select-none"
                  title="กดค้างเพื่อดูรหัสผ่าน"
                >
                  {showPassword ? <EyeOff className="w-4 h-4 text-orange-400" /> : <Eye className="w-4 h-4" />}
                </button>
              </div>
              <p className="text-[11px] text-slate-500 mt-1.5 flex items-center justify-between">
                <span>บังคับกรอกรหัสผ่านผู้ดูแลระบบ</span>
                <span className="text-emerald-400/80 font-mono">1 อุปกรณ์ต่อ 1 คน</span>
              </p>
            </div>

            <button
              type="submit"
              disabled={isLoading}
              className="w-full py-3.5 px-4 bg-gradient-to-r from-orange-600 to-amber-600 hover:from-orange-500 hover:to-amber-500 text-slate-950 font-extrabold rounded-xl text-sm flex items-center justify-center space-x-2 transition-all shadow-lg shadow-orange-950/50 disabled:opacity-50 cursor-pointer"
            >
              {isLoading ? (
                <>
                  <Loader2 className="w-4 h-4 animate-spin text-slate-950" />
                  <span>กำลังตรวจสอบสิทธิ์...</span>
                </>
              ) : (
                <>
                  <span>เข้าสู่ระบบ Dashboard</span>
                  <ArrowRight className="w-4 h-4" />
                </>
              )}
            </button>
          </form>

          {/* Quick info footer */}
          <div className="mt-6 pt-6 border-t border-slate-800/80 text-center">
            <div className="inline-flex items-center space-x-1.5 text-xs text-slate-400 bg-slate-950/50 px-3 py-1.5 rounded-full border border-slate-800">
              <CheckCircle2 className="w-3.5 h-3.5 text-emerald-400" />
              <span>ความปลอดภัยระดับสูง: ล็อกอินได้ 1 อุปกรณ์ต่อ 1 บัญชีผู้ใช้ (Single Device Session)</span>
            </div>
          </div>
        </div>

        <div className="text-center mt-6 text-xs text-slate-600">
          WorkLog Enterprise Management System • Secure Admin Gateway
        </div>
      </div>
    </div>
  );
};
