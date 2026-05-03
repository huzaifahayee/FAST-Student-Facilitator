import React, {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useRef,
  useState,
} from 'react';
import '../pages/PopReminders.css';
import './FsfDialog.css';

const DialogContext = createContext(null);

function normalizeAlert(opts) {
  if (typeof opts === 'string') {
    return { title: 'Notice', message: opts, okText: 'OK' };
  }
  return {
    title: opts.title ?? 'Notice',
    message: opts.message ?? '',
    okText: opts.okText ?? 'OK',
  };
}

/**
 * Global modal dialogs matching My Reminders / login pop-up shell (backdrop + glass sheet).
 * Replaces window.alert, confirm, and prompt for consistent UI.
 */
export function FsfDialogProvider({ children }) {
  const [state, setState] = useState(null);
  const backdropRef = useRef(null);
  const promptInputRef = useRef(null);

  const showAlert = useCallback((opts) => {
    const n = normalizeAlert(opts);
    return new Promise((resolve) => {
      setState({
        kind: 'alert',
        title: n.title,
        message: n.message,
        okText: n.okText,
        resolve,
      });
    });
  }, []);

  const showConfirm = useCallback((opts) => {
    const {
      title = 'Confirm',
      message,
      confirmText = 'OK',
      cancelText = 'Cancel',
      danger = false,
    } = opts;
    return new Promise((resolve) => {
      setState({
        kind: 'confirm',
        title,
        message,
        confirmText,
        cancelText,
        danger,
        resolve,
      });
    });
  }, []);

  const showPrompt = useCallback((opts) => {
    const {
      title = 'Enter text',
      message = '',
      placeholder = '',
      defaultValue = '',
      required = false,
      confirmText = 'OK',
      cancelText = 'Cancel',
    } = opts;
    return new Promise((resolve) => {
      setState({
        kind: 'prompt',
        title,
        message,
        placeholder,
        inputValue: defaultValue,
        required,
        confirmText,
        cancelText,
        promptError: '',
        resolve,
      });
    });
  }, []);

  const closeWith = useCallback((value) => {
    setState((s) => {
      if (!s) return null;
      if (s.kind === 'alert') s.resolve();
      else s.resolve(value);
      return null;
    });
  }, []);

  // Focus only when this dialog instance opens — `kind` + `title` stay stable while typing.
  useEffect(() => {
    if (!state) return;
    const id = requestAnimationFrame(() => {
      if (state.kind === 'prompt') {
        promptInputRef.current?.focus();
      } else {
        backdropRef.current?.focus();
      }
    });
    return () => cancelAnimationFrame(id);
  }, [state?.kind, state?.title]);

  const onBackdropMouseDown = () => {
    if (!state) return;
    if (state.kind === 'alert') closeWith();
    else if (state.kind === 'confirm') closeWith(false);
    else closeWith(null);
  };

  const onBackdropKeyDown = (e) => {
    if (e.key !== 'Escape') return;
    e.preventDefault();
    onBackdropMouseDown();
  };

  const submitPrompt = () => {
    if (!state || state.kind !== 'prompt') return;
    const raw = state.inputValue ?? '';
    const trimmed = raw.trim();
    if (state.required && !trimmed) {
      setState((s) =>
        s && s.kind === 'prompt'
          ? { ...s, promptError: 'This field is required.' }
          : s
      );
      return;
    }
    closeWith(trimmed);
  };

  const ctx = { showAlert, showConfirm, showPrompt };

  return (
    <DialogContext.Provider value={ctx}>
      {children}
      {state && (
        <div
          ref={backdropRef}
          role="presentation"
          className="modal-backdrop reminders-modal-backdrop fsf-dialog-backdrop"
          tabIndex={-1}
          onMouseDown={(e) => {
            if (e.target === e.currentTarget) onBackdropMouseDown();
          }}
          onKeyDown={onBackdropKeyDown}
        >
          <div
            role="dialog"
            aria-modal="true"
            aria-labelledby="fsf-dialog-title"
            className="modal glass-card reminders-modal-sheet fsf-dialog-shell"
            onMouseDown={(e) => e.stopPropagation()}
          >
            <h2 id="fsf-dialog-title" className="fsf-dialog-title">
              {state.title}
            </h2>
            {state.message ? (
              <p className="fsf-dialog-body">{state.message}</p>
            ) : null}

            {state.kind === 'prompt' ? (
              <>
                <input
                  ref={promptInputRef}
                  type="text"
                  className="fsf-dialog-input"
                  value={state.inputValue}
                  placeholder={state.placeholder}
                  autoComplete="off"
                  onChange={(e) =>
                    setState((s) =>
                      s && s.kind === 'prompt'
                        ? {
                            ...s,
                            inputValue: e.target.value,
                            promptError: '',
                          }
                        : s
                    )
                  }
                  onKeyDown={(e) => {
                    if (e.key === 'Enter') {
                      e.preventDefault();
                      submitPrompt();
                    }
                  }}
                />
                {state.promptError ? (
                  <div className="fsf-dialog-inline-error">{state.promptError}</div>
                ) : null}
              </>
            ) : null}

            <div className="fsf-dialog-actions">
              {(state.kind === 'confirm' || state.kind === 'prompt') && (
                <button
                  type="button"
                  className="secondary-btn fsf-dialog-action-btn"
                  onClick={() =>
                    closeWith(state.kind === 'confirm' ? false : null)
                  }
                >
                  {state.cancelText}
                </button>
              )}
              <button
                type="button"
                className={`primary-btn fsf-dialog-action-btn${
                  state.kind === 'confirm' && state.danger
                    ? ' fsf-dialog-primary-danger'
                    : ''
                }`}
                onClick={() => {
                  if (state.kind === 'alert') closeWith();
                  else if (state.kind === 'confirm') closeWith(true);
                  else submitPrompt();
                }}
              >
                {state.kind === 'prompt'
                  ? state.confirmText
                  : state.kind === 'confirm'
                    ? state.confirmText
                    : state.okText}
              </button>
            </div>
          </div>
        </div>
      )}
    </DialogContext.Provider>
  );
}

// Hook is intentionally exported alongside the provider (app-wide dialog API).
// eslint-disable-next-line react-refresh/only-export-components
export function useFsfDialog() {
  const ctx = useContext(DialogContext);
  if (!ctx) {
    throw new Error('useFsfDialog must be used within FsfDialogProvider');
  }
  return ctx;
}
