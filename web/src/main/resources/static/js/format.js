const MONTHS = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"];

export function humanize(value) {
    const text = String(value ?? "").replaceAll("_", " ").toLowerCase();
    return text.charAt(0).toUpperCase() + text.slice(1);
}

export function eventName(type) {
    return humanize(type.replace(/([a-z])([A-Z])/g, "$1 $2"));
}

export function consumerName(name) {
    return humanize(name.replaceAll("-", " "));
}

function dayOf(moment) {
    const [, month, day] = moment.split("T")[0].split("-");
    return `${Number(day)} ${MONTHS[Number(month) - 1]}`;
}

function timeOf(moment) {
    return moment.split("T")[1].slice(0, 5);
}

export function formatMoment(moment) {
    return `${dayOf(moment)}, ${timeOf(moment)}`;
}

export function formatPeriod(period) {
    const sameDay = period.start.split("T")[0] === period.end.split("T")[0];
    return `${formatMoment(period.start)} → ${sameDay ? timeOf(period.end) : formatMoment(period.end)}`;
}

export function formatMinutes(minutes) {
    const hours = Math.floor(minutes / 60);
    const rest = minutes % 60;
    if (hours === 0) {
        return `${rest} min`;
    }
    return rest === 0 ? `${hours} h` : `${hours} h ${rest} min`;
}

export function quantity({amount, unit}) {
    const plural = unit.toLowerCase();
    return `${amount} ${Number(amount) === 1 ? plural.slice(0, -1) : plural}`;
}

export function people(count) {
    return count === 1 ? "1 person" : `${count} people`;
}

export function counted(count, noun) {
    return `${count} ${count === 1 || noun.endsWith("s") ? noun : `${noun}s`}`;
}
