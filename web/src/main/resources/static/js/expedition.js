import {api} from "./api.js";
import {badge, button, h, panel, riskBadge, statusBadge, table} from "./dom.js";
import {formatMinutes, formatMoment, formatPeriod, humanize, quantity} from "./format.js";
import {reporting} from "./messages.js";
import {approvalView} from "./approval.js";
import {eventsView, notificationsView, timelineTable} from "./events.js";
import {activitiesOf, itineraryView} from "./itinerary.js";
import {planningView} from "./planning.js";
import {proposalsView} from "./proposals.js";

const TRANSITIONS = {
    DRAFT: [["Send to review", "review", "Sent to review."]],
    IN_REVIEW: [["Back to draft", "draft", "Back to draft."]],
    APPROVED: [["Start execution", "execution", "Execution started."]],
    IN_EXECUTION: [["Suspend", "suspension", "Expedition suspended."], ["Finish", "completion", "Expedition finished."]],
    SUSPENDED: [["Resume", "resumption", "Expedition resumed."], ["Finish", "completion", "Expedition finished."]],
};

function refreshing(id) {
    return async () => {
        document.getElementById("page").replaceChildren(await renderExpedition(id));
    };
}

function expeditionLink(label, id) {
    return id ? h("p", {}, h("a", {href: `#/expeditions/${encodeURIComponent(id)}`}, label)) : null;
}

function lookup(entries) {
    const known = new Map(entries.map(entry => [entry.id, entry.name]));
    return id => known.get(id) ?? id;
}

function headerView(expedition, names, refresh) {
    const base = `/expeditions/${encodeURIComponent(expedition.id)}`;
    return h("div", {},
        h("a", {href: "#/", class: "back"}, "← Expeditions"),
        h("h1", {}, expedition.name),
        h("div", {class: "row"}, statusBadge(expedition.status),
            expedition.staleAlternative ? badge("Stale alternative: the original kept running", "danger") : null),
        h("p", {class: "muted"}, formatPeriod(expedition.period)),
        h("p", {}, expedition.objectives.join(" · ")),
        h("p", {class: "muted"}, `Zones: ${expedition.zoneIds.map(names.zone).join(", ")} · Responsibles: ${expedition.responsibleIds.map(names.resource).join(", ")}`,
            ` · Permits: ${expedition.grantedPermits.join(", ") || "none"}`,
            ` · Risk tolerance: ${humanize(expedition.restrictions.riskTolerance).toLowerCase()}`,
            ` · Max participants: ${expedition.restrictions.maxParticipants}`),
        expeditionLink("See the expedition it replaces", expedition.replacedExpeditionId),
        expeditionLink("See the expedition that replaced it", expedition.replacementId),
        h("div", {class: "row"}, (TRANSITIONS[expedition.status] ?? []).map(([label, path, done]) =>
            button(label, reporting(async () => {
                await api.post(`${base}/${path}`);
                await refresh();
            }, done)))));
}

function consumptionRows(consumption) {
    return consumption.map(item => [humanize(item.category), quantity(item.quantity)]);
}

function estimateView(estimate) {
    return panel("Estimate",
        h("div", {class: "row"}, `Duration ${formatMinutes(estimate.totalDurationMinutes)}`, riskBadge(estimate.overallRisk)),
        table(["Supply", "Estimated consumption"], consumptionRows(estimate.estimatedConsumption)));
}

function reportView(report, names) {
    return panel("Report",
        h("p", {}, `${report.finishedActivities} of ${report.scheduledActivities} activities finished`),
        h("h3", {}, "Actual consumption"),
        table(["Supply", "Consumed"], consumptionRows(report.actualConsumption)),
        h("h3", {}, "Incidents"),
        report.incidents.length > 0
            ? table(["When", "Activity", "What happened", "Resource", "Expected delay"], report.incidents.map(incident =>
                [formatMoment(incident.occurredAt), names.activity(incident.activityId), incident.description,
                    incident.resourceId ? names.resource(incident.resourceId) : "—",
                    incident.expectedDelayMinutes ? formatMinutes(incident.expectedDelayMinutes) : "—"]))
            : h("p", {class: "muted"}, "No incidents."),
        h("h3", {}, "Accepted warnings"),
        report.acceptedWarnings.length > 0
            ? table(["Warning", "Accepted by", "Justification"], report.acceptedWarnings.map(accepted =>
                [accepted.warning.message, names.resource(accepted.responsibleId), accepted.justification]))
            : h("p", {class: "muted"}, "No accepted warnings."),
        h("h3", {}, "Progress"),
        report.progress.length > 0
            ? table(["When", "Activity", "Finished"], report.progress.map(entry =>
                [formatMoment(entry.occurredAt), names.activity(entry.activityId), `${entry.finishedActivities} of ${entry.scheduledActivities}`]))
            : h("p", {class: "muted"}, "No activity finished yet."),
        h("h3", {}, "Event timeline"),
        report.timeline.length > 0 ? timelineTable(report.timeline) : h("p", {class: "muted"}, "No events yet."));
}

export async function renderExpedition(id) {
    const base = `/expeditions/${encodeURIComponent(id)}`;
    const [expedition, itinerary, validation, estimate, report, proposals, events, notifications, staff, vehicles, instruments, supplies, zones] = await Promise.all([
        api.get(base),
        api.get(`${base}/itinerary`),
        api.get(`${base}/validation`),
        api.get(`${base}/estimate`),
        api.get(`${base}/report`),
        api.get(`${base}/replan-proposals`),
        api.get(`${base}/events`),
        api.get(`${base}/notifications`),
        api.get("/staff"),
        api.get("/vehicles"),
        api.get("/instruments"),
        api.get("/supplies"),
        api.get("/zones"),
    ]);
    const refresh = refreshing(id);
    const names = {
        resource: lookup([...staff, ...vehicles, ...instruments, ...supplies]),
        zone: lookup(zones),
        activity: lookup(activitiesOf(itinerary.nodes)),
    };
    return h("div", {},
        headerView(expedition, names, refresh),
        await itineraryView(expedition, itinerary, names, refresh),
        expedition.status === "DRAFT" ? await planningView(expedition, activitiesOf(itinerary.nodes), refresh) : null,
        approvalView(expedition, validation, names, refresh),
        proposalsView(expedition, proposals, names, refresh),
        eventsView(events, refresh),
        notificationsView(notifications, names, refresh),
        estimateView(estimate),
        reportView(report, names));
}
