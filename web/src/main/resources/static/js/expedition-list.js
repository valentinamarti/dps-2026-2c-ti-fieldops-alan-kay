import {api} from "./api.js";
import {badge, field, formatPeriod, formOf, h, listOfText, panel, selectOf, table, textInput} from "./dom.js";
import {reporting} from "./messages.js";

const RISK_LEVELS = ["LOW", "MEDIUM", "HIGH", "EXTREME"];

function linkTo(expedition) {
    return h("a", {href: `#/expeditions/${encodeURIComponent(expedition.id)}`}, expedition.name);
}

function checkboxes(name, options) {
    return h("div", {}, options.map(option => h("label", {class: "inline"},
        h("input", {type: "checkbox", name, value: option.id}), `${option.name} (${option.id})`)));
}

function creationForm(zones, staff) {
    return formOf([
        field("Name", textInput("name", {required: true})),
        field("Start", textInput("start", {type: "datetime-local", required: true})),
        field("End", textInput("end", {type: "datetime-local", required: true})),
        field("Objectives (one per line)", h("textarea", {name: "objectives", rows: 2, required: true})),
        field("Zones", checkboxes("zoneIds", zones)),
        field("Responsibles", checkboxes("responsibleIds", staff)),
        field("Granted permits (comma separated)", textInput("grantedPermits")),
        field("Max participants", textInput("maxParticipants", {type: "number", min: "1", value: "10", required: true})),
        field("Risk tolerance", selectOf("riskTolerance", RISK_LEVELS)),
    ], "Create expedition", reporting(async data => {
        const created = await api.post("/expeditions", {
            name: data.get("name"),
            period: {start: data.get("start"), end: data.get("end")},
            objectives: listOfText(data.get("objectives")),
            zoneIds: data.getAll("zoneIds"),
            responsibleIds: data.getAll("responsibleIds"),
            grantedPermits: listOfText(data.get("grantedPermits")),
            restrictions: {maxParticipants: Number(data.get("maxParticipants")), riskTolerance: data.get("riskTolerance")},
        });
        window.location.hash = `#/expeditions/${encodeURIComponent(created.id)}`;
    }));
}

export async function renderExpeditionList() {
    const [expeditions, zones, staff] = await Promise.all([api.get("/expeditions"), api.get("/zones"), api.get("/staff")]);
    return h("div", {},
        h("h1", {}, "Expeditions"),
        panel("All expeditions", expeditions.length > 0
            ? table(["Name", "Status", "Period"], expeditions.map(expedition => [
                linkTo(expedition),
                h("span", {}, badge(expedition.status), expedition.staleAlternative ? badge("stale alternative", "danger") : null),
                formatPeriod(expedition.period),
            ]))
            : h("p", {class: "muted"}, "There are no expeditions yet.")),
        panel("New expedition", creationForm(zones, staff)));
}
