(function () {
    const STORE_KEY = "coffeehouse.articleActions.v1";

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
            state[id] = { favorite: false };
        }
        return state[id];
    }

    function syncButtons(root, id, current) {
        root.querySelectorAll(`[data-article-id="${cssEscape(id)}"] [data-action]`).forEach((button) => {
            const action = button.dataset.action;
            const active = action === "favorite" && current.favorite;

            button.classList.toggle("active", active);
            button.setAttribute("aria-pressed", active ? "true" : "false");
            if (action === "favorite") {
                button.querySelector(".action-label").textContent = current.favorite ? "Saved" : "Save";
            }
        });
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

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", init);
    } else {
        init();
    }
})();
