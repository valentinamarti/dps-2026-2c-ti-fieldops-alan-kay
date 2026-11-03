import {renderCatalog} from "./catalog.js";
import {renderExpedition} from "./expedition.js";
import {renderExpeditionList} from "./expedition-list.js";
import {clearMessages, showError} from "./messages.js";

const page = () => document.getElementById("page");

function viewFor(hash) {
    const expedition = hash.match(/^#\/expeditions\/(.+)$/);
    if (expedition) {
        return () => renderExpedition(decodeURIComponent(expedition[1]));
    }
    return hash === "#/catalog" ? renderCatalog : renderExpeditionList;
}

async function route() {
    clearMessages();
    try {
        page().replaceChildren(await viewFor(window.location.hash)());
    } catch (error) {
        showError(error);
    }
}

window.addEventListener("hashchange", route);
route();
