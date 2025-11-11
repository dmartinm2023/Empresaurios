// pw-analyzer.js
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
