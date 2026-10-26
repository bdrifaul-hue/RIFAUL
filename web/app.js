// RIFAUL AI Web Companion Engine
const chatContainer = document.getElementById('chatContainer');
const textInput = document.getElementById('textInput');
const sendBtn = document.getElementById('sendBtn');
const micBtn = document.getElementById('micBtn');
const stopBtn = document.getElementById('stopBtn');
const activeBanner = document.getElementById('activeBanner');
const activeText = document.getElementById('activeText');
const orbStatusText = document.getElementById('orbStatusText');
const statusPill = document.getElementById('statusPill');
const statusDot = document.getElementById('statusDot');
const langSelect = document.getElementById('langSelect');
const cmdChips = document.querySelectorAll('.cmd-chip');

let isListening = false;
let isSpeaking = false;
let recognition = null;
let speechSynth = window.speechSynthesis;

// Check Web Speech API support
if ('webkitSpeechRecognition' in window || 'SpeechRecognition' in window) {
  const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
  recognition = new SpeechRecognition();
  recognition.continuous = false;
  recognition.interimResults = true;

  recognition.onstart = () => {
    isListening = true;
    micBtn.classList.add('active');
    activeBanner.classList.remove('hidden');
    activeText.innerText = 'Listening to voice...';
    statusPill.innerText = 'LISTENING';
    statusDot.className = 'status-dot listening';
    orbStatusText.innerText = 'Listening to your voice in বাংলা / English...';
  };

  recognition.onresult = (event) => {
    let transcript = '';
    for (let i = event.resultIndex; i < event.results.length; ++i) {
      transcript += event.results[i][0].transcript;
    }
    textInput.value = transcript;
    if (event.results[0].isFinal) {
      handleUserSubmit(transcript);
    }
  };

  recognition.onerror = (event) => {
    console.warn('Speech error:', event.error);
    stopListening();
    orbStatusText.innerText = 'Voice error: ' + event.error + '. Try typing.';
  };

  recognition.onend = () => {
    stopListening();
  };
}

function startListening() {
  if (!recognition) {
    alert('Web Speech recognition is not supported in this browser. Please use Chrome or type your message.');
    return;
  }
  stopSpeaking();
  const selectedLang = langSelect.value;
  recognition.lang = selectedLang === 'auto' ? 'bn-BD' : selectedLang;
  try {
    recognition.start();
  } catch (e) {
    console.error(e);
  }
}

function stopListening() {
  isListening = false;
  micBtn.classList.remove('active');
  if (!isSpeaking) {
    activeBanner.classList.add('hidden');
    statusPill.innerText = 'ONLINE';
    statusDot.className = 'status-dot online';
    orbStatusText.innerText = 'Ready • Ask anything or command an app in বাংলা / English';
  }
  if (recognition) {
    try { recognition.stop(); } catch (_) {}
  }
}

function stopSpeaking() {
  if (speechSynth && speechSynth.speaking) {
    speechSynth.cancel();
  }
  isSpeaking = false;
  if (!isListening) {
    activeBanner.classList.add('hidden');
    statusPill.innerText = 'ONLINE';
    statusDot.className = 'status-dot online';
    orbStatusText.innerText = 'Ready • Ask anything or command an app in বাংলা / English';
  }
}

function speakText(text) {
  if (!speechSynth) return;
  stopSpeaking();
  isSpeaking = true;
  activeBanner.classList.remove('hidden');
  activeText.innerText = 'RIFAUL AI Speaking...';
  statusPill.innerText = 'SPEAKING';
  statusDot.className = 'status-dot speaking';
  orbStatusText.innerText = 'RIFAUL AI is speaking answer aloud...';

  const clean = text.replace(/[*#`_~]/g, '');
  const utterance = new SpeechSynthesisUtterance(clean);
  const isBengali = /[\u0980-\u09FF]/.test(clean);
  utterance.lang = isBengali ? 'bn-BD' : 'en-US';

  utterance.onend = () => {
    stopSpeaking();
  };
  utterance.onerror = () => {
    stopSpeaking();
  };

  speechSynth.speak(utterance);
}

// App command mapping
const APP_LINKS = {
  youtube: { name: 'YouTube', url: 'https://youtube.com', keys: ['youtube', 'ইউটিউব'] },
  chrome: { name: 'Google Chrome', url: 'https://google.com', keys: ['chrome', 'ক্রোম', 'browser', 'গুগল'] },
  facebook: { name: 'Facebook', url: 'https://facebook.com', keys: ['facebook', 'ফেসবুক', 'fb'] },
  whatsapp: { name: 'WhatsApp', url: 'https://web.whatsapp.com', keys: ['whatsapp', 'হোয়াটসঅ্যাপ'] },
  instagram: { name: 'Instagram', url: 'https://instagram.com', keys: ['instagram', 'ইনস্টাগ্রাম'] },
  gmail: { name: 'Gmail', url: 'https://mail.google.com', keys: ['gmail', 'জিমেইল'] },
  maps: { name: 'Google Maps', url: 'https://maps.google.com', keys: ['maps', 'ম্যাপ', 'ম্যাপস'] }
};

function checkAppCommand(input) {
  const lower = input.toLowerCase();
  for (const key in APP_LINKS) {
    const app = APP_LINKS[key];
    for (const kw of app.keys) {
      if (lower.includes(kw)) {
        return app;
      }
    }
  }
  return null;
}

function addMessage(sender, text, isAi = false, actionData = null) {
  const bubble = document.createElement('div');
  bubble.className = `chat-bubble ${isAi ? 'ai-bubble' : 'user-bubble'}`;
  const time = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });

  let actionHtml = '';
  if (actionData) {
    actionHtml = `
      <div style="margin-top: 8px; padding: 8px; background: rgba(0,242,254,0.1); border-radius: 8px; border: 1px solid #1e2f4d;">
        <p style="font-size: 0.8rem; margin-bottom: 6px; color: #38bdf8;">Verified App Action Available:</p>
        <a href="${actionData.url}" target="_blank" style="display: inline-block; padding: 6px 12px; background: #00f2fe; color: #000; text-decoration: none; border-radius: 6px; font-weight: bold; font-size: 0.8rem;">Open ${actionData.name}</a>
      </div>
    `;
  }

  bubble.innerHTML = `
    <div class="bubble-header">
      <span class="badge">${sender}</span>
      <span class="time">${time}</span>
    </div>
    <div class="bubble-body">${text.replace(/\n/g, '<br>')}${actionHtml}</div>
  `;
  chatContainer.appendChild(bubble);
  chatContainer.scrollTop = chatContainer.scrollHeight;
}

async function handleUserSubmit(query) {
  const text = query.trim();
  if (!text) return;
  textInput.value = '';

  addMessage('YOU', text, false);

  // Check Stop command
  if (text.toLowerCase() === 'stop' || text.toLowerCase() === 'stop listening' || text.includes('থামো')) {
    stopListening();
    stopSpeaking();
    addMessage('RIFAUL AI', 'Stopped listening and speaking.', true);
    return;
  }

  // Check Time command
  if (text.includes('কয়টা বাজে') || text.includes('time') && text.includes('what')) {
    const timeStr = new Date().toLocaleTimeString();
    const reply = /[\u0980-\u09FF]/.test(text)
      ? `এখন সময় হলো ${timeStr}।`
      : `The current time is ${timeStr}.`;
    addMessage('RIFAUL AI', reply, true);
    speakText(reply);
    return;
  }

  // Check App Control commands
  const matchedApp = checkAppCommand(text);
  if (matchedApp) {
    const isBn = /[\u0980-\u09FF]/.test(text);
    const reply = isBn
      ? `${matchedApp.name} এর নিরাপদ ওয়েব ভার্সন প্রস্তুত রয়েছে। নিচের বাটনে ক্লিক করে ওপেন করতে পারেন।`
      : `Safe verified web link for ${matchedApp.name} is ready. Click the button below to open.`;
    addMessage('RIFAUL AI', reply, true, matchedApp);
    speakText(reply);
    return;
  }

  // Natural Gemini conversational query
  statusPill.innerText = 'THINKING';
  orbStatusText.innerText = 'RIFAUL AI is processing with Gemini AI...';

  try {
    const response = await fetch('/api/chat', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ prompt: text })
    });

    if (response.ok) {
      const data = await response.json();
      addMessage('RIFAUL AI', data.reply, true);
      speakText(data.reply);
    } else {
      throw new Error('API offline');
    }
  } catch (err) {
    // Intelligent fallback for local standalone demo
    const isBn = /[\u0980-\u09FF]/.test(text);
    const fallbackReply = isBn
      ? `আমি RIFAUL AI। আপনার বার্তাটি পেয়েছি: "${text}"। সম্পূর্ণ এআই উত্তরের জন্য ব্যাকএন্ড Gemini API অথবা নেটিভ অ্যান্ড্রয়েড অ্যাপ ব্যবহার করুন।`
      : `I am RIFAUL AI. Received your message: "${text}". Connect the backend server or open the native Android app for full Gemini 3.8 Flash responses!`;
    addMessage('RIFAUL AI', fallbackReply, true);
    speakText(fallbackReply);
  } finally {
    statusPill.innerText = 'ONLINE';
    orbStatusText.innerText = 'Ready • Ask anything or command an app in বাংলা / English';
  }
}

// Event Listeners
sendBtn.addEventListener('click', () => handleUserSubmit(textInput.value));
textInput.addEventListener('keypress', (e) => {
  if (e.key === 'Enter') handleUserSubmit(textInput.value);
});
micBtn.addEventListener('click', () => {
  if (isListening) stopListening();
  else startListening();
});
stopBtn.addEventListener('click', () => {
  stopListening();
  stopSpeaking();
});
cmdChips.forEach(chip => {
  chip.addEventListener('click', () => {
    handleUserSubmit(chip.getAttribute('data-cmd'));
  });
});
