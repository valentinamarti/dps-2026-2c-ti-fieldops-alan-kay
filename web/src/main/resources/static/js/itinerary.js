import {api} from "./api.js";
import {disclosure, field, formOf, h, now, panel, riskBadge, selectOf, statusBadge, textInput} from "./dom.js";
import {counted, formatMinutes, formatMoment, formatPeriod, humanize, people, quantity} from "./format.js";
import {reporting} from "./messages.js";

export function activitiesOf(nodes) {
    return nodes.flatMap(node => node.activity ? [node.activity] : activitiesOf(node.children ?? []));
}

function requirementsText(activity) {
    const staff = activity.requiredStaff.map(requirement => `${people(requirement.headcount)}${requirement.certifications.length ? ` (${requirement.certifications.join(", ")})` : ""}`);
    const equipment = activity.requiredEquipment.map(requirement => counted(requirement.count, requirement.category));
    const supplies = activity.requiredSupplies.map(requirement => `${quantity(requirement.quantity)} of ${requirement.category}`);
    return [...staff, ...equipment, ...supplies].join(" · ");
}

function assignmentForm(base, kind, label, options, refresh) {
    return options.length > 0 ? formOf([field(label, selectOf("resourceId", options))], "Assign", reporting(async data => {
        await api.post(`${base}/${kind}`, {resourceId: data.get("resourceId")});
        await refresh();
    }, "Resource assigned.")) : h("p", {class: "muted"}, `No available candidates: ${label}.`);
}

function option(candidate) {
    return {value: candidate.id, label: candidate.name};
}

async function assignmentsView(expedition, activity, base, refresh) {
    const candidates = await api.get(`${base}/candidates`);
    const staff = candidates.staff.flatMap(entry => entry.candidates).map(option);
    const equipment = candidates.equipment.flatMap(entry => [...entry.vehicles, ...entry.instruments]).map(option);
    const supplies = candidates.supplies.flatMap(entry => entry.candidates).map(option);
    return disclosure(`${expedition.id}:${activity.id}:assign`, false, "Assign resources",
        assignmentForm(base, "staff", "Staff", staff, refresh),
        assignmentForm(base, "equipment", "Equipment", equipment, refresh),
        assignmentForm(base, "supplies", "Supplies", supplies, refresh));
}

function trackingSummary(tracking) {
    const moments = [
        tracking.startedAt ? `Started ${formatMoment(tracking.startedAt)}` : null,
        tracking.finishedAt ? `finished ${formatMoment(tracking.finishedAt)}` : null,
        tracking.result ? `result: ${tracking.result}` : null,
    ].filter(Boolean);
    return [
        moments.length > 0 ? h("div", {class: "muted"}, moments.join(" · ")) : null,
        tracking.observations.length > 0
            ? h("ul", {}, tracking.observations.map(observation => h("li", {}, `${formatMoment(observation.recordedAt)}: ${observation.note}`)))
            : null,
    ];
}

function trackingActions(expedition, activity, base, names, refresh) {
    const status = activity.tracking.status;
    const incidents = `/expeditions/${encodeURIComponent(expedition.id)}/incidents`;
    const start = formOf([], "Start", reporting(async () => {
        await api.post(`${base}/start`, {at: now()});
        await refresh();
    }, "Activity started."));
    const finish = formOf([field("Result", textInput("result", {required: true}))], "Finish", reporting(async data => {
        await api.post(`${base}/completion`, {at: now(), result: data.get("result")});
        await refresh();
    }, "Activity finished."));
    const observe = formOf([field("Observation", textInput("note", {required: true}))], "Record observation", reporting(async data => {
        await api.post(`${base}/observations`, {note: data.get("note"), recordedAt: now()});
        await refresh();
    }, "Observation recorded."));
    const resources = assignedIds(activity).map(id => ({value: id, label: names.resource(id)}));
    const incident = formOf([
        field("Incident", textInput("description", {required: true})),
        field("Affected resource", selectOf("resourceId", [{value: "", label: "None"}, ...resources])),
        field("Expected delay (minutes)", textInput("delay", {type: "number", min: "1"})),
    ], "Register incident", reporting(async data => {
        await api.post(incidents, {
            activityId: activity.id,
            description: data.get("description"),
            occurredAt: now(),
            resourceId: data.get("resourceId") || null,
            expectedDelayMinutes: data.get("delay") ? Number(data.get("delay")) : null,
        });
        await refresh();
    }, "Incident registered: a replan proposal is waiting for a decision."));
    return disclosure(`${expedition.id}:${activity.id}:track`, false, "Track",
        status === "PENDING" ? [start, incident] : [finish, observe, incident]);
}

function assignedIds(activity) {
    return [...activity.assignedStaffIds, ...activity.assignedEquipmentIds, ...activity.assignedSupplyIds];
}

function showsTracking(expedition, activity) {
    return expedition.status === "IN_EXECUTION" || activity.tracking.status !== "PENDING";
}

function detail(label, value) {
    return h("div", {class: "detail"}, h("span", {class: "detail-label"}, label), h("span", {}, value));
}

async function activityView(expedition, node, names, refresh) {
    const activity = node.activity;
    const base = `/expeditions/${encodeURIComponent(expedition.id)}/activities/${encodeURIComponent(activity.id)}`;
    return h("div", {class: "node"},
        h("div", {class: "row"}, h("strong", {}, activity.name), riskBadge(activity.estimatedRisk),
            showsTracking(expedition, activity) ? statusBadge(activity.tracking.status) : null),
        h("div", {class: "muted"}, `${formatPeriod(activity.scheduledPeriod)} · estimated ${formatMinutes(activity.estimatedDurationMinutes)}`),
        detail("Allowed window", formatPeriod(activity.timeWindow)),
        detail("Needs", requirementsText(activity) || "Nothing"),
        activity.requiredCertifications.length > 0 ? detail("Certifications", activity.requiredCertifications.join(", ")) : null,
        activity.requiredPermits.length > 0 ? detail("Permits", activity.requiredPermits.join(", ")) : null,
        detail("Assigned", assignedIds(activity).map(names.resource).join(", ") || "Nothing yet"),
        activity.dependencyIds.length > 0 ? detail("Depends on", activity.dependencyIds.map(names.activity).join(", ")) : null,
        trackingSummary(activity.tracking),
        expedition.status === "DRAFT" ? await assignmentsView(expedition, activity, base, refresh) : null,
        expedition.status === "IN_EXECUTION" && activity.tracking.status !== "FINISHED" ? trackingActions(expedition, activity, base, names, refresh) : null);
}

function collapsedSummary(expedition, node) {
    const activities = activitiesOf([node]);
    const list = `${activities.length === 1 ? "1 activity" : `${activities.length} activities`}: ${activities.map(activity => activity.name).join(", ")}`;
    const finished = activities.filter(activity => activity.tracking.status === "FINISHED").length;
    const started = activities.some(activity => showsTracking(expedition, activity));
    return h("div", {class: "when-collapsed"}, started ? `${list} · ${finished} of ${activities.length} finished` : list);
}

async function nodeView(expedition, node, names, refresh, path) {
    if (node.activity) {
        return activityView(expedition, node, names, refresh);
    }
    const children = await Promise.all((node.children ?? []).map((child, index) => nodeView(expedition, child, names, refresh, `${path}/${index}`)));
    return h("div", {class: "node block"}, disclosure(`${expedition.id}:${path}`, true,
        h("div", {class: "block-header"},
            h("div", {class: "row"}, h("strong", {}, `${humanize(node.type)} block`), riskBadge(node.estimatedRisk)),
            h("div", {class: "muted"}, `${formatPeriod(node.scheduledPeriod)} · estimated ${formatMinutes(node.estimatedDurationMinutes)}`),
            collapsedSummary(expedition, node)),
        ...children));
}

export async function itineraryView(expedition, itinerary, names, refresh) {
    const nodes = await Promise.all(itinerary.nodes.map((node, index) => nodeView(expedition, node, names, refresh, `${index}`)));
    return panel("Itinerary", nodes.length > 0 ? nodes : h("p", {class: "muted"}, "Nothing scheduled yet."));
}
