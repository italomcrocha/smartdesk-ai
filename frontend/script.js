const form = document.querySelector('#form');
const description = document.querySelector('#descricao');
const button = document.querySelector('#analisar');
const error = document.querySelector('#error');
const resultCard = document.querySelector('.result-card');
let busy = false;

// Same origin when served by Spring; a separate local frontend uses port 8081.
const apiBase = ['5500', '3000'].includes(location.port) || location.protocol === 'file:'
  ? 'http://localhost:8081' : '';

function updateInput() {
  document.querySelector('#count').textContent = `${description.value.length.toLocaleString('pt-BR')} / 10.000`;
  button.disabled = busy || !description.value.trim();
}
description.addEventListener('input', updateInput);
document.querySelectorAll('[data-example]').forEach(example => {
  example.addEventListener('click', () => {
    if (busy) return;
    description.value = example.dataset.example;
    error.hidden = true;
    updateInput();
    description.focus();
  });
});

form.addEventListener('submit', async event => {
  event.preventDefault();
  if (busy || !description.value.trim()) return;
  busy = true;
  error.hidden = true;
  description.disabled = true;
  document.querySelectorAll('[data-example]').forEach(item => { item.disabled = true; });
  document.querySelector('#empty').hidden = true;
  document.querySelector('#result').hidden = true;
  document.querySelector('#loading').hidden = false;
  document.querySelector('#button-label').textContent = 'Analisando chamado com IA...';
  resultCard.setAttribute('aria-busy', 'true');
  updateInput();
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), 65000);
  try {
    const response = await fetch(`${apiBase}/api/chamados/analisar`, {
      method: 'POST', headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ descricao: description.value.trim() }), signal: controller.signal
    });
    const data = await response.json().catch(() => ({}));
    if (!response.ok) throw new Error(data.erro || 'Não foi possível analisar o chamado. Tente novamente.');
    const priorities = { CRITICA: 'CRÍTICA', ALTA: 'ALTA', MEDIA: 'MÉDIA', BAIXA: 'BAIXA' };
    if (!priorities[data.prioridade] || !['categoria', 'resumo', 'acaoRecomendada'].every(key => typeof data[key] === 'string' && data[key].trim())) {
      throw new Error('A análise recebida está incompleta. Tente novamente.');
    }
    document.querySelector('#categoria').textContent = data.categoria;
    document.querySelector('#prioridade').textContent = priorities[data.prioridade];
    document.querySelector('#prioridade').className = `priority ${data.prioridade}`;
    document.querySelector('#resumo').textContent = data.resumo;
    document.querySelector('#acao').textContent = data.acaoRecomendada;
    document.querySelector('#result').hidden = false;
    document.querySelector('#result-title').setAttribute('tabindex', '-1');
    document.querySelector('#result-title').focus({ preventScroll: true });
    if (window.innerWidth <= 800) resultCard.scrollIntoView({ behavior: 'smooth', block: 'start' });
  } catch (failure) {
    error.textContent = failure.name === 'AbortError'
      ? 'A análise demorou mais que o esperado. Tente novamente.'
      : failure instanceof TypeError
        ? 'Não foi possível conectar ao backend. Verifique se o Spring Boot está em execução.'
        : failure.message;
    error.hidden = false;
    document.querySelector('#empty').hidden = false;
  } finally {
    clearTimeout(timeout);
    busy = false;
    description.disabled = false;
    document.querySelectorAll('[data-example]').forEach(item => { item.disabled = false; });
    document.querySelector('#loading').hidden = true;
    document.querySelector('#button-label').textContent = 'Analisar chamado';
    resultCard.setAttribute('aria-busy', 'false');
    updateInput();
  }
});
updateInput();
