import {api} from "./api.js";
import {button, field, formOf, h, now, panel, selectOf, statusBadge, textInput} from "./dom.js";
import {formatMinutes, formatMoment} from "./format.js";
import {reporting} from "./messages.js";

function decisionView(proposal, names) {
    const decision = proposal.decision;
    return decision ? h("p", {}, `Decided by ${names.resource(decision.responsibleId)}: ${decision.justification}`,
        decision.chosenOptionNumber ? ` · option ${decision.chosenOptionNumber}` : null,
        decision.alternativeExpeditionId ? h("span", {}, " · ", h("a", {href: `#/expeditions/${encodeURIComponent(decision.alternativeExpeditionId)}`}, "see the alternative")) : null) : null;
}

function decisionForm(expedition, proposal, names, refresh) {
    const base = `/replan-proposals/${encodeURIComponent(proposal.id)}`;
    const reject = form => reporting(async () => {
        const data = new FormData(form);
        await api.post(`${base}/rejection`, {responsibleId: data.get("responsibleId"), justification: data.get("justification"), decidedAt: now()});
        await refresh();
    }, "Proposal rejected.");
    const form = formOf([
        field("Option", selectOf("optionNumber", proposal.options.map(option => ({value: option.number, label: `${option.number}. ${option.summary}`})))),
        field("Responsible", selectOf("responsibleId", expedition.responsibleIds.map(id => ({value: id, label: names.resource(id)})))),
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

function incidentText(incident, names) {
    return [
        names.activity(incident.activityId),
        formatMoment(incident.occurredAt),
        incident.resourceId ? `Affected resource: ${names.resource(incident.resourceId)}` : null,
        incident.expectedDelayMinutes ? `Expected delay: ${formatMinutes(incident.expectedDelayMinutes)}` : null,
    ].filter(Boolean).join(" · ");
}

function supersededView(proposal, proposals) {
    const accepted = proposals.find(other => other.id === proposal.supersededByProposalId);
    return accepted ? h("p", {class: "muted"}, `Superseded by the proposal accepted for: ${accepted.incident.description}`) : null;
}

function proposalView(expedition, proposal, proposals, names, refresh) {
    return h("div", {class: "node"},
        h("div", {class: "row"}, statusBadge(proposal.status), h("strong", {}, proposal.incident.description)),
        h("div", {class: "muted"}, incidentText(proposal.incident, names)),
        h("ol", {}, proposal.options.map(option => h("li", {}, option.summary))),
        decisionView(proposal, names),
        supersededView(proposal, proposals),
        proposal.status === "PENDING" ? decisionForm(expedition, proposal, names, refresh) : null);
}

export function proposalsView(expedition, proposals, names, refresh) {
    return panel("Replan proposals", proposals.length > 0
        ? proposals.map(proposal => proposalView(expedition, proposal, proposals, names, refresh))
        : h("p", {class: "muted"}, "No proposals: they appear when an incident is registered."));
}
