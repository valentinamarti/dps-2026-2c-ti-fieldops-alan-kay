import {api} from "./api.js";
import {field, formOf, h, listOfText, panel, selectOf, textInput} from "./dom.js";
import {reporting, showError} from "./messages.js";

const BLOCK_EXAMPLE = JSON.stringify({
    type: "sequential",
    children: [
        {activity: {id: "a-20", name: "First", timeWindow: {start: "2026-11-02T00:00:00", end: "2026-11-02T23:00:00"}, scheduledPeriod: {start: "2026-11-02T08:00:00", end: "2026-11-02T10:00:00"}, zoneId: "z-coast", dependencyIds: [], type: "Sample collection", parameters: {sampleCount: 3}, atNight: false}},
        {
            type: "parallel",
            children: [
                {activity: {id: "a-21", name: "Second", timeWindow: {start: "2026-11-02T00:00:00", end: "2026-11-02T23:00:00"}, scheduledPeriod: {start: "2026-11-02T10:00:00", end: "2026-11-02T12:00:00"}, zoneId: "z-coast", dependencyIds: [], type: "Sample collection", parameters: {sampleCount: 3}, atNight: false}},
                {activity: {id: "a-22", name: "Third", timeWindow: {start: "2026-11-02T00:00:00", end: "2026-11-02T23:00:00"}, scheduledPeriod: {start: "2026-11-02T10:00:00", end: "2026-11-02T12:00:00"}, zoneId: "z-coast", dependencyIds: [], type: "Sample collection", parameters: {sampleCount: 3}, atNight: false}},
            ],
        },
    ],
}, null, 2);

function parametersFrom(types, data) {
    const type = types.find(candidate => candidate.name === data.get("type"));
    return Object.fromEntries(type.parameterNames.map(name => [name, Number(data.get("parameterValue"))]));
}

function activityForm(expedition, types, zones, base, refresh) {
    const options = types.map(type => ({value: type.name, label: `${type.name} (${type.parameterNames.join(", ")})`}));
    return formOf([
        field("Id", textInput("id", {required: true})),
        field("Name", textInput("name", {required: true})),
        field("Type", selectOf("type", options)),
        field("Value of the type parameter", textInput("parameterValue", {type: "number", min: "1", required: true})),
        field("Zone", selectOf("zoneId", zones.map(zone => zone.id).filter(id => expedition.zoneIds.includes(id)))),
        field("Window start", textInput("windowStart", {type: "datetime-local", required: true})),
        field("Window end", textInput("windowEnd", {type: "datetime-local", required: true})),
        field("Scheduled start", textInput("start", {type: "datetime-local", required: true})),
        field("Scheduled end", textInput("end", {type: "datetime-local", required: true})),
        field("Depends on (comma separated)", textInput("dependencyIds")),
        h("label", {class: "inline"}, h("input", {type: "checkbox", name: "atNight"}), "Night operation"),
    ], "Schedule activity", reporting(async data => {
        await api.post(`${base}/activities`, {
            id: data.get("id"),
            name: data.get("name"),
            timeWindow: {start: data.get("windowStart"), end: data.get("windowEnd")},
            scheduledPeriod: {start: data.get("start"), end: data.get("end")},
            zoneId: data.get("zoneId"),
            dependencyIds: listOfText(data.get("dependencyIds")),
            type: data.get("type"),
            parameters: parametersFrom(types, data),
            atNight: data.get("atNight") !== null,
        });
        await refresh();
    }));
}

function blockForm(base, refresh) {
    const parsed = text => {
        try {
            return JSON.parse(text);
        } catch (error) {
            showError(new Error(`The block is not valid JSON: ${error.message}`));
            return null;
        }
    };
    return formOf([
        field("Block, as a tree of sequential and parallel nodes", h("textarea", {name: "block", rows: 12}, BLOCK_EXAMPLE)),
    ], "Schedule block", reporting(async data => {
        const block = parsed(data.get("block"));
        if (block) {
            await api.post(`${base}/blocks`, block);
            await refresh();
        }
    }));
}

export async function planningView(expedition, refresh) {
    const base = `/expeditions/${encodeURIComponent(expedition.id)}`;
    const [types, zones] = await Promise.all([api.get("/activity-types"), api.get("/zones")]);
    return panel("Plan",
        h("h3", {}, "Schedule an activity"),
        activityForm(expedition, types, zones, base, refresh),
        h("details", {}, h("summary", {}, "Schedule a block"), blockForm(base, refresh)));
}
