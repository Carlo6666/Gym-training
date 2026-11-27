/* KORE · Secret Luxury Wellness — intro di apertura (circa 8 secondi)
   Sei immagini su nero → fili oro che convergono → logo Wellness → particelle oro → Lora.
   Timeline deterministica guidata da un solo orologio (render(t)), così il passaggio a Lora
   resta sincronizzato anche se il dispositivo perde qualche frame. */
(function () {
  'use strict';
  var root = document.getElementById('intro');
  if (!root) return;

  var GOLD = '#D4AF63';
  var T = { imgIn: 0, imgStep: 0.22, imgDur: 0.6, threadIn: 1.5, threadDur: 1.9, imgOut: 3.0, imgOutDur: 1.1,
            logoIn: 3.5, logoDur: 0.9, threadOut: 4.1, hold: 4.4, dissolve: 5.5, dissolveDur: 1.6,
            loraStart: 5.9, loraIn: 6.0, loraInDur: 1.4, end: 8.0 };
  var reduce = window.matchMedia && matchMedia('(prefers-reduced-motion: reduce)').matches;

  var tiles = Array.prototype.slice.call(root.querySelectorAll('.intro-img'));
  var svg = root.querySelector('.intro-threads');
  var logo = root.querySelector('.intro-logo');
  var canvas = root.querySelector('.intro-particles');
  var ctx = canvas.getContext('2d');
  var video = root.querySelector('.intro-lora');
  var line = root.querySelector('.intro-line');
  var listen = root.querySelector('.intro-listen');
  var skip = root.querySelector('.intro-skip');
  var paths = [], particles = null, start = 0, raf = 0, done = false, videoOk = false, videoStarted = false;
  var W = 0, H = 0, DPR = 1, cx = 0, cy = 0;

  var clamp = function (v) { return v < 0 ? 0 : v > 1 ? 1 : v; };
  var ease = function (v) { v = clamp(v); return v < .5 ? 4 * v * v * v : 1 - Math.pow(-2 * v + 2, 3) / 2; };
  var prog = function (t, a, d) { return clamp((t - a) / d); };

  // ---------------------------------------------------------------- layout
  function layout() {
    W = root.clientWidth; H = root.clientHeight; DPR = Math.min(window.devicePixelRatio || 1, 2);
    cx = W / 2; cy = H / 2;
    canvas.width = W * DPR; canvas.height = H * DPR; canvas.style.width = W + 'px'; canvas.style.height = H + 'px';
    svg.setAttribute('viewBox', '0 0 ' + W + ' ' + H);
    var size = Math.min(W * 0.3, H * 0.19, 230), rx = Math.min(W * 0.36, 420), ry = Math.min(H * 0.33, 330);
    while (svg.firstChild) svg.removeChild(svg.firstChild);
    var defs = document.createElementNS('http://www.w3.org/2000/svg', 'defs');
    defs.innerHTML = '<filter id="glow" x="-50%" y="-50%" width="200%" height="200%"><feGaussianBlur stdDeviation="2.2" result="b"/><feMerge><feMergeNode in="b"/><feMergeNode in="SourceGraphic"/></feMerge></filter>';
    svg.appendChild(defs);
    paths = tiles.map(function (el, i) {
      var a = -Math.PI / 2 + i * Math.PI / 3, x = cx + Math.cos(a) * rx, y = cy + Math.sin(a) * ry;
      el.style.width = el.style.height = size + 'px';
      el.style.left = (x - size / 2) + 'px'; el.style.top = (y - size / 2) + 'px';
      // curva morbida: il punto di controllo ruota leggermente, così i fili "avvolgono" il centro
      var qx = (x + cx) / 2 + Math.cos(a + Math.PI / 2) * size * 0.28, qy = (y + cy) / 2 + Math.sin(a + Math.PI / 2) * size * 0.28;
      var p = document.createElementNS('http://www.w3.org/2000/svg', 'path');
      p.setAttribute('d', 'M' + x + ' ' + y + ' Q' + qx + ' ' + qy + ' ' + cx + ' ' + cy);
      p.setAttribute('fill', 'none'); p.setAttribute('stroke', GOLD); p.setAttribute('stroke-width', '1.6');
      p.setAttribute('stroke-linecap', 'round'); p.setAttribute('filter', 'url(#glow)');
      svg.appendChild(p);
      var len = p.getTotalLength(); p.style.strokeDasharray = len + ' ' + len; p.style.strokeDashoffset = len;
      return { el: p, len: len };
    });
    particles = null;
  }

  // ---------------------------------------------------------------- particelle dal logo reale
  function buildParticles() {
    var r = logo.getBoundingClientRect(), base = root.getBoundingClientRect();
    if (!r.width || !logo.naturalWidth) return [];
    var off = document.createElement('canvas'), w = Math.round(r.width), h = Math.round(r.height);
    off.width = w; off.height = h;
    var o = off.getContext('2d'); o.drawImage(logo, 0, 0, w, h);
    var data; try { data = o.getImageData(0, 0, w, h).data; } catch (e) { return []; }
    var out = [], step = Math.max(2, Math.round(Math.min(w, h) / 110)), seed = 7;
    var rnd = function () { seed = (seed * 16807) % 2147483647; return seed / 2147483647; };
    for (var y = 0; y < h; y += step) for (var x = 0; x < w; x += step) {
      var k = (y * w + x) * 4; if (data[k + 3] < 60) continue;
      var ang = rnd() * Math.PI * 2, sp = 30 + rnd() * 120;
      out.push({ x: r.left - base.left + x, y: r.top - base.top + y, vx: Math.cos(ang) * sp, vy: Math.sin(ang) * sp - 60,
        delay: (x / w) * 0.55 + rnd() * 0.2, s: step * (0.5 + rnd() * 0.6),
        c: 'rgba(' + data[k] + ',' + data[k + 1] + ',' + data[k + 2] + ',' });
    }
    return out;
  }

  function drawParticles(t) {
    ctx.setTransform(DPR, 0, 0, DPR, 0, 0); ctx.clearRect(0, 0, W, H);
    var d = t - T.dissolve; if (d < 0 || d > T.dissolveDur + 0.8) return;
    if (!particles) particles = buildParticles();
    ctx.globalCompositeOperation = 'lighter';
    for (var i = 0; i < particles.length; i++) {
      var p = particles[i], u = d - p.delay; if (u < 0) { ctx.fillStyle = p.c + '1)'; ctx.fillRect(p.x, p.y, p.s, p.s); continue; }
      var life = clamp(1 - u / 1.1); if (!life) continue;
      ctx.fillStyle = p.c + (life * 0.95).toFixed(3) + ')';
      ctx.fillRect(p.x + p.vx * u, p.y + p.vy * u + 30 * u * u, p.s * (0.6 + life * 0.6), p.s * (0.6 + life * 0.6));
    }
    ctx.globalCompositeOperation = 'source-over';
  }

  // ---------------------------------------------------------------- Lora
  function startLora() {
    if (videoStarted || !videoOk) return; videoStarted = true;
    video.currentTime = 0; video.muted = false;
    var pr = video.play();
    if (pr && pr.catch) pr.catch(function () {
      // audio automatico bloccato dal browser: Lora parte senza audio e mostriamo «Ascolta Lora»
      video.muted = true; video.play().catch(function () {}); listen.hidden = false;
    });
  }
  listen.addEventListener('click', function () {
    listen.hidden = true; video.muted = false; video.currentTime = 0; video.play().catch(function () {});
  });
  video.addEventListener('ended', function () { finish(); });
  video.addEventListener('loadeddata', function () { videoOk = true; });
  video.addEventListener('error', function () { videoOk = false; }, true);
  if (video.readyState >= 2) videoOk = true;

  // ---------------------------------------------------------------- render
  function render(t) {
    tiles.forEach(function (el, i) {
      var a = ease(prog(t, T.imgIn + i * T.imgStep, T.imgDur)), b = ease(prog(t, T.imgOut + i * 0.06, T.imgOutDur));
      el.style.opacity = (a * (1 - b)).toFixed(3);
      el.style.transform = 'scale(' + (0.9 + a * 0.1 - b * 0.12).toFixed(3) + ')';
      el.style.filter = b ? 'blur(' + (b * 6).toFixed(1) + 'px)' : '';
    });
    var th = ease(prog(t, T.threadIn, T.threadDur)), tho = ease(prog(t, T.threadOut, 0.7));
    paths.forEach(function (p, i) {
      var k = clamp(th * 1.15 - i * 0.03);
      p.el.style.strokeDashoffset = (p.len * (1 - k)).toFixed(1);
      p.el.style.opacity = (1 - tho).toFixed(3);
    });
    var li = ease(prog(t, T.logoIn, T.logoDur)), dissolving = t >= T.dissolve;
    logo.style.opacity = dissolving ? 0 : li.toFixed(3);
    logo.style.transform = 'translate(-50%,-50%) scale(' + (0.86 + li * 0.14).toFixed(3) + ')';
    root.style.setProperty('--halo', (li * (1 - prog(t, T.dissolve, 1))).toFixed(3));
    drawParticles(t);
    if (t >= T.loraStart) startLora();
    var lo = ease(prog(t, T.loraIn, T.loraInDur));
    video.style.opacity = videoOk ? lo.toFixed(3) : 0;
    line.style.opacity = ease(prog(t, T.loraIn + 0.3, 0.9)).toFixed(3);
  }

  function loop(now) {
    if (done) return;
    var t = (now - start) / 1000;
    render(t);
    if (t >= T.end && !videoStarted) return finish(); // nessun video: chiudiamo a fine intro
    if (t >= T.end + 0.4 && videoStarted && video.paused && !video.muted) return finish();
    raf = requestAnimationFrame(loop);
  }

  function finish() {
    if (done) return; done = true; cancelAnimationFrame(raf);
    root.classList.add('intro-out');
    try { video.pause(); } catch (e) {}
    document.documentElement.classList.remove('intro-lock');
    setTimeout(function () { root.hidden = true; root.setAttribute('aria-hidden', 'true'); }, 700);
    var main = document.getElementById('top'); if (main) main.focus({ preventScroll: true });
  }
  skip.addEventListener('click', finish);
  document.addEventListener('keydown', function (e) { if (e.key === 'Escape') finish(); });

  // ---------------------------------------------------------------- avvio
  function begin() {
    layout();
    if (reduce) { // movimento ridotto: logo subito, poi Lora
      T.imgIn = -10; T.threadIn = -10; T.imgOut = -9; T.threadOut = -9; T.logoIn = 0; T.logoDur = 0.4;
      T.dissolve = 1.6; T.dissolveDur = 0.6; T.loraStart = 1.8; T.loraIn = 1.9; T.loraInDur = 0.6; T.end = 3.0;
    }
    start = performance.now();
    raf = requestAnimationFrame(loop);
  }
  window.addEventListener('resize', function () { if (!done) layout(); });
  // aspettiamo il logo e le immagini (massimo 1,5 s) per non partire con riquadri vuoti
  var waits = [logo].concat(tiles.map(function (el) { return el.querySelector('img'); })).map(function (img) {
    return img.complete ? Promise.resolve() : new Promise(function (res) { img.addEventListener('load', res); img.addEventListener('error', res); });
  });
  Promise.race([Promise.all(waits), new Promise(function (res) { setTimeout(res, 1500); })]).then(begin);

  // per i test: posiziona l'intro su un istante preciso
  window.__koreIntro = { seek: function (t) { cancelAnimationFrame(raf); layout(); render(t); }, finish: finish, timeline: T };
})();
