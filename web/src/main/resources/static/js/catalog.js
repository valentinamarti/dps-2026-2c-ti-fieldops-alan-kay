import {api} from "./api.js";
import {field, formOf, h, listOfText, panel, selectOf, table, textInput} from "./dom.js";
import {humanize, quantity} from "./format.js";
import {reporting} from "./messages.js";

const UNITS = ["UNITS", "LITERS", "KILOGRAMS"].map(value => ({value, label: humanize(value)}));
const names = list => (list ?? []).join(", ");

const SECTIONS = [
    {
        title: "Staff",
        path: "/staff",
        headers: ["Id", "Name", "Certifications"],
        row: person => [person.id, person.name, names(person.certifications)],
        fields: () => [identity(), name(), certifications()],
        body: data => ({id: data.get("id"), name: data.get("name"), certifications: listOfText(data.get("certifications"))}),
        schedulable: true,
    },
    {
        title: "Vehicles",
        path: "/vehicles",
        headers: ["Id", "Name", "Category", "Plate", "Certifications"],
        row: vehicle => [vehicle.id, vehicle.name, vehicle.category, vehicle.licensePlate, names(vehicle.certifications)],
        fields: () => [identity(), name(), category(), field("License plate", textInput("licensePlate", {required: true})), certifications()],
        body: data => ({
            id: data.get("id"),
            name: data.get("name"),
            category: data.get("category"),
            licensePlate: data.get("licensePlate"),
            certifications: listOfText(data.get("certifications")),
        }),
        schedulable: true,
    },
    {
        title: "Instruments",
        path: "/instruments",
        headers: ["Id", "Name", "Category", "Certifications"],
        row: instrument => [instrument.id, instrument.name, instrument.category, names(instrument.certifications)],
        fields: () => [identity(), name(), category(), certifications()],
        body: data => ({id: data.get("id"), name: data.get("name"), category: data.get("category"), certifications: listOfText(data.get("certifications"))}),
        schedulable: true,
    },
    {
        title: "Supplies",
        path: "/supplies",
        headers: ["Id", "Name", "Category", "Stock"],
        row: supply => [supply.id, supply.name, supply.category, quantity(supply.stock)],
        fields: () => [identity(), name(), category(),
            field("Initial stock", textInput("amount", {type: "number", min: "0", step: "any", required: true})),
            field("Unit", selectOf("unit", UNITS))],
        body: data => ({
            id: data.get("id"),
            name: data.get("name"),
            category: data.get("category"),
            initialStock: {amount: data.get("amount"), unit: data.get("unit")},
        }),
    },
    {
        title: "Zones",
        path: "/zones",
        headers: ["Id", "Name", "Required permits"],
        row: zone => [zone.id, zone.name, names(zone.requiredPermits)],
        fields: () => [identity(), name(), field("Required permits (comma separated)", textInput("requiredPermits"))],
        body: data => ({id: data.get("id"), name: data.get("name"), requiredPermits: listOfText(data.get("requiredPermits"))}),
    },
];

function identity() {
    return field("Id", textInput("id", {required: true}));
}

function name() {
    return field("Name", textInput("name", {required: true}));
}

function category() {
    return field("Category", textInput("category", {required: true}));
}

function certifications() {
    return field("Certifications (comma separated)", textInput("certifications"));
}

async function refresh() {
    document.getElementById("page").replaceChildren(await renderCatalog());
}

function unavailabilityForm(section, items) {
    return h("details", {}, h("summary", {}, "Mark as unavailable"), formOf([
        field("Resource", selectOf("id", items.map(item => item.id), {required: true})),
        field("From", textInput("start", {type: "datetime-local", required: true})),
        field("To", textInput("end", {type: "datetime-local", required: true})),
        field("Reason", textInput("reason", {required: true})),
    ], "Mark unavailable", reporting(data => api.post(`${section.path}/${encodeURIComponent(data.get("id"))}/unavailabilities`, {
        period: {start: data.get("start"), end: data.get("end")},
        reason: data.get("reason"),
    }), "Marked as unavailable.")));
}

async function sectionView(section) {
    const items = await api.get(section.path);
    return panel(section.title,
        items.length > 0 ? table(section.headers, items.map(section.row)) : h("p", {class: "muted"}, "Nothing registered yet."),
        h("h3", {}, "Register"),
        formOf(section.fields(), "Register", reporting(async data => {
            await api.post(section.path, section.body(data));
            await refresh();
        }, "Registered.")),
        section.schedulable && items.length > 0 ? unavailabilityForm(section, items) : null);
}

export async function renderCatalog() {
    const sections = await Promise.all(SECTIONS.map(sectionView));
    return h("div", {}, h("h1", {}, "Catalog"), ...sections);
}
