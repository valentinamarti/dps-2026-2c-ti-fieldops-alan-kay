import {h} from "./dom.js";

const SUCCESS_DURATION_MS = 3000;

const container = () => document.getElementById("messages");

let dismissal;

function show(...messages) {
    clearTimeout(dismissal);
    container().replaceChildren(...messages);
}

export function clearMessages() {
    show();
}

export function showError(error) {
    show(h("div", {class: "message"}, error.message));
    window.scrollTo({top: 0});
}

export function showSuccess(text) {
    show(h("div", {class: "message success"}, text));
    dismissal = setTimeout(clearMessages, SUCCESS_DURATION_MS);
}

export function reporting(action, successText) {
    return async (...args) => {
        try {
            clearMessages();
            await action(...args);
            if (successText) {
                showSuccess(successText);
            }
        } catch (error) {
            showError(error);
        }
    };
}
