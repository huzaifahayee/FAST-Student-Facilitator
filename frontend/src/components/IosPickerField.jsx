import React, { useState, useEffect, useLayoutEffect, useRef, useCallback } from 'react';
import { createPortal } from 'react-dom';
import '../styles/IosMenuPicker.css';

/** z-index above app modals (e.g. reminders 5200, lost&found 5000). */
const PORTAL_Z = 6000;

/** iOS-style sheet picker (uses styles/IosMenuPicker.css). Sheet is portaled to document.body so modals cannot stack over it. */
export default function IosPickerField({
  value,
  onChange,
  options = [],
  sheetTitle = 'Select',
  minWidth,
  className = '',
}) {
  const [open, setOpen] = useState(false);
  const [sheetRect, setSheetRect] = useState(null);
  const rootRef = useRef(null);
  const triggerRef = useRef(null);
  const sheetRef = useRef(null);
  const current = options.find((o) => o.value === value);
  const label = current?.label ?? options[0]?.label ?? '';

  const updateRect = useCallback(() => {
    const el = triggerRef.current;
    if (!el) return;
    const r = el.getBoundingClientRect();
    const top = r.bottom + 8;
    const maxH = Math.max(120, window.innerHeight - top - 12);
    setSheetRect({
      left: r.left,
      top,
      width: r.width,
      maxHeight: maxH,
    });
  }, []);

  useLayoutEffect(() => {
    if (!open) {
      setSheetRect(null);
      return;
    }
    updateRect();
    window.addEventListener('resize', updateRect);
    window.addEventListener('scroll', updateRect, true);
    return () => {
      window.removeEventListener('resize', updateRect);
      window.removeEventListener('scroll', updateRect, true);
    };
  }, [open, updateRect]);

  useEffect(() => {
    if (!open) return;
    const onDoc = (e) => {
      const t = e.target;
      if (rootRef.current?.contains(t) || sheetRef.current?.contains(t)) return;
      setOpen(false);
    };
    const onKey = (e) => {
      if (e.key === 'Escape') setOpen(false);
    };
    document.addEventListener('mousedown', onDoc);
    document.addEventListener('keydown', onKey);
    return () => {
      document.removeEventListener('mousedown', onDoc);
      document.removeEventListener('keydown', onKey);
    };
  }, [open]);

  const rootClass = ['ios-category-dropdown', className].filter(Boolean).join(' ');
  const style = minWidth != null ? { minWidth } : undefined;

  const sheetContent =
    open &&
    sheetRect &&
    createPortal(
      <div
        ref={sheetRef}
        className="ios-category-dropdown-sheet ios-category-dropdown-sheet--portaled"
        style={{
          position: 'fixed',
          left: sheetRect.left,
          top: sheetRect.top,
          width: sheetRect.width,
          maxHeight: sheetRect.maxHeight,
          zIndex: PORTAL_Z,
        }}
      >
        <div className="ios-category-dropdown-panel" role="listbox" aria-label={sheetTitle}>
          <p className="ios-category-dropdown-title">{sheetTitle}</p>
          <div className="ios-category-dropdown-list">
            {options.map((opt) => (
              <button
                key={String(opt.value)}
                type="button"
                role="option"
                aria-selected={value === opt.value}
                className={`ios-category-option${value === opt.value ? ' is-selected' : ''}`}
                onClick={() => {
                  onChange(opt.value);
                  setOpen(false);
                }}
              >
                {opt.label}
              </button>
            ))}
          </div>
        </div>
      </div>,
      document.body
    );

  return (
    <div className={rootClass} ref={rootRef} style={style}>
      <button
        ref={triggerRef}
        type="button"
        className={`ios-category-dropdown-trigger${open ? ' is-open' : ''}`}
        aria-haspopup="listbox"
        aria-expanded={open}
        onClick={() => setOpen((o) => !o)}
      >
        <span className="ios-category-trigger-label">{label}</span>
        <span className="ios-category-trigger-chevron" aria-hidden>
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
            <path d="M6 9l6 6 6-6" />
          </svg>
        </span>
      </button>
      {sheetContent}
    </div>
  );
}
