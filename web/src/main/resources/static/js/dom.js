import {humanize} from "./format.js";

export function h(tag, attributes = {}, ...children) {
    const element = document.createElement(tag);
    Object.entries(attributes).forEach(([name, value]) => applyAttribute(element, name, value));
    children.flat().filter(child => child !== null && child !== undefined && child !== false)
        .forEach(child => element.append(child));
    return element;
}

function applyAttribute(element, name, value) {
    if (name.startsWith("on")) {
        element.addEventListener(name.slice(2).toLowerCase(), value);
    } else if (typeof value === "boolean") {
        element[name] = value;
    } else if (value !== null && value !== undefined) {
        element.setAttribute(name, value);
    }
}

export function panel(title, ...children) {
    return h("section", {class: "panel"}, h("h2", {}, title), ...children);
}

export function field(label, control) {
    return h("label", {}, label, control);
}

export function textInput(name, attributes = {}) {
    return h("input", {name, ...attributes});
}

export function selectOf(name, options, attributes = {}) {
    return h("select", {name, ...attributes}, options.map(option => {
        const {value, label} = typeof option === "object" ? option : {value: option, label: option};
        return h("option", {value}, label);
    }));
}

export function table(headers, rows) {
    return h("div", {class: "table-wrap"}, h("table", {},
        h("thead", {}, h("tr", {}, headers.map(header => h("th", {}, header)))),
        h("tbody", {}, rows.map(row => h("tr", {}, row.map(cell => h("td", {}, cell)))))));
}

export function badge(text, kind = "") {
    return h("span", {class: `badge ${kind}`}, text);
}

const STATUS_KINDS = {
    IN_REVIEW: "info",
    APPROVED: "info",
    IN_EXECUTION: "accent",
    IN_PROGRESS: "accent",
    SUSPENDED: "warn",
    FINISHED: "ok",
    ACCEPTED: "ok",
    DELIVERED: "ok",
    REJECTED: "danger",
    FAILED: "danger",
};

const RISK_KINDS = {MEDIUM: "info", HIGH: "warn", EXTREME: "danger"};

export function statusBadge(status) {
    return badge(humanize(status), STATUS_KINDS[status] ?? "");
}

export function riskBadge(level) {
    return badge(`${humanize(level)} risk`, RISK_KINDS[level] ?? "");
}

const remembered = new Map();

export function disclosure(key, openByDefault, header, ...children) {
    return h("details", {
        open: remembered.get(key) ?? openByDefault,
        onToggle: event => remembered.set(key, event.target.open),
    }, h("summary", {}, header), ...children);
}

export function button(label, onClick, kind = "") {
    return h("button", {type: "button", class: kind, onClick}, label);
}

export function checkboxes(name, options) {
    return h("div", {class: "checks"}, options.map(option => h("label", {class: "inline"},
        h("input", {type: "checkbox", name, value: option.id}), option.name)));
}

export function submitButton(label) {
    return h("button", {type: "submit"}, label);
}

export function formOf(fields, submitLabel, onSubmit) {
    return h("form", {
        class: "grid-form",
        onSubmit: event => {
            event.preventDefault();
            onSubmit(new FormData(event.target), event.target);
        },
    }, ...fields, submitLabel ? submitButton(submitLabel) : null);
}

export function listOfText(value) {
    return String(value ?? "").split(/[,\n]/).map(item => item.trim()).filter(item => item.length > 0);
}

export function now() {
    return new Date(Date.now() - new Date().getTimezoneOffset() * 60000).toISOString().slice(0, 19);
}
