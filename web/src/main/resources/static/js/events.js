import {badge, button, h, panel, table} from "./dom.js";
import {reporting} from "./messages.js";

const DELIVERY_KINDS = {DELIVERED: "ok", FAILED: "danger"};

function moment(timestamp) {
    return timestamp.replace("T", " ").slice(0, 19);
}

function deliveryView(delivery) {
    return h("div", {class: "row"},
        badge(delivery.status, DELIVERY_KINDS[delivery.status] ?? ""),
        `${delivery.consumer} · ${delivery.attempts} ${delivery.attempts === 1 ? "attempt" : "attempts"}`,
        delivery.lastError ? h("span", {class: "muted"}, delivery.lastError) : null);
}

export function timelineTable(events) {
    return table(["When", "Event", "Consumers"], events.map(event => [
        moment(event.occurredAt),
        event.type,
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

export function notificationsView(notifications, refresh) {
    return panel("Notifications", refreshButton(refresh),
        notifications.length > 0
            ? table(["When", "Responsible", "Notice"], notifications.map(notification =>
                [moment(notification.occurredAt), notification.responsibleId, notification.text]))
            : h("p", {class: "muted"}, "No notifications yet."));
}
