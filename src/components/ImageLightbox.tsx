import React from "react";
import { X, ZoomIn, Download, ExternalLink } from "lucide-react";

interface ImageLightboxProps {
  isOpen: boolean;
  imageUrl: string;
  title?: string;
  onClose: () => void;
}

export const ImageLightbox: React.FC<ImageLightboxProps> = ({
  isOpen,
  imageUrl,
  title = "รูปภาพหลักฐาน",
  onClose
}) => {
  if (!isOpen || !imageUrl) return null;

  return (
    <div 
      id="image-lightbox-backdrop"
      className="fixed inset-0 z-50 bg-slate-950/90 backdrop-blur-md flex items-center justify-center p-4"
      onClick={onClose}
    >
      <div 
        id="image-lightbox-content"
        className="relative max-w-4xl max-h-[90vh] bg-slate-900 border border-slate-800 rounded-2xl overflow-hidden shadow-2xl flex flex-col"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-slate-800 bg-slate-900/80">
          <div className="flex items-center space-x-2">
            <ZoomIn className="w-5 h-5 text-indigo-400" />
            <h3 className="text-sm font-semibold text-slate-100">{title}</h3>
          </div>
          <div className="flex items-center space-x-2">
            <a 
              href={imageUrl} 
              target="_blank" 
              rel="noopener noreferrer" 
              download
              className="p-2 text-slate-400 hover:text-slate-100 hover:bg-slate-800 rounded-lg transition-colors"
              title="เปิดภาพในแท็บใหม่ / ดาวน์โหลด"
            >
              <ExternalLink className="w-4 h-4" />
            </a>
            <button
              onClick={onClose}
              className="p-2 text-slate-400 hover:text-slate-100 hover:bg-slate-800 rounded-lg transition-colors"
            >
              <X className="w-5 h-5" />
            </button>
          </div>
        </div>

        {/* Image Display */}
        <div className="p-4 flex items-center justify-center bg-slate-950/50 overflow-auto max-h-[75vh]">
          <img 
            src={imageUrl} 
            alt={title} 
            referrerPolicy="no-referrer"
            className="max-w-full max-h-[70vh] object-contain rounded-lg shadow-md"
          />
        </div>
      </div>
    </div>
  );
};
