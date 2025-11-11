
(function(){
  if (window.__PW_ANALYZER_INIT__) return; // prevent double init
  window.__PW_ANALYZER_INIT__ = true;

  function estEntropy(pw){
    let charsets = 0;
    if(/[a-z]/.test(pw)) charsets += 26;
    if(/[A-Z]/.test(pw)) charsets += 26;
    if(/[0-9]/.test(pw)) charsets += 10;
    if(/[^A-Za-z0-9]/.test(pw)) charsets += 33;
    if(charsets === 0) return 0;
    return Math.round(pw.length * (Math.log(charsets)/Math.log(2)));
  }
  function scorePassword(pw, ctx){
    const tests = {
      length12: pw.length >= 12,
      upper: /[A-Z]/.test(pw),
      lower: /[a-z]/.test(pw),
      digit: /\d/.test(pw),
      symbol: /[^A-Za-z0-9]/.test(pw),
      noSeq: !/(.)\1{2,}/.test(pw) && !/(0123|1234|2345|abcd|qwer|asdf|zxcv|password|admin|qwerty)/i.test(pw),
      notUser: ctx && ctx.user ? !pw.toLowerCase().includes(ctx.user.toLowerCase()) : true
    };
    let score = 0;
    Object.keys(tests).forEach(k => { score += tests[k] ? 1 : 0; });
    if(pw.length >= 16) score += 1;
    if(/[A-Z]/.test(pw) && /[a-z]/.test(pw) && /\d/.test(pw) && /[^A-Za-z0-9]/.test(pw)) score += 1;
    const max = 10;
    const pct = Math.min(100, Math.round(score/max*100));
    let label='Debil', cls='weak', color='#ef5350';
    if(pct>=25){label='Aceptable';cls='fair';color='#ffa726';}
    if(pct>=50){label='Buena';cls='good';color='#42a5f5';}
    if(pct>=75){label='Fuerte';cls='strong';color='#66bb6a';}
    return {pct, label, cls, color, tests, entropy: estEntropy(pw)};
  }

  function wrapInputWithToggle(input){
    // avoid double wrap
    if (input.parentElement && input.parentElement.classList.contains('pw-input-wrap')) return input.parentElement;
    const wrap = document.createElement('div');
    wrap.className = 'pw-input-wrap';
    input.parentElement.insertBefore(wrap, input);
    wrap.appendChild(input);
    const btn = document.createElement('button');
    btn.type = 'button';
    btn.className = 'pw-toggle';
    btn.textContent = '👁';
    btn.setAttribute('aria-label', 'Mostrar/ocultar contraseña');
    btn.addEventListener('click', function(){
      input.type = input.type === 'password' ? 'text' : 'password';
      btn.textContent = input.type === 'password' ? '👁' : '🙈';
      input.focus();
    });
    wrap.appendChild(btn);
    return wrap;
  }

  function makeUI(passInput, userInput){
    if (passInput.dataset.pwInit === '1') return;
    passInput.dataset.pwInit = '1';

    wrapInputWithToggle(passInput);

    const wrap = document.createElement('div');
    wrap.className = 'pw-wrap';
    wrap.innerHTML = [
      '<div class="pw-meter"><div></div></div>',
      '<div class="pw-status"><span class="badge weak" data-badge></span>',
      '<span class="pw-entropy" data-entropy></span></div>',
      '<div class="pw-checks">',
      '<div class="pw-check" data-check="length12"><span class="dot"></span><span>>= 12 caracteres</span></div>',
      '<div class="pw-check" data-check="upper"><span class="dot"></span><span>Incluye MAYUSCULAS</span></div>',
      '<div class="pw-check" data-check="lower"><span class="dot"></span><span>Incluye minusculas</span></div>',
      '<div class="pw-check" data-check="digit"><span class="dot"></span><span>Incluye numeros</span></div>',
      '<div class="pw-check" data-check="symbol"><span class="dot"></span><span>Incluye simbolos (! % _ # ...)</span></div>',
      '<div class="pw-check" data-check="noSeq"><span class="dot"></span><span>Sin secuencias/repeticiones</span></div>',
      '<div class="pw-check" data-check="notUser"><span class="dot"></span><span>No usar usuario/email</span></div>',
      '</div>',
      '<div class="pw-tip" data-tip></div>',
      '<div class="pw-chips" data-chips></div>',
      '<div class="pw-actions">',
      '<button type="button" class="pw-btn" data-ins="+!">Añadir "!")</button>',
      '<button type="button" class="pw-btn" data-ins="+_">Añadir "_"</button>',
      '<button type="button" class="pw-btn" data-ins="+%">Añadir "%"</button>',
      '<button type="button" class="pw-btn" data-suggest>Generar idea</button>',
      '</div>'
    ].join('');
    passInput.insertAdjacentElement('afterend', wrap);

    const bar = wrap.querySelector('.pw-meter > div');
    const badge = wrap.querySelector('[data-badge]');
    const entropyEl = wrap.querySelector('[data-entropy]');
    const tipEl = wrap.querySelector('[data-tip]');
    const chipsEl = wrap.querySelector('[data-chips]');
    const checks = wrap.querySelectorAll('[data-check]');
    const boosters = [
      'Mezcla 2 palabras sin relacion + numero + simbolo: "Cebra!Azulejo7"',
      'Frase corta con espacios/guiones bajos: "cine_martes_21"',
      'Prefijo privado + palabra + numero: "~Tesis2025*Granito"'
    ];
    const chipHints = [
      'No reutilices contraseñas',
      'Evita "1234", "qwerty"',
      'Usa >= 16 si es sensible',
      'No pegues el usuario en la clave'
    ];

    function update(){
      const ctx = { user: (userInput && userInput.value || '').trim() };
      const v = passInput.value || '';
      const res = scorePassword(v, ctx);
      bar.style.width = res.pct + '%';
      bar.style.background = res.color;
      badge.className = 'badge ' + res.cls;
      badge.textContent = res.label + ' (' + res.pct + '%)';
      entropyEl.textContent = 'Entropia aprox.: ' + res.entropy + ' bits';
      checks.forEach(c=>{
        const key = c.getAttribute('data-check');
        if(res.tests[key]) c.classList.add('ok'); else c.classList.remove('ok');
      });
      tipEl.textContent = res.pct < 75 ? boosters[Math.floor(Math.random()*boosters.length)] : 'Listo: contraseña solida.';
      chipsEl.innerHTML = '';
      chipHints.forEach(h=>{
        const s = document.createElement('span');
        s.className = 'pw-chip';
        s.textContent = h;
        chipsEl.appendChild(s);
      });
    }

    wrap.addEventListener('click', function(e){
      const btn = e.target.closest('[data-ins]');
      if(btn){
        const sym = btn.getAttribute('data-ins').slice(1);
        const start = passInput.selectionStart || passInput.value.length;
        const end = passInput.selectionEnd || passInput.value.length;
        const v = passInput.value;
        passInput.value = v.slice(0,start) + sym + v.slice(end);
        passInput.dispatchEvent(new Event('input'));
        passInput.focus();
        passInput.setSelectionRange(start+sym.length,start+sym.length);
      }
      if(e.target.matches('[data-suggest]')){
        const ideas = ['Cobre!Rugby_2042','Menta%Faro_19','Luna@Tren_852','Eco_Tabla!703'];
        passInput.value = ideas[Math.floor(Math.random()*ideas.length)];
        passInput.dispatchEvent(new Event('input'));
      }
    });

    passInput.addEventListener('input', update);
    if(userInput){ userInput.addEventListener('input', update); }
    update();
  }

  document.addEventListener('DOMContentLoaded', function(){
    const passInput = document.querySelector('input[name="passwordEncrypted"], input[type="password"]');
    const userInput = document.querySelector('input[name="login"], input[name="username"], input[type="email"]');
    if(passInput){ makeUI(passInput, userInput); }
  });
})();
