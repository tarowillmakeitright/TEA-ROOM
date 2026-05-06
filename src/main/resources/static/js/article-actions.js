(function () {
    const STORE_KEY = "coffeehouse.articleActions.v1";
    const SWIPE_THRESHOLD = 110;

    function loadState() {
        try {
            return JSON.parse(localStorage.getItem(STORE_KEY)) || {};
        } catch (e) {
            return {};
        }
    }

    function saveState(state) {
        localStorage.setItem(STORE_KEY, JSON.stringify(state));
    }

    function articleState(state, id) {
        if (!state[id]) {
            state[id] = { reaction: null, favorite: false };
        }
        return state[id];
    }

    function syncButtons(root, id, current) {
        root.querySelectorAll(`[data-article-id="${cssEscape(id)}"] [data-action]`).forEach((button) => {
            const action = button.dataset.action;
            const active =
                (action === "good" && current.reaction === "good") ||
                (action === "bad" && current.reaction === "bad") ||
                (action === "favorite" && current.favorite);

            button.classList.toggle("active", active);
            button.setAttribute("aria-pressed", active ? "true" : "false");
            if (action === "favorite") {
                button.querySelector(".action-label").textContent = current.favorite ? "Saved" : "Save";
            }
        });
    }

    function setReaction(state, id, reaction) {
        const current = articleState(state, id);
        current.reaction = current.reaction === reaction ? null : reaction;
        saveState(state);
        syncButtons(document, id, current);
    }

    function getShareUrl(button) {
        const value = button.dataset.sourceUrl || button.dataset.shareUrl || window.location.href;
        return new URL(value, window.location.origin).href;
    }

    async function share(button) {
        const title = button.dataset.shareTitle || document.title;
        const url = getShareUrl(button);
        if (navigator.share) {
            await navigator.share({ title, url });
            return;
        }
        await navigator.clipboard.writeText(url);
        const label = button.querySelector(".action-label");
        const previous = label.textContent;
        label.textContent = "Copied";
        window.setTimeout(() => {
            label.textContent = previous;
        }, 1600);
    }

    function cssEscape(value) {
        if (window.CSS && CSS.escape) {
            return CSS.escape(value);
        }
        return value.replace(/["\\]/g, "\\$&");
    }

    function init() {
        const state = loadState();
        document.querySelectorAll("[data-article-id]").forEach((article) => {
            const id = article.dataset.articleId;
            syncButtons(article.ownerDocument, id, articleState(state, id));
        });
        initSwipeDecks(state);

        document.addEventListener("click", async (event) => {
            const button = event.target.closest("[data-action]");
            if (!button) {
                return;
            }

            event.preventDefault();
            event.stopPropagation();

            const article = button.closest("[data-article-id]");
            if (!article) {
                return;
            }

            const id = article.dataset.articleId;
            const current = articleState(state, id);
            const action = button.dataset.action;

            if (action === "good" || action === "bad") {
                setReaction(state, id, action);
                dismissCard(article, action);
                return;
            }

            if (action === "favorite") {
                current.favorite = !current.favorite;
                current.title = button.dataset.shareTitle || article.dataset.articleTitle || "";
                current.url = getShareUrl(button);
                saveState(state);
                syncButtons(document, id, current);
                return;
            }

            if (action === "share") {
                try {
                    await share(button);
                } catch (e) {
                    // User cancellation is normal for native share sheets.
                }
            }
        });
    }

    function initSwipeDecks(state) {
        document.querySelectorAll(".swipe-deck").forEach((deck) => {
            const cards = Array.from(deck.querySelectorAll("[data-swipe-card]"));
            cards.forEach((card, index) => {
                card.style.zIndex = String(cards.length - index);
                card.style.transform = deckTransform(index);
                card.classList.toggle("active", index === 0);
                attachSwipe(card, state);
            });
        });
    }

    function deckTransform(index) {
        const depth = Math.min(index, 3);
        return `translateY(${depth * 0.55}rem) scale(${1 - depth * 0.025})`;
    }

    function attachSwipe(card, state) {
        let startX = 0;
        let startY = 0;
        let currentX = 0;
        let dragging = false;

        card.addEventListener("pointerdown", (event) => {
            if (!card.classList.contains("active") || event.target.closest("a, button, [data-action]")) {
                return;
            }
            startX = event.clientX;
            startY = event.clientY;
            currentX = 0;
            dragging = true;
            card.setPointerCapture(event.pointerId);
            card.classList.add("dragging");
        });

        card.addEventListener("pointermove", (event) => {
            if (!dragging) {
                return;
            }
            currentX = event.clientX - startX;
            const currentY = event.clientY - startY;
            if (Math.abs(currentY) > Math.abs(currentX) && Math.abs(currentY) > 18) {
                dragging = false;
                resetCard(card);
                return;
            }
            applyDrag(card, currentX);
        });

        card.addEventListener("pointerup", () => {
            if (!dragging) {
                return;
            }
            dragging = false;
            if (Math.abs(currentX) >= SWIPE_THRESHOLD) {
                const reaction = currentX > 0 ? "good" : "bad";
                setReaction(state, card.dataset.articleId, reaction);
                dismissCard(card, reaction);
                return;
            }
            resetCard(card);
        });

        card.addEventListener("pointercancel", () => {
            dragging = false;
            resetCard(card);
        });
    }

    function applyDrag(card, x) {
        const rotation = Math.max(-12, Math.min(12, x / 14));
        card.style.transition = "none";
        card.style.transform = `translateX(${x}px) rotate(${rotation}deg)`;
        card.classList.toggle("swiping-good", x > 35);
        card.classList.toggle("swiping-bad", x < -35);
    }

    function resetCard(card) {
        card.style.transition = "";
        card.style.transform = deckTransform(0);
        card.classList.remove("dragging", "swiping-good", "swiping-bad");
    }

    function dismissCard(card, reaction) {
        if (!card.matches("[data-swipe-card]") || card.classList.contains("dismissed")) {
            return;
        }
        const direction = reaction === "good" ? 1 : -1;
        card.style.transition = "transform 220ms ease, opacity 220ms ease";
        card.style.transform = `translateX(${direction * 130}%) rotate(${direction * 16}deg)`;
        card.classList.add("dismissed");
        card.classList.remove("active", "swiping-good", "swiping-bad");

        const deck = card.closest(".swipe-deck");
        window.setTimeout(() => activateNextCard(deck), 180);
    }

    function activateNextCard(deck) {
        if (!deck) {
            return;
        }
        const remaining = Array.from(deck.querySelectorAll("[data-swipe-card]:not(.dismissed)"));
        remaining.forEach((card, index) => {
            card.classList.toggle("active", index === 0);
            card.style.zIndex = String(remaining.length - index);
            card.style.transition = "";
            card.style.transform = deckTransform(index);
        });
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", init);
    } else {
        init();
    }
})();
