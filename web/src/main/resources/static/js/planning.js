import {api} from "./api.js";
import {checkboxes, field, formOf, h, panel, selectOf, submitButton, textInput} from "./dom.js";
import {eventName, humanize} from "./format.js";
import {reporting} from "./messages.js";

function parametersFrom(types, data) {
    const type = types.find(candidate => candidate.name === data.get("type"));
    return Object.fromEntries(type.parameterNames.map(name => [name, Number(data.get("parameterValue"))]));
}

function parameterLabel(type) {
    return type.parameterNames.map(eventName).join(", ");
}

function nextActivityId(activities) {
    const used = activities.map(activity => Number(activity.id.match(/^a-(\d+)$/)?.[1] ?? 0));
    return `a-${String(Math.max(0, ...used) + 1).padStart(2, "0")}`;
}

function activityForm(expedition, activities, types, zones, base, refresh) {
    const options = types.map(type => ({value: type.name, label: humanize(type.name)}));
    const label = h("span", {}, parameterLabel(types[0]));
    const onChange = event => {
        label.textContent = parameterLabel(types.find(type => type.name === event.target.value));
    };
    return formOf([
        field("Name", textInput("name", {required: true})),
        field("Type", selectOf("type", options, {onChange})),
        field(label, textInput("parameterValue", {type: "number", min: "1", required: true})),
        field("Zone", selectOf("zoneId", zones.filter(zone => expedition.zoneIds.includes(zone.id)).map(zone => ({value: zone.id, label: zone.name})))),
        field("Allowed from", textInput("windowStart", {type: "datetime-local", required: true})),
        field("Allowed until", textInput("windowEnd", {type: "datetime-local", required: true})),
        field("Scheduled start", textInput("start", {type: "datetime-local", required: true})),
        field("Scheduled end", textInput("end", {type: "datetime-local", required: true})),
        activities.length > 0 ? h("div", {class: "form-wide"}, field("Depends on", checkboxes("dependencyIds", activities))) : null,
        h("div", {class: "form-actions"},
            h("label", {class: "inline"}, h("input", {type: "checkbox", name: "atNight"}), "Night operation"),
            submitButton("Schedule activity")),
    ], null, reporting(async data => {
        await api.post(`${base}/activities`, {
            id: nextActivityId(activities),
            name: data.get("name"),
            timeWindow: {start: data.get("windowStart"), end: data.get("windowEnd")},
            scheduledPeriod: {start: data.get("start"), end: data.get("end")},
            zoneId: data.get("zoneId"),
            dependencyIds: data.getAll("dependencyIds"),
            type: data.get("type"),
            parameters: parametersFrom(types, data),
            atNight: data.get("atNight") !== null,
        });
        await refresh();
    }, "Activity scheduled."));
}

export async function planningView(expedition, activities, refresh) {
    const base = `/expeditions/${encodeURIComponent(expedition.id)}`;
    const [types, zones] = await Promise.all([api.get("/activity-types"), api.get("/zones")]);
    return panel("Plan",
        h("h3", {}, "Schedule an activity"),
        h("div", {class: "top-aligned"}, activityForm(expedition, activities, types, zones, base, refresh)));
}
