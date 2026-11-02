async function request(method, path, body) {
    const response = await fetch(`/api${path}`, {
        method,
        headers: body === undefined ? {} : {"Content-Type": "application/json"},
        body: body === undefined ? undefined : JSON.stringify(body),
    });
    const text = await response.text();
    const content = text ? JSON.parse(text) : null;
    if (!response.ok) {
        throw new Error(describeProblem(content, response.status));
    }
    return content;
}

function describeProblem(problem, status) {
    if (!problem) {
        return `The request failed with status ${status}`;
    }
    const criticals = (problem.criticals ?? []).map(critical => `• ${critical.message}`);
    return criticals.length > 0
        ? [problem.title ?? "The expedition cannot be approved", ...criticals].join("\n")
        : problem.detail ?? problem.title ?? `The request failed with status ${status}`;
}

export const api = {
    get: path => request("GET", path),
    post: (path, body) => request("POST", path, body),
};
