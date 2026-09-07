import React from "react";

interface LogoProps {
  className?: string;
  size?: number | string;
  showText?: boolean;
  textSize?: string;
  subtextSize?: string;
}

export const Logo: React.FC<LogoProps> = ({
  className = "",
  size = 36,
  showText = false,
  textSize = "text-base",
  subtextSize = "text-[10px]"
}) => {
  return (
    <div className={`flex items-center gap-2.5 ${className}`}>
      {/* Precision Vector SVG Logo matching the user's uploaded icon */}
      <svg
        width={size}
        height={size}
        viewBox="0 0 512 512"
        fill="none"
        xmlns="http://www.w3.org/2000/svg"
        className="shrink-0 drop-shadow-sm transition-transform hover:scale-105"
      >
        {/* Background Container */}
        <rect width="512" height="512" rx="100" fill="#0B0F19" />
        
        {/* Clipboard Outer Frame */}
        <rect
          x="110"
          y="110"
          width="292"
          height="340"
          rx="36"
          fill="#0E1424"
          stroke="#FFFFFF"
          strokeWidth="20"
          strokeLinejoin="round"
        />
        
        {/* Clipboard Top Clip */}
        <path
          d="M190 110V88C190 77 199 68 210 68H302C313 68 322 77 322 88V110"
          fill="#0E1424"
          stroke="#FFFFFF"
          strokeWidth="20"
          strokeLinejoin="round"
        />
        <rect x="228" y="92" width="56" height="12" rx="6" fill="#FFFFFF" />
        
        {/* Top Clip Handle */}
        <path
          d="M236 68V54C236 46 244 38 256 38C268 38 276 46 276 54V68"
          stroke="#FFFFFF"
          strokeWidth="16"
          strokeLinecap="round"
        />

        {/* Subtle Document Lines */}
        <line x1="160" y1="180" x2="192" y2="180" stroke="#CBD5E1" strokeWidth="14" strokeLinecap="round" />
        <line x1="160" y1="340" x2="196" y2="340" stroke="#CBD5E1" strokeWidth="14" strokeLinecap="round" />
        <line x1="316" y1="340" x2="352" y2="340" stroke="#CBD5E1" strokeWidth="14" strokeLinecap="round" />
        <line x1="160" y1="388" x2="352" y2="388" stroke="#CBD5E1" strokeWidth="14" strokeLinecap="round" />

        {/* Orange Circle Badge */}
        <circle cx="256" cy="256" r="64" stroke="#F97316" strokeWidth="20" fill="none" />
        
        {/* Orange Checkmark Inside Circle */}
        <path
          d="M224 256L246 278L292 230"
          stroke="#F97316"
          strokeWidth="22"
          strokeLinecap="round"
          strokeLinejoin="round"
        />
      </svg>

      {showText && (
        <div className="flex flex-col leading-tight min-w-0 shrink">
          <div className={`font-bold text-white tracking-wide ${textSize} font-['Prompt'] flex items-center gap-1 min-w-0`}>
            <span className="truncate">WORKLOG</span>
            <span className="text-orange-500 font-normal hidden sm:inline">·</span>
            <span className="hidden sm:inline text-xs font-semibold text-slate-300">OPERATIONS</span>
          </div>
          <span className={`text-slate-400 font-mono tracking-wider ${subtextSize} hidden md:block truncate`}>
            DISPATCH & SERVICE TRACKING
          </span>
        </div>
      )}
    </div>
  );
};
