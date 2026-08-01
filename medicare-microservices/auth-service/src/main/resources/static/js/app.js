// Theme Toggle Logic
function initTheme() {
    const savedTheme = localStorage.getItem('medicare_theme');
    if (savedTheme === 'dark') {
        document.body.classList.add('dark-mode');
        updateThemeBtn(true);
    } else {
        document.body.classList.remove('dark-mode');
        updateThemeBtn(false);
    }
}

function toggleTheme() {
    const isDark = document.body.classList.toggle('dark-mode');
    localStorage.setItem('medicare_theme', isDark ? 'dark' : 'light');
    updateThemeBtn(isDark);
}

function updateThemeBtn(isDark) {
    const themeIcon = document.querySelector('#theme-status-icon');
    if (themeIcon) themeIcon.textContent = isDark ? '🌙' : '☀️';
}

// Sidebar Hover Expansion on MediCare Plus Brand Logo
function initHoverSidebar() {
    const brandTrigger = document.querySelector('.top-header-brand');
    const sidebar = document.querySelector('.app-sidebar');

    if (brandTrigger && sidebar) {
        let hoverTimer = null;

        const openSidebar = () => {
            clearTimeout(hoverTimer);
            sidebar.classList.add('expanded');
        };

        const closeSidebar = () => {
            hoverTimer = setTimeout(() => {
                sidebar.classList.remove('expanded');
            }, 250);
        };

        brandTrigger.addEventListener('mouseenter', openSidebar);
        brandTrigger.addEventListener('mouseleave', closeSidebar);
        sidebar.addEventListener('mouseenter', openSidebar);
        sidebar.addEventListener('mouseleave', closeSidebar);
    }
}

// Auto-trigger native calendar picker when clicking anywhere on a date input
document.addEventListener('click', (e) => {
    if (e.target && e.target.tagName === 'INPUT' && e.target.type === 'date') {
        if (typeof e.target.showPicker === 'function') {
            try {
                e.target.showPicker();
            } catch (err) {
                // Browser security restriction fallback
            }
        }
    }
});

// Auto init theme & sidebar hover on DOM ready
document.addEventListener('DOMContentLoaded', () => {
    initTheme();
    initHoverSidebar();

    const roleButtons = document.querySelectorAll('.role-option');
    const hiddenRole = document.querySelector('#loginRole');
    const submitButton = document.querySelector('.auth-card button[type="submit"]');

    if (roleButtons.length && hiddenRole && submitButton) {
        const labels = {
            chairman: 'Continue as Chairman',
            doctor: 'Continue as Doctor',
            patient: 'Continue as Patient'
        };

        roleButtons.forEach((button) => {
            button.addEventListener('click', () => {
                roleButtons.forEach((item) => item.classList.remove('active'));
                button.classList.add('active');
                hiddenRole.value = button.dataset.role;
                submitButton.textContent = labels[button.dataset.role];
            });
        });
    }
    
    // Auto-init clinical chatbot if trigger button is present
    initChatbot();
});

// Floating Health Assistant Chatbot Functionality
function initChatbot() {
    const triggerBtn = document.querySelector('.chatbot-trigger-btn');
    const panel = document.querySelector('.chatbot-panel');
    const closeBtn = document.querySelector('#chatbot-close');
    const maxBtn = document.querySelector('#chatbot-max');
    const clearBtn = document.querySelector('#chatbot-clear');
    const msgBody = document.querySelector('#chatbot-messages');
    const chatInput = document.querySelector('#chatbot-input-field');
    const sendBtn = document.querySelector('#chatbot-send-btn');
    const notifBubble = document.querySelector('.chatbot-trigger-btn .notification-bubble');

    if (!triggerBtn || !panel || !msgBody || !chatInput) return;

    // Toggle Chat panel
    triggerBtn.addEventListener('click', () => {
        const isOpen = panel.classList.toggle('active');
        if (isOpen) {
            chatInput.focus();
            if (notifBubble) notifBubble.style.display = 'none';
            scrollToBottom();
        }
    });

    // Close Panel Event
    if (closeBtn) {
        closeBtn.addEventListener('click', (e) => {
            e.stopPropagation();
            panel.classList.remove('active');
        });
    }

    // Toggle Maximize Panel size
    if (maxBtn) {
        maxBtn.addEventListener('click', (e) => {
            e.stopPropagation();
            panel.classList.toggle('maximized');
            scrollToBottom();
        });
    }

    // Clear history session
    if (clearBtn) {
        clearBtn.addEventListener('click', (e) => {
            e.stopPropagation();
            if (confirm("Are you sure you want to clear your chat history?")) {
                msgBody.innerHTML = '';
                sessionStorage.removeItem('medicare_chat_history');
                
                // Clear session history on the backend
                fetch('/api/settings/clear-chat', { method: 'POST' }).catch(() => {});
                
                appendWelcomeMessage();
            }
        });
    }

    // Suggested queries click delegation
    document.addEventListener('click', (e) => {
        const chip = e.target.closest('.suggestion-chip');
        if (chip) {
            e.stopPropagation();
            const query = chip.dataset.query || chip.textContent.trim();
            chatInput.value = query;
            handleSend();
        }
    });

    // Copy to clipboard click delegation
    msgBody.addEventListener('click', (e) => {
        const copyBtn = e.target.closest('.copy-msg-btn');
        if (copyBtn) {
            e.stopPropagation();
            const chatMsg = copyBtn.closest('.chat-msg');
            const bubble = chatMsg.querySelector('.msg-bubble');
            const textToCopy = bubble.textContent.trim();
            
            navigator.clipboard.writeText(textToCopy).then(() => {
                const label = copyBtn.querySelector('.copy-label') || copyBtn;
                const prevText = label.textContent;
                label.textContent = "Copied!";
                copyBtn.style.color = "#10b981";
                setTimeout(() => {
                    label.textContent = prevText;
                    copyBtn.style.color = "";
                }, 1500);
            }).catch(err => {
                console.error("Copy failed: ", err);
            });
        }
    });

    // Submit actions
    if (sendBtn) {
        sendBtn.addEventListener('click', handleSend);
    }
    chatInput.addEventListener('keydown', (e) => {
        if (e.key === 'Enter') {
            e.preventDefault();
            handleSend();
        }
    });

    function scrollToBottom() {
        msgBody.scrollTop = msgBody.scrollHeight;
    }

    function getFormattedTime() {
        const now = new Date();
        return now.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    }

    // Markdown Parser
    function translateMarkdownToHtml(text) {
        if (!text) return "";
        
        // Escape HTML tags to protect against Prompt Injection / XSS
        let escaped = text
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#039;");

        const lines = escaped.split('\n');
        let result = [];
        let inList = false;
        let listType = ""; // "ul" or "ol"

        for (let line of lines) {
            let trimmed = line.trim();

            const isUnordered = trimmed.startsWith('- ') || trimmed.startsWith('* ');
            const isOrdered = /^\d+\.\s/.test(trimmed);

            if (inList && !isUnordered && !isOrdered) {
                result.push(`</${listType}>`);
                inList = false;
                listType = "";
            }

            if (trimmed.startsWith('### ')) {
                result.push(`<h3>${parseInlineMarkdown(trimmed.substring(4))}</h3>`);
            } else if (trimmed.startsWith('## ')) {
                result.push(`<h2>${parseInlineMarkdown(trimmed.substring(3))}</h2>`);
            } else if (trimmed.startsWith('# ')) {
                result.push(`<h1>${parseInlineMarkdown(trimmed.substring(2))}</h1>`);
            } else if (isUnordered) {
                if (!inList) {
                    result.push('<ul>');
                    inList = true;
                    listType = "ul";
                }
                result.push(`<li>${parseInlineMarkdown(trimmed.substring(2))}</li>`);
            } else if (isOrdered) {
                if (!inList) {
                    result.push('<ol>');
                    inList = true;
                    listType = "ol";
                }
                const content = trimmed.replace(/^\d+\.\s/, "");
                result.push(`<li>${parseInlineMarkdown(content)}</li>`);
            } else {
                if (trimmed !== "") {
                    result.push(`<p>${parseInlineMarkdown(trimmed)}</p>`);
                }
            }
        }

        if (inList) {
            result.push(`</${listType}>`);
        }

        return result.join('\n');
    }

    function parseInlineMarkdown(text) {
        // Bold: **text**
        let parsed = text.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>');
        // Inline code highlights: `text`
        parsed = parsed.replace(/`(.*?)`/g, '<code style="background: rgba(0,0,0,0.06); padding: 2px 4px; border-radius: 4px; font-weight: 500;">$1</code>');
        return parsed;
    }

    function appendMessage(text, sender) {
        const msgDiv = document.createElement('div');
        msgDiv.className = `chat-msg ${sender}`;

        const bubble = document.createElement('div');
        bubble.className = 'msg-bubble';
        
        if (sender === 'bot') {
            bubble.innerHTML = translateMarkdownToHtml(text);
        } else {
            bubble.textContent = text;
        }

        const metaRow = document.createElement('div');
        metaRow.className = 'chat-msg-meta-row';

        const timeSpan = document.createElement('span');
        timeSpan.className = 'msg-meta';
        timeSpan.textContent = getFormattedTime();
        metaRow.appendChild(timeSpan);

        if (sender === 'bot') {
            const copyBtn = document.createElement('button');
            copyBtn.className = 'copy-msg-btn';
            copyBtn.title = 'Copy response';
            copyBtn.innerHTML = '<svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="9" y="9" width="13" height="13" rx="2" ry="2"/><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/></svg> <span class="copy-label">Copy</span>';
            metaRow.appendChild(copyBtn);
        }

        msgDiv.appendChild(bubble);
        msgDiv.appendChild(metaRow);
        msgBody.appendChild(msgDiv);
        scrollToBottom();
        saveChatHistory();
    }

    function showTypingIndicator() {
        const typingDiv = document.createElement('div');
        typingDiv.className = 'chat-msg bot typing-placeholder';

        const bubble = document.createElement('div');
        bubble.className = 'msg-bubble';
        bubble.innerHTML = `
            <div class="typing-dots">
                <span></span>
                <span></span>
                <span></span>
            </div>
        `;

        typingDiv.appendChild(bubble);
        msgBody.appendChild(typingDiv);
        scrollToBottom();
    }

    function removeTypingIndicator() {
        const indicator = msgBody.querySelector('.typing-placeholder');
        if (indicator) {
            indicator.remove();
        }
    }

    function appendWelcomeMessage() {
        appendMessage("Hello! I'm **Neura** — your healthcare assistant.\nAsk me about symptoms, medicines, lab tests, diet, or hospital services.", "bot");

        // Inject clickable suggestion chips after the welcome bubble
        const chipsContainer = document.createElement('div');
        chipsContainer.className = 'suggestion-chips-container';
        chipsContainer.style.cssText = 'display: flex; flex-wrap: wrap; gap: 8px; padding: 4px 12px 12px; justify-content: center;';

        const suggestions = [
            { label: '🩺 Symptoms of Diabetes', query: 'What are the symptoms of diabetes?' },
            { label: '🔥 First Aid for Burns', query: 'How to give first aid for burns?' },
            { label: '🏥 Hospital OPD Timings', query: 'What are the hospital OPD timings?' },
            { label: '🧪 What is CBC Test?', query: 'What is a CBC blood test and why is it done?' },
            { label: '🥗 Diet for Hypertension', query: 'What diet is recommended for hypertension?' },
            { label: '💊 Paracetamol Info', query: 'What are the uses and side effects of Paracetamol?' }
        ];

        suggestions.forEach(s => {
            const chip = document.createElement('button');
            chip.className = 'suggestion-chip';
            chip.dataset.query = s.query;
            chip.textContent = s.label;
            chipsContainer.appendChild(chip);
        });

        msgBody.appendChild(chipsContainer);
        scrollToBottom();
    }

    function saveChatHistory() {
        sessionStorage.setItem('medicare_chat_history', msgBody.innerHTML);
    }

    function loadChatHistory() {
        const cached = sessionStorage.getItem('medicare_chat_history');
        if (cached) {
            msgBody.innerHTML = cached;
            scrollToBottom();
        } else {
            appendWelcomeMessage();
        }
    }

    function handleSend() {
        const query = chatInput.value.trim();
        if (!query) return;

        appendMessage(query, 'user');
        chatInput.value = '';

        showTypingIndicator();

        fetch('/api/chat', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ message: query })
        })
        .then(res => {
            if (!res.ok) throw new Error("Connection failed");
            return res.json();
        })
        .then(data => {
            removeTypingIndicator();
            appendMessage(data.response, 'bot');
            if (data.remainingQuota !== undefined) {
                updateQuotaDisplay(data.remainingQuota);
            }
        })
        .catch(err => {
            console.error("[Chatbot Error] Connection failed: ", err);
            removeTypingIndicator();
            appendMessage("I apologize, but I am unable to connect to the medical assistant service right now. Please check your network connection or try again later.", "bot");
        });
    }

    function updateQuotaDisplay(remaining) {
        const badge = document.querySelector('#quota-remaining-badge');
        if (badge) {
            badge.textContent = remaining;
            const parent = badge.parentElement;
            if (parent) {
                if (remaining <= 0) {
                    parent.style.color = '#f87171';
                    parent.style.background = 'rgba(248,113,113,0.15)';
                } else if (remaining <= 10) {
                    parent.style.color = '#fbbf24';
                    parent.style.background = 'rgba(251,191,36,0.15)';
                } else {
                    parent.style.color = '#60a5fa';
                    parent.style.background = 'rgba(96,165,250,0.15)';
                }
            }
        }
    }

    function fetchCurrentQuota() {
        fetch('/api/chat/quota')
            .then(res => {
                if (res.ok) return res.json();
                throw new Error("Quota check failed");
            })
            .then(remaining => {
                updateQuotaDisplay(remaining);
            })
            .catch(() => {});
    }

    // Load session chat logs & initial quota status
    loadChatHistory();
    fetchCurrentQuota();
}
