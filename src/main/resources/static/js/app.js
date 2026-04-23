(() => {
    const shell = document.querySelector(".erp-shell");
    const toggle = document.querySelector("[data-erp-sidebar-toggle]");
    const toastContainer = document.querySelector(".erp-toast-container");

    if (shell && toggle) {
        toggle.addEventListener("click", () => {
            shell.classList.toggle("sidebar-open");
        });
    }

    const toastMeta = {
        success: { title: "Sucesso", icon: "bi-check-circle-fill" },
        warning: { title: "Atenção", icon: "bi-exclamation-triangle-fill" },
        error: { title: "Erro", icon: "bi-x-octagon-fill" },
        info: { title: "Informação", icon: "bi-info-circle-fill" }
    };

    const createToast = (type, message) => {
        if (!toastContainer || !message) {
            return;
        }

        const normalizedType = toastMeta[type] ? type : "info";
        const meta = toastMeta[normalizedType];
        const toast = document.createElement("div");
        toast.className = `erp-toast ${normalizedType}`;
        toast.innerHTML = `
            <span class="erp-toast-icon"><i class="bi ${meta.icon}"></i></span>
            <div class="erp-toast-content">
                <div class="erp-toast-title">${meta.title}</div>
                <div class="erp-toast-message"></div>
            </div>
            <button type="button" class="erp-toast-close" aria-label="Fechar">×</button>
        `;
        const messageNode = toast.querySelector(".erp-toast-message");
        const closeButton = toast.querySelector(".erp-toast-close");
        messageNode.textContent = message;

        let removed = false;
        const removeToast = () => {
            if (removed) {
                return;
            }
            removed = true;
            toast.classList.add("is-closing");
            window.setTimeout(() => toast.remove(), 180);
        };

        closeButton.addEventListener("click", removeToast);
        toastContainer.appendChild(toast);
        window.setTimeout(removeToast, 6000);
    };

    window.erpToast = (type, message) => {
        createToast(type, message);
    };

    document.querySelectorAll(".erp-toast-source").forEach((source) => {
        const type = source.dataset.toastType || "info";
        const message = source.dataset.toastMessage || source.textContent;
        createToast(type, message.trim());
    });
})();
