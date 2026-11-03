import {api} from "./api.js";
import {badge, button, formatMinutes, formatPeriod, h, panel, table} from "./dom.js";
import {reporting} from "./messages.js";
import {approvalView} from "./approval.js";
import {itineraryView} from "./itinerary.js";
import {planningView} from "./planning.js";
import {proposalsView} from "./proposals.js";

const TRANSITIONS = {
    DRAFT: [["Send to review", "review"]],
    IN_REVIEW: [["Back to draft", "draft"]],
    APPROVED: [["Start execution", "execution"]],
    IN_EXECUTION: [["Suspend", "suspension"], ["Finish", "completion"]],
    SUSPENDED: [["Resume", "resumption"]],
};

function refreshing(id) {
    return async () => {
        document.getElementById("page").replaceChildren(await renderExpedition(id));
    };
}

function expeditionLink(label, id) {
    return id ? h("p", {}, `${label}: `, h("a", {href: `#/expeditions/${encodeURIComponent(id)}`}, id)) : null;
}

function headerView(expedition, refresh) {
    const base = `/expeditions/${encodeURIComponent(expedition.id)}`;
    return h("div", {},
        h("h1", {}, expedition.name),
        h("div", {class: "row"}, badge(expedition.status),
            expedition.staleAlternative ? badge("stale alternative: the original kept running", "danger") : null),
        h("p", {class: "muted"}, formatPeriod(expedition.period)),
        h("p", {}, expedition.objectives.join(" · ")),
        h("p", {class: "muted"}, `Zones: ${expedition.zoneIds.join(", ")} · Responsibles: ${expedition.responsibleIds.join(", ")}`,
            ` · Permits: ${expedition.grantedPermits.join(", ") || "none"}`,
            ` · Risk tolerance: ${expedition.restrictions.riskTolerance}`),
        expeditionLink("Replaces", expedition.replacedExpeditionId),
        expeditionLink("Replaced by", expedition.replacementId),
        h("div", {class: "row"}, (TRANSITIONS[expedition.status] ?? []).map(([label, path]) =>
            button(label, reporting(async () => {
                await api.post(`${base}/${path}`);
                await refresh();
            })))));
}

function consumptionRows(consumption) {
    return consumption.map(item => [item.category, `${item.quantity.amount} ${item.quantity.unit}`]);
}

function estimateView(estimate) {
    return panel("Estimate",
        h("p", {}, `Duration ${formatMinutes(estimate.totalDurationMinutes)} · Overall risk ${estimate.overallRisk}`),
        table(["Supply", "Estimated consumption"], consumptionRows(estimate.estimatedConsumption)));
}

function reportView(report) {
    return panel("Report",
        h("p", {}, `${report.finishedActivities} of ${report.scheduledActivities} activities finished`),
        h("h3", {}, "Actual consumption"),
        table(["Supply", "Consumed"], consumptionRows(report.actualConsumption)),
        h("h3", {}, "Incidents"),
        report.incidents.length > 0
            ? table(["When", "Activity", "What happened"], report.incidents.map(incident =>
                [incident.occurredAt.replace("T", " "), incident.activityId, incident.description]))
            : h("p", {class: "muted"}, "No incidents."),
        h("h3", {}, "Accepted warnings"),
        report.acceptedWarnings.length > 0
            ? table(["Warning", "Accepted by", "Justification"], report.acceptedWarnings.map(accepted =>
                [accepted.warning.message, accepted.responsibleId, accepted.justification]))
            : h("p", {class: "muted"}, "No accepted warnings."));
}

export async function renderExpedition(id) {
    const base = `/expeditions/${encodeURIComponent(id)}`;
    const [expedition, itinerary, validation, estimate, report, proposals] = await Promise.all([
        api.get(base),
        api.get(`${base}/itinerary`),
        api.get(`${base}/validation`),
        api.get(`${base}/estimate`),
        api.get(`${base}/report`),
        api.get(`${base}/replan-proposals`),
    ]);
    const refresh = refreshing(id);
    return h("div", {},
        headerView(expedition, refresh),
        await itineraryView(expedition, itinerary, refresh),
        expedition.status === "DRAFT" ? await planningView(expedition, refresh) : null,
        approvalView(expedition, validation, refresh),
        proposalsView(expedition, proposals, refresh),
        estimateView(estimate),
        reportView(report));
}
