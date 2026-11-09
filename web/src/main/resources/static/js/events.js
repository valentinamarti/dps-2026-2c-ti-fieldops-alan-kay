import {button, h, panel, statusBadge, table} from "./dom.js";
import {consumerName, eventName, formatMoment} from "./format.js";
import {reporting} from "./messages.js";

function deliveryView(delivery) {
    return h("div", {class: "row"},
        statusBadge(delivery.status),
        `${consumerName(delivery.consumer)} · ${delivery.attempts} ${delivery.attempts === 1 ? "attempt" : "attempts"}`,
        delivery.lastError ? h("span", {class: "muted"}, delivery.lastError) : null);
}

export function timelineTable(events) {
    return table(["When", "Event", "Consumers"], events.map(event => [
        formatMoment(event.occurredAt),
        eventName(event.type),
        event.deliveries.length > 0 ? event.deliveries.map(deliveryView) : "No asynchronous consumers",
    ]));
}

function refreshButton(refresh) {
    return button("Refresh", reporting(refresh));
}

export function eventsView(events, refresh) {
    return panel("Events", refreshButton(refresh),
        h("p", {class: "muted"}, "Consumers receive each event some seconds after it happens."),
        events.length > 0 ? timelineTable(events) : h("p", {class: "muted"}, "No events yet: they appear when the expedition is approved or tracked."));
}

function groupedByEvent(notifications) {
    const groups = new Map();
    notifications.forEach(notification => {
        const group = groups.get(notification.eventId) ?? {...notification, responsibleIds: []};
        group.responsibleIds.push(notification.responsibleId);
        groups.set(notification.eventId, group);
    });
    return [...groups.values()];
}

export function notificationsView(notifications, names, refresh) {
    return panel("Notifications", refreshButton(refresh),
        notifications.length > 0
            ? table(["When", "Notice", "Sent to"], groupedByEvent(notifications).map(group =>
                [formatMoment(group.occurredAt), group.text, group.responsibleIds.map(names.resource).join(", ")]))
            : h("p", {class: "muted"}, "No notifications yet."));
}
