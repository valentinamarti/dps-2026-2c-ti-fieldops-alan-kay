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

export function button(label, onClick, kind = "") {
    return h("button", {type: "button", class: kind, onClick}, label);
}

export function formOf(fields, submitLabel, onSubmit) {
    return h("form", {
        class: "grid-form",
        onSubmit: event => {
            event.preventDefault();
            onSubmit(new FormData(event.target), event.target);
        },
    }, ...fields, h("button", {type: "submit"}, submitLabel));
}

export function formatMinutes(minutes) {
    const hours = Math.floor(minutes / 60);
    const rest = minutes % 60;
    return hours > 0 ? `${hours} h ${rest} min` : `${rest} min`;
}

export function formatPeriod(period) {
    return `${period.start.replace("T", " ")} → ${period.end.replace("T", " ")}`;
}

export function listOfText(value) {
    return String(value ?? "").split(/[,\n]/).map(item => item.trim()).filter(item => item.length > 0);
}

export function now() {
    return new Date(Date.now() - new Date().getTimezoneOffset() * 60000).toISOString().slice(0, 19);
}
