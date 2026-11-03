import {api} from "./api.js";
import {badge, button, field, formOf, h, now, panel, selectOf, textInput} from "./dom.js";
import {reporting} from "./messages.js";

function decisionView(proposal) {
    const decision = proposal.decision;
    return decision ? h("p", {}, `Decided by ${decision.responsibleId}: ${decision.justification}`,
        decision.chosenOptionNumber ? ` · option ${decision.chosenOptionNumber}` : null,
        decision.alternativeExpeditionId ? h("span", {}, " · ", h("a", {href: `#/expeditions/${encodeURIComponent(decision.alternativeExpeditionId)}`}, "see the alternative")) : null) : null;
}

function decisionForm(expedition, proposal, refresh) {
    const base = `/replan-proposals/${encodeURIComponent(proposal.id)}`;
    const reject = form => reporting(async () => {
        const data = new FormData(form);
        await api.post(`${base}/rejection`, {responsibleId: data.get("responsibleId"), justification: data.get("justification"), decidedAt: now()});
        await refresh();
    });
    const form = formOf([
        field("Option", selectOf("optionNumber", proposal.options.map(option => ({value: option.number, label: `${option.number}. ${option.summary}`})))),
        field("Responsible", selectOf("responsibleId", expedition.responsibleIds)),
        field("Justification", textInput("justification", {required: true})),
    ], "Accept option", reporting(async data => {
        const alternative = await api.post(`${base}/acceptance`, {
            optionNumber: Number(data.get("optionNumber")),
            responsibleId: data.get("responsibleId"),
            justification: data.get("justification"),
            decidedAt: now(),
        });
        window.location.hash = `#/expeditions/${encodeURIComponent(alternative.id)}`;
    }));
    form.append(button("Reject", () => {
        if (form.reportValidity()) {
            reject(form)();
        }
    }, "danger"));
    return form;
}

function proposalView(expedition, proposal, refresh) {
    return h("div", {class: "node"},
        h("div", {class: "row"}, badge(proposal.status), h("strong", {}, proposal.incident.description)),
        h("div", {class: "muted"}, `Activity ${proposal.incident.activityId} · ${proposal.incident.occurredAt.replace("T", " ")}`),
        h("ol", {}, proposal.options.map(option => h("li", {}, option.summary))),
        decisionView(proposal),
        proposal.status === "PENDING" ? decisionForm(expedition, proposal, refresh) : null);
}

export function proposalsView(expedition, proposals, refresh) {
    return panel("Replan proposals", proposals.length > 0
        ? proposals.map(proposal => proposalView(expedition, proposal, refresh))
        : h("p", {class: "muted"}, "No proposals: they appear when an incident is registered."));
}
