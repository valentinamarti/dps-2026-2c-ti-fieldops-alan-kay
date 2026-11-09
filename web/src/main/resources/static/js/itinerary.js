import {api} from "./api.js";
import {badge, field, formatMinutes, formatPeriod, formOf, h, now, panel, selectOf, textInput} from "./dom.js";
import {reporting} from "./messages.js";

function requirementsText(activity) {
    const staff = activity.requiredStaff.map(requirement => `${requirement.headcount} people${requirement.certifications.length ? ` (${requirement.certifications.join(", ")})` : ""}`);
    const equipment = activity.requiredEquipment.map(requirement => `${requirement.count} ${requirement.category}`);
    const supplies = activity.requiredSupplies.map(requirement => `${requirement.quantity.amount} ${requirement.quantity.unit} of ${requirement.category}`);
    return [...staff, ...equipment, ...supplies].join(" · ");
}

function assignmentForm(activity, base, kind, label, options, refresh) {
    return options.length > 0 ? formOf([field(label, selectOf("resourceId", options))], "Assign", reporting(async data => {
        await api.post(`${base}/${kind}`, {resourceId: data.get("resourceId")});
        await refresh();
    })) : h("p", {class: "muted"}, `No available candidates: ${label}.`);
}

function option(candidate) {
    return {value: candidate.id, label: `${candidate.name} (${candidate.id})`};
}

async function assignmentsView(activity, base, refresh) {
    const candidates = await api.get(`${base}/candidates`);
    const staff = candidates.staff.flatMap(entry => entry.candidates).map(option);
    const equipment = candidates.equipment.flatMap(entry => [...entry.vehicles, ...entry.instruments]).map(option);
    const supplies = candidates.supplies.flatMap(entry => entry.candidates).map(option);
    return h("details", {}, h("summary", {}, "Assign resources"),
        assignmentForm(activity, base, "staff", "Staff", staff, refresh),
        assignmentForm(activity, base, "equipment", "Equipment", equipment, refresh),
        assignmentForm(activity, base, "supplies", "Supplies", supplies, refresh));
}

const TRACKING_LABELS = {PENDING: "Pending", IN_PROGRESS: "In progress", FINISHED: "Finished"};

function trackingSummary(tracking) {
    const moments = [
        tracking.startedAt ? `Started ${tracking.startedAt.replace("T", " ")}` : null,
        tracking.finishedAt ? `finished ${tracking.finishedAt.replace("T", " ")}` : null,
        tracking.result ? `result: ${tracking.result}` : null,
    ].filter(Boolean);
    return [
        moments.length > 0 ? h("div", {class: "muted"}, moments.join(" · ")) : null,
        tracking.observations.length > 0
            ? h("ul", {}, tracking.observations.map(observation => h("li", {}, `${observation.recordedAt.replace("T", " ")}: ${observation.note}`)))
            : null,
    ];
}

function trackingBadge(tracking) {
    return badge(TRACKING_LABELS[tracking.status], tracking.status === "FINISHED" ? "ok" : "");
}

function trackingActions(expedition, activity, base, refresh) {
    const status = activity.tracking.status;
    const incidents = `/expeditions/${encodeURIComponent(expedition.id)}/incidents`;
    const start = formOf([], "Start", reporting(async () => {
        await api.post(`${base}/start`, {at: now()});
        await refresh();
    }));
    const finish = formOf([field("Result", textInput("result", {required: true}))], "Finish", reporting(async data => {
        await api.post(`${base}/completion`, {at: now(), result: data.get("result")});
        await refresh();
    }));
    const observe = formOf([field("Observation", textInput("note", {required: true}))], "Record observation", reporting(async data => {
        await api.post(`${base}/observations`, {note: data.get("note"), recordedAt: now()});
        await refresh();
    }));
    const incident = formOf([
        field("Incident", textInput("description", {required: true})),
        field("Affected resource", selectOf("resourceId", [{value: "", label: "None"}, ...assignedIds(activity)])),
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
    }));
    return h("details", {}, h("summary", {}, "Track"),
        status === "PENDING" ? [start, incident] : [finish, observe, incident]);
}

function assignedIds(activity) {
    return [...activity.assignedStaffIds, ...activity.assignedEquipmentIds, ...activity.assignedSupplyIds];
}

function showsTracking(expedition, activity) {
    return expedition.status === "IN_EXECUTION" || activity.tracking.status !== "PENDING";
}

async function activityView(expedition, node, refresh) {
    const activity = node.activity;
    const base = `/expeditions/${encodeURIComponent(expedition.id)}/activities/${encodeURIComponent(activity.id)}`;
    return h("div", {class: "node"},
        h("strong", {}, `${activity.name} `), badge(activity.estimatedRisk), " ", showsTracking(expedition, activity) ? trackingBadge(activity.tracking) : null,
        h("div", {class: "muted"}, `${activity.id} · ${formatPeriod(activity.scheduledPeriod)} · estimated ${formatMinutes(activity.estimatedDurationMinutes)}`),
        h("div", {class: "muted"}, `Needs: ${requirementsText(activity) || "nothing"}`),
        h("div", {}, `Assigned: ${assignedIds(activity).join(", ") || "nothing yet"}`),
        activity.dependencyIds.length > 0 ? h("div", {class: "muted"}, `Depends on: ${activity.dependencyIds.join(", ")}`) : null,
        expedition.status === "DRAFT" ? await assignmentsView(activity, base, refresh) : null,
        trackingSummary(activity.tracking),
        expedition.status === "IN_EXECUTION" && activity.tracking.status !== "FINISHED" ? trackingActions(expedition, activity, base, refresh) : null);
}

async function nodeView(expedition, node, refresh) {
    if (node.activity) {
        return activityView(expedition, node, refresh);
    }
    const children = await Promise.all((node.children ?? []).map(child => nodeView(expedition, child, refresh)));
    return h("div", {class: "node block"},
        h("strong", {}, `${node.type} block `), badge(node.estimatedRisk),
        h("div", {class: "muted"}, `${formatPeriod(node.scheduledPeriod)} · estimated ${formatMinutes(node.estimatedDurationMinutes)}`),
        ...children);
}

export async function itineraryView(expedition, itinerary, refresh) {
    const nodes = await Promise.all(itinerary.nodes.map(node => nodeView(expedition, node, refresh)));
    return panel("Itinerary", nodes.length > 0 ? nodes : h("p", {class: "muted"}, "Nothing scheduled yet."));
}
