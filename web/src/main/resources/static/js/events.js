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

export function notificationsView(notifications, names, refresh) {
    return panel("Notifications", refreshButton(refresh),
        notifications.length > 0
            ? table(["When", "Responsible", "Notice"], notifications.map(notification =>
                [formatMoment(notification.occurredAt), names.resource(notification.responsibleId), notification.text]))
            : h("p", {class: "muted"}, "No notifications yet."));
}
