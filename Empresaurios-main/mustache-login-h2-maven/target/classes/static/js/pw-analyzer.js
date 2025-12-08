(function (global) {
  function score(pw, ctx) {
    let s = 0;
    const t = {
      length: pw.length >= 12,
      upper: /[A-Z]/.test(pw),
      lower: /[a-z]/.test(pw),
      digit: /\d/.test(pw),
      symbol: /[^A-Za-z0-9]/.test(pw),
      noSeq: !/(.)\1{2,}/.test(pw) && !/(0123|1234|2345|3456|4567|5678|6789|abcd|qwer|asdf|zxcv)/i.test(pw),
      notUser: ctx && ctx.user ? !pw.toLowerCase().includes(ctx.user.toLowerCase()) : true
    };
    s += t.length + t.upper + t.lower + t.digit + t.symbol + t.noSeq + t.notUser;
    if (pw.length >= 16) s += 1;
    if (t.upper && t.lower && t.digit && t.symbol) s += 1;

    const max = 10;
    const pct = Math.min(100, Math.round(s / max * 100));

    let level = 'weak', label = 'Débil';
    if (pct >= 25) { level = 'fair'; label = 'Aceptable'; }
    if (pct >= 50) { level = 'good'; label = 'Buena'; }
    if (pct >= 75) { level = 'strong'; label = 'Fuerte'; }

    return { pct, level, label, tests: t };
  }

  function mount(input, userField, mountEl) {
    const ui = mountEl || document.createElement('div');
    ui.classList.add('pw-wrap');

    // Estructura
    const meter = document.createElement('div');
    meter.className = 'pw-meter';
    const bar = document.createElement('div');
    bar.className = 'pw-bar weak';
    meter.appendChild(bar);

    const badge = document.createElement('div');
    badge.className = 'pw-badge weak';
    badge.innerHTML = '<span class="text">Debil</span> <span class="pw-mini">(0%)</span>';

    const tips = document.createElement('div');
    tips.className = 'pw-tips';

    ui.appendChild(meter);
    ui.appendChild(badge);
    ui.appendChild(tips);

    if (!mountEl) input.insertAdjacentElement('afterend', ui);

    function render() {
      const ctx = { user: userField ? userField.value.trim() : '' };
      const pw = input.value || '';
      const r = score(pw, ctx);

      // Barra
      bar.style.width = r.pct + '%';
      bar.className = 'pw-bar ' + r.level;

      // Badge
      badge.className = 'pw-badge ' + r.level;
      badge.querySelector('.text').textContent = r.label;
      badge.querySelector('.pw-mini').textContent = '(' + r.pct + '%)';

      // Tips
      tips.innerHTML = '';
      const miss = [];
      const t = r.tests;
      if (!t.length) miss.push('Usa ≥ 12 caracteres');
      if (!t.upper) miss.push('Añade MAYUSCULAS');
      if (!t.lower) miss.push('Incluye minusculas');
      if (!t.digit) miss.push('Agrega numeros');
      if (!t.symbol) miss.push('Incluye un simbolo (! % _ #)');
      if (!t.noSeq) miss.push('Evita secuencias/repeticiones');
      if (!t.notUser) miss.push('No uses tu usuario');

      if (miss.length) {
        const chips = document.createElement('div');
        chips.className = 'pw-chips';
        miss.slice(0, 3).forEach(m => {
          const chip = document.createElement('span');
          chip.className = 'pw-chip';
          chip.textContent = m;
          chips.appendChild(chip);
        });
        tips.appendChild(chips);
      } else if (pw.length) {
        const ok = document.createElement('div');
        ok.className = 'pw-ok';
        ok.textContent = '¡Listo! Contraseña solida.';
        tips.appendChild(ok);
      }
    }

    input.addEventListener('input', render);
    if (userField) userField.addEventListener('input', render);
    render();

    return { render, destroy: () => ui.remove() };
  }

  global.PwAnalyzer = { mount };
})(window);

(function () {
  function csprngBytes(len) {
    const b = new Uint8Array(len);
    crypto.getRandomValues(b);
    return b;
  }

  function pickFrom(set, n) {
    const out = [];
    const bytes = csprngBytes(n);
    const m = set.length;
    for (let i = 0; i < n; i++) {
      out.push(set[bytes[i] % m]);
    }
    return out;
  }

  function shuffle(arr) {
    for (let i = arr.length - 1; i > 0; i--) {
      const j = crypto.getRandomValues(new Uint32Array(1))[0] % (i + 1);
      [arr[i], arr[j]] = [arr[j], arr[i]];
    }
    return arr;
  }

  function buildSets(opts) {
    let U = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ';
    let L = 'abcdefghijklmnopqrstuvwxyz';
    let D = '0123456789';
    let S = '!@#$%^&*()-_=+[]{};:,.?';
    if (opts.noAmbiguous) {
      U = U.replace(/[O]/g, '');
      L = L.replace(/[lio]/g, '');
      D = D.replace(/[01]/g, '');
    }
    const sets = [];
    if (opts.upper) sets.push(U);
    if (opts.lower) sets.push(L);
    if (opts.digit) sets.push(D);
    if (opts.symbol) sets.push(S);
    return sets;
  }

  function generatePassword(opts) {
    const sets = buildSets(opts);
    if (sets.length === 0) return '';
    const len = Math.max(8, Math.min(64, opts.length | 0 || 16));

    const must = sets.map(s => s[Math.floor((crypto.getRandomValues(new Uint32Array(1))[0]) % s.length)]);
    const pool = sets.join('');
    const rest = pickFrom(pool, Math.max(0, len - must.length));
    return shuffle(must.concat(rest)).join('');
  }

  function initPasswordGenerator() {
    const input = document.querySelector('input[name="passwordEncrypted"]');
    if (!input) return;

    const btnGen = document.getElementById('pw-generate');
    const btnCopy = document.getElementById('pw-copy');
    const len = document.getElementById('pw-len');
    const upper = document.getElementById('pw-upper');
    const lower = document.getElementById('pw-lower');
    const digit = document.getElementById('pw-digit');
    const symbol = document.getElementById('pw-symbol');
    const noamb = document.getElementById('pw-noamb');

    function currentOpts() {
      return {
        length: parseInt(len.value || '16', 10),
        upper: upper.checked,
        lower: lower.checked,
        digit: digit.checked,
        symbol: symbol.checked,
        noAmbiguous: noamb.checked
      };
    }

    btnGen?.addEventListener('click', () => {
      const pwd = generatePassword(currentOpts());
      input.value = pwd;
      input.dispatchEvent(new Event('input', { bubbles: true }));
    });

    btnCopy?.addEventListener('click', async () => {
      try {
        await navigator.clipboard.writeText(input.value || '');
        btnCopy.textContent = 'Copiado';
        setTimeout(() => btnCopy.textContent = 'Copiar', 1200);
      } catch {
        btnCopy.textContent = 'Error';
        setTimeout(() => btnCopy.textContent = 'Copiar', 1200);
      }
    });
  }

  window.initPasswordGenerator = initPasswordGenerator;
})();

window.addEventListener('DOMContentLoaded', () => {
  if (window.initPasswordGenerator) window.initPasswordGenerator();
});

(function(){
  const passInput = document.querySelector('input[name="passwordEncrypted"]');
  const toggle = document.getElementById('pw-toggle');
  if(!passInput || !toggle) return;

  function setVisible(v){
    passInput.type = v ? 'text' : 'password';
    toggle.textContent = v ? '🙈' : '👁';
    toggle.title = v ? 'Ocultar' : 'Mostrar';
    toggle.setAttribute('aria-label', toggle.title);
  }
  let visible = false;
  toggle.addEventListener('click', ()=>{
    visible = !visible;
    setVisible(visible);
    passInput.dispatchEvent(new Event('input', {bubbles:true}));
  });
  setVisible(false);
})();

(function(){
  const passInput = document.querySelector('input[name="passwordEncrypted"]');
  const btnGen = document.getElementById('pw-generate');
  const btnCopy = document.getElementById('pw-copy');
  const len = document.getElementById('pw-len');
  const upper = document.getElementById('pw-upper');
  const lower = document.getElementById('pw-lower');
  const digit = document.getElementById('pw-digit');
  const symbol = document.getElementById('pw-symbol');
  const noamb = document.getElementById('pw-noamb');

  if(!passInput || !btnGen) return;

  function flash(el, ok=true){
    const prev = el.textContent;
    el.textContent = ok ? 'Listo' : 'Error';
    el.classList.add('btn-primary');
    setTimeout(()=>{ el.textContent = prev; el.classList.remove('btn-primary'); }, 900);
  }
  function bytes(n){const b=new Uint8Array(n); crypto.getRandomValues(b); return b;}
  function shuffle(a){for(let i=a.length-1;i>0;i--){const j=crypto.getRandomValues(new Uint32Array(1))[0]%(i+1); [a[i],a[j]]=[a[j],a[i]];} return a;}
  function pick(pool,n){const out=[],b=bytes(n),m=pool.length; for(let i=0;i<n;i++) out.push(pool[b[i]%m]); return out;}
  function sets(opts){
    let U='ABCDEFGHIJKLMNOPQRSTUVWXYZ', L='abcdefghijklmnopqrstuvwxyz', D='0123456789', S='!@#$%^&*()-_=+[]{};:,.?';
    if(opts.noAmb){ U=U.replace(/O/g,''); L=L.replace(/[lio]/g,''); D=D.replace(/[01]/g,''); }
    const a=[]; if(opts.U)a.push(U); if(opts.L)a.push(L); if(opts.D)a.push(D); if(opts.S)a.push(S); return a;
  }
  function generate(opts){
    const st=sets(opts); if(!st.length) return '';
    const n = Math.max(8, Math.min(64, opts.len|0||16));
    const must = st.map(s => s[crypto.getRandomValues(new Uint32Array(1))[0] % s.length]);
    const pool = st.join(''); const rest = pick(pool, Math.max(0, n - must.length));
    return shuffle(must.concat(rest)).join('');
  }
  function cur(){ return { len:parseInt(len.value||'16',10), U:upper.checked, L:lower.checked, D:digit.checked, S:symbol.checked, noAmb:noamb.checked }; }

  btnGen.addEventListener('click', ()=>{
    const pwd = generate(cur());
    passInput.value = pwd;
    passInput.dispatchEvent(new Event('input', {bubbles:true}));
    flash(btnGen,true);
  });
  btnCopy.addEventListener('click', async ()=>{
    try{ await navigator.clipboard.writeText(passInput.value||''); flash(btnCopy,true); }
    catch{ flash(btnCopy,false); }
  });
})();