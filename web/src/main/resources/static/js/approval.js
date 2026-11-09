import {api} from "./api.js";
import {field, formOf, h, panel, selectOf, textInput} from "./dom.js";
import {reporting} from "./messages.js";

function findingsView(findings, kind, emptyText) {
    return findings.length > 0
        ? h("ul", {}, findings.map(finding => h("li", {class: kind}, finding.message)))
        : h("p", {class: "muted"}, emptyText);
}

function warningFields(warnings) {
    return warnings.map((warning, index) => field(`Justification: ${warning.message}`,
        textInput(`justification-${index}`, {required: true})));
}

function acceptancesFrom(data, warnings) {
    return warnings.map((warning, index) => ({
        rule: warning.rule,
        subject: warning.subject,
        responsibleId: data.get("responsibleId"),
        justification: data.get(`justification-${index}`),
    }));
}

function approvalForm(expedition, validation, names, refresh) {
    return formOf([
        field("Responsible who accepts", selectOf("responsibleId", expedition.responsibleIds.map(id => ({value: id, label: names.resource(id)})))),
        ...warningFields(validation.warnings),
    ], "Approve", reporting(async data => {
        await api.post(`/expeditions/${encodeURIComponent(expedition.id)}/approval`, {
            acceptedWarnings: acceptancesFrom(data, validation.warnings),
        });
        await refresh();
    }, "Expedition approved."));
}

export function approvalView(expedition, validation, names, refresh) {
    return panel("Validation",
        h("p", {class: validation.canBeApproved ? "ok" : "critical"},
            validation.canBeApproved ? "No critical errors." : "It has critical errors and cannot be approved."),
        h("h3", {}, "Critical errors"),
        findingsView(validation.criticals, "critical", "None."),
        h("h3", {}, "Warnings"),
        findingsView(validation.warnings, "warning", "None."),
        expedition.status === "IN_REVIEW" ? approvalForm(expedition, validation, names, refresh) : null);
}
