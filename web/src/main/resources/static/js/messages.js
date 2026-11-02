import {h} from "./dom.js";

const container = () => document.getElementById("messages");

export function clearMessages() {
    container().replaceChildren();
}

export function showError(error) {
    container().replaceChildren(h("div", {class: "message"}, error.message));
    window.scrollTo({top: 0});
}

export function showSuccess(text) {
    container().replaceChildren(h("div", {class: "message success"}, text));
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
