/**
 * ═══════════════════════════════════════════════════════════
 * Bipando — Login Page Script
 * Arquivo: login.js
 * ═══════════════════════════════════════════════════════════
 *
 * Estrutura:
 *  1. Constantes / seletores DOM
 *  2. Decoração de barcode animado
 *  3. Controle de visibilidade dos formulários
 *  4. Login com Google (OAuth / Firebase)
 *  5. Login com e-mail e senha
 *  6. Toast de feedback
 *  7. Atalhos de teclado
 *  8. Init
 */

'use strict';

/* ── 1. SELETORES DOM ─────────────────────────────────────── */
const DOM = {
  /* Barcode decoration */
  bcLines:       document.getElementById('bcLines'),

  /* Seções do card */
  googleSection: document.getElementById('googleSection'),
  emailSection:  document.getElementById('emailSection'),
  toggleMode:    document.getElementById('toggleMode'),

  /* Botão Google */
  btnGoogle:      document.getElementById('btnGoogle'),
  btnGoogleText:  document.getElementById('btnGoogleText'),
  googleSpinner:  document.getElementById('googleSpinner'),

  /* Inputs de email/senha */
  emailInput: document.getElementById('emailInput'),
  passInput:  document.getElementById('passInput'),

  /* Botão de e-mail */
  btnEmail:      document.getElementById('btnEmail'),
  btnEmailText:  document.getElementById('btnEmailText'),
  emailSpinner:  document.getElementById('emailSpinner'),

  /* Toast */
  toast:    document.getElementById('toast'),
  toastMsg: document.getElementById('toastMsg'),
};

/* ── 2. DECORAÇÃO DE BARCODE ──────────────────────────────── */
const BAR_HEIGHTS = [28, 18, 40, 22, 35, 15, 42, 25, 30, 20, 38, 18, 44, 22, 32, 16, 40, 28, 36, 20, 44, 18, 30, 24];

function buildBarcodeDecoration() {
  BAR_HEIGHTS.forEach((height, index) => {
    const bar = document.createElement('div');
    bar.className = 'bc-bar';
    bar.style.height = height + 'px';
    bar.style.animationDelay = (index * 0.08) + 's';
    DOM.bcLines.appendChild(bar);
  });
}

/* ── 3. CONTROLE DE VISIBILIDADE ─────────────────────────── */

/**
 * Exibe o formulário de e-mail e oculta o bloco Google.
 */
function showEmailForm() {
  DOM.googleSection.style.display = 'none';
  DOM.emailSection.style.display  = 'block';
  DOM.toggleMode.style.display    = 'none';
  DOM.emailInput.focus();
}

/**
 * Volta para o bloco de login com Google.
 */
function hideEmailForm() {
  DOM.googleSection.style.display = 'block';
  DOM.emailSection.style.display  = 'none';
  DOM.toggleMode.style.display    = 'block';
}

/* ── 4. LOGIN COM GOOGLE ──────────────────────────────────── */

/**
 * Coloca o botão Google em estado de loading e
 * dispara o fluxo OAuth / Firebase.
 *
 * Substitua o bloco "TODO" pelo código real do Firebase SDK:
 *   import { signInWithPopup, GoogleAuthProvider } from 'firebase/auth';
 *   const provider = new GoogleAuthProvider();
 *   signInWithPopup(auth, provider)
 *     .then(() => window.location.href = '/dashboard')
 *     .catch(err => showToast('Erro: ' + err.message));
 */
function handleGoogleLogin() {
  setButtonLoading(DOM.btnGoogle, DOM.btnGoogleText, DOM.googleSpinner, true);

  /* TODO: substituir pelo Firebase signInWithPopup */
  setTimeout(() => {
    setButtonLoading(DOM.btnGoogle, DOM.btnGoogleText, DOM.googleSpinner, false);
    showToast('🔐 Redirecionando para autenticação Google…');
    /* window.location.href = '/dashboard'; */
  }, 2000);
}

/* ── 5. LOGIN COM E-MAIL E SENHA ──────────────────────────── */

/**
 * Valida os campos, coloca o botão em loading e
 * dispara a autenticação via Firebase.
 *
 * Substitua o bloco "TODO" pelo código real do Firebase SDK:
 *   import { signInWithEmailAndPassword } from 'firebase/auth';
 *   signInWithEmailAndPassword(auth, email, password)
 *     .then(() => window.location.href = '/dashboard')
 *     .catch(err => {
 *       showToast('Credenciais inválidas.');
 *       setButtonLoading(...)
 *     });
 */
function handleEmailLogin() {
  const email    = DOM.emailInput.value.trim();
  const password = DOM.passInput.value;

  if (!email || !password) {
    showToast('⚠️  Preencha e-mail e senha.');
    return;
  }

  if (!isValidEmail(email)) {
    showToast('⚠️  Formato de e-mail inválido.');
    return;
  }

  setButtonLoading(DOM.btnEmail, DOM.btnEmailText, DOM.emailSpinner, true);

  /* TODO: substituir pelo Firebase signInWithEmailAndPassword */
  setTimeout(() => {
    setButtonLoading(DOM.btnEmail, DOM.btnEmailText, DOM.emailSpinner, false);
    showToast('✓ Login realizado! Entrando no painel…');
    /* window.location.href = '/dashboard'; */
  }, 2000);
}

/* ── 6. TOAST DE FEEDBACK ─────────────────────────────────── */

let toastTimer = null;

/**
 * Exibe uma mensagem temporária no toast.
 * @param {string} message - Texto a exibir.
 * @param {number} [duration=3500] - Duração em ms.
 */
function showToast(message, duration = 3500) {
  DOM.toastMsg.textContent = message;
  DOM.toast.classList.add('show');

  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => DOM.toast.classList.remove('show'), duration);
}

/* ── 7. ATALHOS DE TECLADO ────────────────────────────────── */

function handleKeydown(event) {
  if (event.key !== 'Enter') return;
  if (DOM.emailSection.style.display === 'block') {
    handleEmailLogin();
  }
}

/* ── HELPERS ──────────────────────────────────────────────── */

/**
 * Ativa/desativa o estado de loading de um botão.
 * @param {HTMLElement} btn
 * @param {HTMLElement} textEl
 * @param {HTMLElement} spinnerEl
 * @param {boolean}     loading
 */
function setButtonLoading(btn, textEl, spinnerEl, loading) {
  btn.disabled           = loading;
  textEl.style.display   = loading ? 'none'   : 'inline';
  spinnerEl.style.display = loading ? 'block'  : 'none';
}

/**
 * Validação básica de formato de e-mail.
 * @param {string} email
 * @returns {boolean}
 */
function isValidEmail(email) {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
}

/* ── 8. INIT ──────────────────────────────────────────────── */

/**
 * Ponto de entrada: chamado quando o DOM está pronto.
 * Expõe as funções necessárias no escopo global para
 * compatibilidade com os atributos onclick do HTML.
 */
function init() {
  buildBarcodeDecoration();
  document.addEventListener('keydown', handleKeydown);

  /* Expõe funções para os handlers onclick inline do HTML */
  window.showEmailForm   = showEmailForm;
  window.hideEmailForm   = hideEmailForm;
  window.handleGoogleLogin = handleGoogleLogin;
  window.handleEmailLogin  = handleEmailLogin;
  window.showToast         = showToast;
}

document.addEventListener('DOMContentLoaded', init);